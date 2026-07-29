package com.deaddict.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deaddict.app.billing.EntitlementTier
import com.deaddict.app.billing.PurchaseVerificationStatus
import com.deaddict.app.rescue.RescueStep
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramCategory
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.SafetyTier
import kotlinx.coroutines.delay

/** Production entry point that passes an explicit RecoveryTrackUi to every track-sensitive screen. */
@Composable
fun RecoveryTrackAppRoot(
    state: AppUiState,
    isAppUnlocked: Boolean,
    onTabSelected: (AppTab) -> Unit,
    onProgramSelected: (ProgramDefinition) -> Unit,
    onTrackSelected: (String) -> Unit,
    onMakePrimary: (String) -> Unit,
    onPauseTrack: (String) -> Unit,
    onResumeTrack: (String) -> Unit,
    onMaintenanceTrack: (String) -> Unit,
    onArchiveTrack: (String) -> Unit,
    onTrackingRecorded: (TrackingEntry) -> Unit,
    onRequestUsageAccess: () -> Unit,
    onBeginRescue: () -> Unit,
    onRescueTick: () -> Unit,
    onRescueContinue: () -> Unit,
    onRescueInitialUrge: (Int) -> Unit,
    onRescueTrigger: (String) -> Unit,
    onRescueAction: (String) -> Unit,
    onRescueFinalUrge: (Int) -> Unit,
    onRescueComplete: () -> Unit,
    onRescueReset: () -> Unit,
    onEnableDailyNotifications: () -> Unit,
    onDisableDailyNotifications: () -> Unit,
    onEnableBiometricLock: () -> Unit,
    onDisableBiometricLock: () -> Unit,
    onScreenProtectionChanged: (Boolean) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onUsageMonitoringChanged: (Boolean) -> Unit,
    onDeleteLocalData: () -> Unit,
    onPurchasePlus: (String) -> Unit,
    onRestorePurchases: () -> Unit,
) {
    when {
        !isAppUnlocked -> LockedScreenV2()
        state.isLoading -> LoadingScreenV2()
        state.requiresOnboarding -> ProgramOnboardingV2(state.availablePrograms, onProgramSelected)
        else -> AppScaffoldV2(
            state = state,
            onTabSelected = onTabSelected,
            onProgramSelected = onProgramSelected,
            onTrackSelected = onTrackSelected,
            onMakePrimary = onMakePrimary,
            onPauseTrack = onPauseTrack,
            onResumeTrack = onResumeTrack,
            onMaintenanceTrack = onMaintenanceTrack,
            onArchiveTrack = onArchiveTrack,
            onTrackingRecorded = onTrackingRecorded,
            onRequestUsageAccess = onRequestUsageAccess,
            onBeginRescue = onBeginRescue,
            onRescueTick = onRescueTick,
            onRescueContinue = onRescueContinue,
            onRescueInitialUrge = onRescueInitialUrge,
            onRescueTrigger = onRescueTrigger,
            onRescueAction = onRescueAction,
            onRescueFinalUrge = onRescueFinalUrge,
            onRescueComplete = onRescueComplete,
            onRescueReset = onRescueReset,
            onEnableDailyNotifications = onEnableDailyNotifications,
            onDisableDailyNotifications = onDisableDailyNotifications,
            onEnableBiometricLock = onEnableBiometricLock,
            onDisableBiometricLock = onDisableBiometricLock,
            onScreenProtectionChanged = onScreenProtectionChanged,
            onAnalyticsChanged = onAnalyticsChanged,
            onUsageMonitoringChanged = onUsageMonitoringChanged,
            onDeleteLocalData = onDeleteLocalData,
            onPurchasePlus = onPurchasePlus,
            onRestorePurchases = onRestorePurchases,
        )
    }
}

@Composable
private fun AppScaffoldV2(
    state: AppUiState,
    onTabSelected: (AppTab) -> Unit,
    onProgramSelected: (ProgramDefinition) -> Unit,
    onTrackSelected: (String) -> Unit,
    onMakePrimary: (String) -> Unit,
    onPauseTrack: (String) -> Unit,
    onResumeTrack: (String) -> Unit,
    onMaintenanceTrack: (String) -> Unit,
    onArchiveTrack: (String) -> Unit,
    onTrackingRecorded: (TrackingEntry) -> Unit,
    onRequestUsageAccess: () -> Unit,
    onBeginRescue: () -> Unit,
    onRescueTick: () -> Unit,
    onRescueContinue: () -> Unit,
    onRescueInitialUrge: (Int) -> Unit,
    onRescueTrigger: (String) -> Unit,
    onRescueAction: (String) -> Unit,
    onRescueFinalUrge: (Int) -> Unit,
    onRescueComplete: () -> Unit,
    onRescueReset: () -> Unit,
    onEnableDailyNotifications: () -> Unit,
    onDisableDailyNotifications: () -> Unit,
    onEnableBiometricLock: () -> Unit,
    onDisableBiometricLock: () -> Unit,
    onScreenProtectionChanged: (Boolean) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onUsageMonitoringChanged: (Boolean) -> Unit,
    onDeleteLocalData: () -> Unit,
    onPurchasePlus: (String) -> Unit,
    onRestorePurchases: () -> Unit,
) {
    val selectedTrack = state.selectedRecoveryTrack
    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Text(tab.iconV2()) },
                        label = { Text(tab.labelV2()) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.selectedTab) {
                AppTab.HOME -> TodayScreenV2(state)
                AppTab.TRACK -> TracksScreenV2(
                    tracks = state.recoveryTracks,
                    selectedTrack = selectedTrack,
                    availablePrograms = state.availablePrograms,
                    onProgramSelected = onProgramSelected,
                    onTrackSelected = onTrackSelected,
                    onMakePrimary = onMakePrimary,
                    onPauseTrack = onPauseTrack,
                    onResumeTrack = onResumeTrack,
                    onMaintenanceTrack = onMaintenanceTrack,
                    onArchiveTrack = onArchiveTrack,
                    onTrackingRecorded = onTrackingRecorded,
                )
                AppTab.RESCUE -> ToolsScreenV2(
                    state = state,
                    selectedTrack = selectedTrack,
                    onBegin = onBeginRescue,
                    onTick = onRescueTick,
                    onContinue = onRescueContinue,
                    onInitialUrge = onRescueInitialUrge,
                    onTrigger = onRescueTrigger,
                    onAction = onRescueAction,
                    onFinalUrge = onRescueFinalUrge,
                    onComplete = onRescueComplete,
                    onReset = onRescueReset,
                )
                AppTab.INSIGHTS -> InsightsScreenV2(state, selectedTrack)
                AppTab.PROFILE -> YouScreenV2(
                    state = state,
                    onRequestUsageAccess = onRequestUsageAccess,
                    onEnableDailyNotifications = onEnableDailyNotifications,
                    onDisableDailyNotifications = onDisableDailyNotifications,
                    onEnableBiometricLock = onEnableBiometricLock,
                    onDisableBiometricLock = onDisableBiometricLock,
                    onScreenProtectionChanged = onScreenProtectionChanged,
                    onAnalyticsChanged = onAnalyticsChanged,
                    onUsageMonitoringChanged = onUsageMonitoringChanged,
                    onDeleteLocalData = onDeleteLocalData,
                    onPurchasePlus = onPurchasePlus,
                    onRestorePurchases = onRestorePurchases,
                )
            }
            state.message?.let { message ->
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        message,
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramOnboardingV2(programs: List<ProgramDefinition>, onSelected: (ProgramDefinition) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("What would you like support with?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Choose one primary recovery focus. Supporting tracks can be added later.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProgramCategory.entries.forEach { category ->
            item {
                Text(
                    category.nameV2(),
                    Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(programs.filter { it.category == category }, key = { it.id.value }) { program ->
                Card(Modifier.fillMaxWidth(), onClick = { onSelected(program) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(program.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            program.safety.tier.supportLabelV2(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayScreenV2(state: AppUiState) {
    val primary = state.recoveryTracks.firstOrNull { it.role == RecoveryTrackRole.PRIMARY }
    val supporting = state.recoveryTracks.filterNot { it.id == primary?.id }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Today", style = MaterialTheme.typography.headlineLarge)
            Text("One useful decision at a time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        primary?.let { item { TrackSummaryCardV2(it, true) } }
        items(supporting, key = { it.id }) { TrackSummaryCardV2(it, false) }
        item {
            SectionCardV2("Next action") {
                Text("Open Tracks for a private check-in, or Tools when an urge needs immediate attention.")
            }
        }
    }
}

@Composable
private fun TrackSummaryCardV2(track: RecoveryTrackUi, primary: Boolean) {
    SectionCardV2(track.title) {
        Text(
            if (primary) "Primary recovery track" else "Supporting recovery track",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text("${track.status.labelV2()} · Progress and lapses stay independent.")
        if (track.program.safety.tier == SafetyTier.MEDICALLY_HIGH_RISK) {
            Text(
                "Use professional guidance for major changes. DeAddict does not provide detox or taper instructions.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TracksScreenV2(
    tracks: List<RecoveryTrackUi>,
    selectedTrack: RecoveryTrackUi?,
    availablePrograms: List<ProgramDefinition>,
    onProgramSelected: (ProgramDefinition) -> Unit,
    onTrackSelected: (String) -> Unit,
    onMakePrimary: (String) -> Unit,
    onPauseTrack: (String) -> Unit,
    onResumeTrack: (String) -> Unit,
    onMaintenanceTrack: (String) -> Unit,
    onArchiveTrack: (String) -> Unit,
    onTrackingRecorded: (TrackingEntry) -> Unit,
) {
    if (selectedTrack == null) return
    var showAddDialog by remember { mutableStateOf(false) }
    var archiveCandidate by remember { mutableStateOf<RecoveryTrackUi?>(null) }
    val openProgramIds = tracks.mapTo(mutableSetOf()) { it.program.id.value }
    val addablePrograms = availablePrograms.filterNot { it.id.value in openProgramIds }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Tracks", style = MaterialTheme.typography.headlineLarge)
                    Text("Every journey has its own history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (addablePrograms.isNotEmpty()) {
                    TextButton(onClick = { showAddDialog = true }) { Text("Add") }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tracks.forEach { track ->
                    FilterChip(
                        selected = track.id == selectedTrack.id,
                        onClick = { onTrackSelected(track.id) },
                        label = {
                            Text(if (track.role == RecoveryTrackRole.PRIMARY) "${track.title} · Primary" else track.title)
                        },
                    )
                }
            }
        }
        item {
            LifecycleCardV2(
                selectedTrack,
                onMakePrimary,
                onPauseTrack,
                onResumeTrack,
                onMaintenanceTrack,
                onArchiveRequested = { archiveCandidate = it },
            )
        }
        item { TrackingCardV2(selectedTrack, onTrackingRecorded) }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add recovery track") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(addablePrograms, key = { it.id.value }) { program ->
                        Card(
                            Modifier.fillMaxWidth(),
                            onClick = {
                                showAddDialog = false
                                onProgramSelected(program)
                            },
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(program.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Progress, slips, Rescue sessions, and insights stay independent.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
        )
    }

    archiveCandidate?.let { track ->
        AlertDialog(
            onDismissRequest = { archiveCandidate = null },
            title = { Text("Archive ${track.title}?") },
            text = { Text("History remains available. Continuing later creates a new journey.") },
            confirmButton = {
                TextButton(onClick = {
                    archiveCandidate = null
                    onArchiveTrack(track.id)
                }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { archiveCandidate = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LifecycleCardV2(
    track: RecoveryTrackUi,
    onMakePrimary: (String) -> Unit,
    onPauseTrack: (String) -> Unit,
    onResumeTrack: (String) -> Unit,
    onMaintenanceTrack: (String) -> Unit,
    onArchiveRequested: (RecoveryTrackUi) -> Unit,
) {
    SectionCardV2(track.title) {
        Text("${track.role.labelV2()} · ${track.status.labelV2()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (
                track.role != RecoveryTrackRole.PRIMARY &&
                track.status in setOf(RecoveryTrackStatus.ACTIVE, RecoveryTrackStatus.MAINTENANCE)
            ) {
                TextButton(onClick = { onMakePrimary(track.id) }) { Text("Make primary") }
            }
            when (track.status) {
                RecoveryTrackStatus.ACTIVE -> {
                    TextButton(onClick = { onPauseTrack(track.id) }) { Text("Pause") }
                    TextButton(onClick = { onMaintenanceTrack(track.id) }) { Text("Maintenance") }
                }
                RecoveryTrackStatus.PAUSED -> TextButton(onClick = { onResumeTrack(track.id) }) { Text("Resume") }
                RecoveryTrackStatus.MAINTENANCE -> TextButton(onClick = { onResumeTrack(track.id) }) { Text("Return active") }
                RecoveryTrackStatus.ARCHIVED -> Unit
            }
            if (track.status != RecoveryTrackStatus.ARCHIVED) {
                TextButton(onClick = { onArchiveRequested(track) }) { Text("Archive") }
            }
        }
    }
}

@Composable
private fun TrackingCardV2(track: RecoveryTrackUi, onTrackingRecorded: (TrackingEntry) -> Unit) {
    var intensity by remember(track.id) { mutableIntStateOf(3) }
    var selectedKind by remember(track.id) { mutableStateOf(TrackingEventKind.URGE) }
    var numericValue by remember(track.id) { mutableStateOf("") }
    var selectedTrigger by remember(track.id) { mutableStateOf<String?>(null) }
    val needsIntensity = selectedKind in setOf(TrackingEventKind.URGE, TrackingEventKind.CRAVING, TrackingEventKind.SLIP)
    val needsValue = selectedKind in setOf(TrackingEventKind.QUANTITY, TrackingEventKind.TIME, TrackingEventKind.COST)
    val parsedValue = numericValue.toDoubleOrNull()
    val canSave = !needsValue || (parsedValue != null && parsedValue > 0)

    SectionCardV2("Private check-in") {
        Text("Saving to ${track.title}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                TrackingEventKind.URGE to "Urge",
                TrackingEventKind.CRAVING to "Craving",
                TrackingEventKind.SLIP to "Slip",
                TrackingEventKind.QUANTITY to "Quantity",
                TrackingEventKind.TIME to "Time",
                TrackingEventKind.COST to "Cost",
                TrackingEventKind.ACTIVITY to "Activity",
            ).forEach { (kind, label) ->
                FilterChip(
                    selected = selectedKind == kind,
                    onClick = {
                        selectedKind = kind
                        numericValue = ""
                    },
                    label = { Text(label) },
                )
            }
        }
        if (needsIntensity) {
            Text("Intensity", style = MaterialTheme.typography.titleMedium)
            IntensityPickerV2(intensity) { intensity = it }
        }
        if (needsValue) {
            OutlinedTextField(
                value = numericValue,
                onValueChange = { candidate ->
                    if (candidate.length <= 10 && candidate.count { it == '.' } <= 1) {
                        numericValue = candidate.filter { it.isDigit() || it == '.' }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(when (selectedKind) {
                        TrackingEventKind.TIME -> "Minutes"
                        TrackingEventKind.COST -> "Cost"
                        else -> "Quantity"
                    })
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = numericValue.isNotEmpty() && !canSave,
            )
        }
        Text("Trigger", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("stress", "boredom", "social", "routine").forEach { trigger ->
                FilterChip(
                    selected = selectedTrigger == trigger,
                    onClick = { selectedTrigger = if (selectedTrigger == trigger) null else trigger },
                    label = { Text(trigger.replaceFirstChar(Char::uppercase)) },
                )
            }
        }
        Button(
            enabled = canSave,
            onClick = {
                onTrackingRecorded(
                    TrackingEntry(
                        kind = selectedKind,
                        value = parsedValue,
                        urgeIntensity = if (needsIntensity) intensity else null,
                        triggerKey = selectedTrigger,
                    ),
                )
                numericValue = ""
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (selectedKind == TrackingEventKind.SLIP) "Record slip" else "Save to ${track.title}")
        }
    }
}

@Composable
private fun ToolsScreenV2(
    state: AppUiState,
    selectedTrack: RecoveryTrackUi?,
    onBegin: () -> Unit,
    onTick: () -> Unit,
    onContinue: () -> Unit,
    onInitialUrge: (Int) -> Unit,
    onTrigger: (String) -> Unit,
    onAction: (String) -> Unit,
    onFinalUrge: (Int) -> Unit,
    onComplete: () -> Unit,
    onReset: () -> Unit,
) {
    val rescue = state.rescue
    LaunchedEffect(rescue.step, rescue.secondsRemaining) {
        if (rescue.step == RescueStep.PAUSE && rescue.secondsRemaining > 0) {
            delay(1_000)
            onTick()
        }
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (rescue.step) {
            RescueStep.READY -> {
                Text("Tools", style = MaterialTheme.typography.displaySmall)
                Text(
                    selectedTrack?.let { "Rescue will be saved to ${it.title}." }
                        ?: "Choose a Recovery Track before Rescue.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("You do not have to decide everything right now.", textAlign = TextAlign.Center)
                Button(enabled = selectedTrack != null, onClick = onBegin) { Text("Begin two-minute Rescue") }
                Text("Works offline", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RescueStep.PAUSE -> {
                Text("Pause", style = MaterialTheme.typography.displaySmall)
                Text(rescue.secondsRemaining.toString(), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                Text(if (rescue.secondsRemaining % 10 < 5) "Breathe in slowly" else "Breathe out gently")
                Button(enabled = rescue.secondsRemaining == 0, onClick = onContinue) { Text("Continue") }
            }
            RescueStep.MOTIVATION -> {
                Text("Remember why", style = MaterialTheme.typography.headlineLarge)
                Card(Modifier.fillMaxWidth()) {
                    Text(rescue.motivation, Modifier.padding(24.dp), textAlign = TextAlign.Center)
                }
                Button(onClick = onContinue) { Text("Keep going") }
            }
            RescueStep.INITIAL_URGE -> {
                Text("How strong is the urge?", style = MaterialTheme.typography.headlineMedium)
                IntensityPickerV2(rescue.initialUrge, onInitialUrge)
                Button(onClick = onContinue) { Text("Continue") }
            }
            RescueStep.TRIGGER -> {
                Text("What may have triggered this?", style = MaterialTheme.typography.headlineMedium)
                listOf("stress", "boredom", "social", "routine", "access").forEach { trigger ->
                    Button(onClick = { onTrigger(trigger) }, modifier = Modifier.fillMaxWidth()) {
                        Text(trigger.replaceFirstChar(Char::uppercase))
                    }
                }
            }
            RescueStep.REPLACEMENT -> {
                Text("Choose one small action", style = MaterialTheme.typography.headlineMedium)
                rescue.replacementActions.forEach { action ->
                    Button(onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(action) }
                }
            }
            RescueStep.RECHECK -> {
                Text("Check in again", style = MaterialTheme.typography.headlineMedium)
                IntensityPickerV2(rescue.finalUrge, onFinalUrge)
                Button(onClick = onComplete) { Text("Record outcome") }
            }
            RescueStep.COMPLETE -> {
                Text("Recovery continues", style = MaterialTheme.typography.headlineLarge)
                Text(
                    when {
                        rescue.finalUrge < rescue.initialUrge -> "The urge eased. You created useful space."
                        rescue.finalUrge == rescue.initialUrge -> "The urge remains, and the pause still counts."
                        else -> "This is difficult right now. The next safe action still matters."
                    },
                    textAlign = TextAlign.Center,
                )
                if (rescue.program?.safety?.tier == SafetyTier.MEDICALLY_HIGH_RISK) {
                    Text(
                        "Seek immediate professional or emergency help for danger, overdose risk, severe withdrawal, or inability to stay safe.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(onClick = onReset) { Text("Done") }
            }
        }
    }
}

@Composable
private fun InsightsScreenV2(state: AppUiState, selectedTrack: RecoveryTrackUi?) {
    val insights = state.insights
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Insights", style = MaterialTheme.typography.headlineLarge)
            Text(
                selectedTrack?.let { "Patterns for ${it.title} only." } ?: "Choose a track to view patterns.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (insights == null) {
            item { SectionCardV2("Not enough data") { Text("Record a few check-ins for this track to begin seeing patterns.") } }
        } else {
            item {
                SectionCardV2("Seven-day summary") {
                    MetricV2("Check-ins", insights.checkInCount.toString())
                    MetricV2("Slips recorded", insights.slipCount.toString())
                    MetricV2("Average intensity", insights.averageUrge?.let { "%.1f / 5".format(it) } ?: "Not enough data")
                    MetricV2("Rescue sessions", insights.rescueCount.toString())
                }
            }
            item {
                SectionCardV2("Why you’re seeing this") {
                    Text(insights.explanation)
                    Text(
                        "These are recorded patterns for one Recovery Track, not a diagnosis or prediction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun YouScreenV2(
    state: AppUiState,
    onRequestUsageAccess: () -> Unit,
    onEnableDailyNotifications: () -> Unit,
    onDisableDailyNotifications: () -> Unit,
    onEnableBiometricLock: () -> Unit,
    onDisableBiometricLock: () -> Unit,
    onScreenProtectionChanged: (Boolean) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onUsageMonitoringChanged: (Boolean) -> Unit,
    onDeleteLocalData: () -> Unit,
    onPurchasePlus: (String) -> Unit,
    onRestorePurchases: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete local recovery data?") },
            text = { Text("Tracks, check-ins, Rescue sessions, and local insights will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteLocalData()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("You", style = MaterialTheme.typography.headlineLarge)
            Text("Privacy, reminders, data, and membership.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionCardV2("DeAddict Plus") {
                if (state.billing.entitlement == EntitlementTier.PLUS) {
                    Text("Plus is active", color = MaterialTheme.colorScheme.primary)
                } else {
                    state.billing.offers.forEach { offer ->
                        Button(onClick = { onPurchasePlus(offer.offerToken) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${offer.basePlanId}: ${offer.formattedPrice}")
                        }
                    }
                    if (state.billing.offers.isEmpty()) {
                        Text(if (state.billing.loading) "Loading plans…" else "Plus is unavailable right now.")
                    }
                }
                state.billing.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (
                    state.billing.verification == PurchaseVerificationStatus.PENDING ||
                    state.billing.verification == PurchaseVerificationStatus.BACKEND_UNAVAILABLE
                ) {
                    Text("Purchase verification is required before Plus can activate.", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onRestorePurchases) { Text("Restore purchases") }
            }
        }
        item {
            SectionCardV2("Privacy and security") {
                ToggleV2(
                    "Biometric app lock",
                    "Require biometrics or the device credential when returning.",
                    state.privacyPreferences.biometricLockEnabled,
                ) { if (it) onEnableBiometricLock() else onDisableBiometricLock() }
                HorizontalDivider()
                ToggleV2(
                    "Protect screenshots and previews",
                    "Block screenshots and obscure the recent-app preview.",
                    state.privacyPreferences.screenProtectionEnabled,
                    onScreenProtectionChanged,
                )
                HorizontalDivider()
                ToggleV2(
                    "Usage monitoring",
                    "Use granted app-level usage access for digital estimates.",
                    state.privacyPreferences.usageMonitoringEnabled,
                    onUsageMonitoringChanged,
                )
                HorizontalDivider()
                ToggleV2(
                    "Anonymous analytics",
                    "Off by default. Notes and sensitive content are excluded.",
                    state.privacyPreferences.analyticsEnabled,
                    onAnalyticsChanged,
                )
            }
        }
        item {
            SectionCardV2("Private reminders") {
                Text("Reminder text does not name a habit or Recovery Track.")
                if (state.notificationPreferences.dailyCheckInEnabled) {
                    Text("Daily check-in enabled", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onDisableDailyNotifications) { Text("Turn off") }
                } else {
                    Button(onClick = onEnableDailyNotifications) { Text("Enable daily check-in") }
                }
            }
        }
        item {
            SectionCardV2("Digital usage") {
                if (!state.usageAccessGranted) {
                    Text("Optional Android usage access can estimate app time and opening patterns.")
                    Button(onClick = onRequestUsageAccess) { Text("Choose usage access") }
                } else {
                    state.dailyUsage?.let { usage ->
                        MetricV2("Estimated app time today", "${usage.totalForegroundMinutes} min")
                        MetricV2("Estimated openings", usage.totalOpeningEstimate.toString())
                    } ?: Text("No estimate is available yet.")
                    TextButton(onClick = onRequestUsageAccess) { Text("Manage Android access") }
                }
            }
        }
        item {
            SectionCardV2("Your data") {
                Text("Delete recovery data stored on this device. Privacy settings are retained.")
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete local data", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SectionCardV2(title: String, content: @Composable Column.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun IntensityPickerV2(selected: Int, onSelected: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (1..5).forEach { value ->
            if (value == selected) {
                Button(onClick = { onSelected(value) }) { Text("$value") }
            } else {
                TextButton(onClick = { onSelected(value) }) { Text("$value") }
            }
        }
    }
    Text("1 is mild · 5 is intense", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ToggleV2(
    label: String,
    description: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked, onChanged, Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun MetricV2(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LockedScreenV2() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DeAddict is locked", style = MaterialTheme.typography.headlineMedium)
            Text("Confirm your identity to continue.")
        }
    }
}

@Composable
private fun LoadingScreenV2() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

private fun AppTab.labelV2() = when (this) {
    AppTab.HOME -> "Today"
    AppTab.TRACK -> "Tracks"
    AppTab.RESCUE -> "Tools"
    AppTab.INSIGHTS -> "Insights"
    AppTab.PROFILE -> "You"
}

private fun AppTab.iconV2() = when (this) {
    AppTab.HOME -> "●"
    AppTab.TRACK -> "+"
    AppTab.RESCUE -> "◉"
    AppTab.INSIGHTS -> "↗"
    AppTab.PROFILE -> "○"
}

private fun RecoveryTrackRole.labelV2() = when (this) {
    RecoveryTrackRole.PRIMARY -> "Primary"
    RecoveryTrackRole.SUPPORTING -> "Supporting"
}

private fun RecoveryTrackStatus.labelV2() = when (this) {
    RecoveryTrackStatus.ACTIVE -> "Active"
    RecoveryTrackStatus.PAUSED -> "Paused"
    RecoveryTrackStatus.MAINTENANCE -> "Maintenance"
    RecoveryTrackStatus.ARCHIVED -> "Archived"
}

private fun ProgramCategory.nameV2() = when (this) {
    ProgramCategory.SUBSTANCE -> "Substances"
    ProgramCategory.DIGITAL -> "Digital habits"
    ProgramCategory.BEHAVIOURAL -> "Behavioural habits"
}

private fun SafetyTier.supportLabelV2() = when (this) {
    SafetyTier.GENERAL_SELF_MANAGEMENT -> "Self-management support"
    SafetyTier.CLINICALLY_SENSITIVE -> "Sensitive support"
    SafetyTier.MEDICALLY_HIGH_RISK -> "Professional guidance recommended"
}
