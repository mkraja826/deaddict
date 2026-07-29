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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deaddict.app.billing.EntitlementTier
import com.deaddict.app.billing.PurchaseVerificationStatus
import com.deaddict.app.coach.RookTone
import com.deaddict.app.onboarding.OnboardingCoordinator
import com.deaddict.app.onboarding.OnboardingDraft
import com.deaddict.app.onboarding.OnboardingStep
import com.deaddict.app.onboarding.OnboardingUsageMode
import com.deaddict.app.rescue.RescueStep
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramCategory
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.SafetyTier
import kotlinx.coroutines.delay

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
    onOnboardingNext: () -> Unit,
    onOnboardingBack: () -> Unit,
    onOnboardingPrivacyChanged: (Boolean) -> Unit,
    onOnboardingUsageModeChanged: (OnboardingUsageMode) -> Unit,
    onOnboardingMotivationChanged: (String) -> Unit,
    onOnboardingPrimaryProgramChanged: (ProgramDefinition) -> Unit,
    onOnboardingSafetyChanged: (Boolean) -> Unit,
    onOnboardingGoalChanged: (RecoveryGoalType) -> Unit,
    onOnboardingGoalDetailsChanged: (Double?, String?) -> Unit,
    onOnboardingBaselineChanged: (Double?) -> Unit,
    onOnboardingTriggerToggled: (String) -> Unit,
    onOnboardingRookToneChanged: (RookTone) -> Unit,
    onOnboardingNotificationsChanged: (Boolean) -> Unit,
    onOnboardingComplete: () -> Unit,
) {
    when {
        !isAppUnlocked -> LockedScreen()
        state.isLoading -> LoadingScreen()
        state.requiresOnboarding -> OnboardingScreen(
            state = state,
            onNext = onOnboardingNext,
            onBack = onOnboardingBack,
            onPrivacyChanged = onOnboardingPrivacyChanged,
            onUsageModeChanged = onOnboardingUsageModeChanged,
            onMotivationChanged = onOnboardingMotivationChanged,
            onPrimaryProgramChanged = onOnboardingPrimaryProgramChanged,
            onSafetyChanged = onOnboardingSafetyChanged,
            onGoalChanged = onOnboardingGoalChanged,
            onGoalDetailsChanged = onOnboardingGoalDetailsChanged,
            onBaselineChanged = onOnboardingBaselineChanged,
            onTriggerToggled = onOnboardingTriggerToggled,
            onRookToneChanged = onOnboardingRookToneChanged,
            onNotificationsChanged = onOnboardingNotificationsChanged,
            onComplete = onOnboardingComplete,
        )
        else -> MainScaffold(
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
private fun OnboardingScreen(
    state: AppUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPrivacyChanged: (Boolean) -> Unit,
    onUsageModeChanged: (OnboardingUsageMode) -> Unit,
    onMotivationChanged: (String) -> Unit,
    onPrimaryProgramChanged: (ProgramDefinition) -> Unit,
    onSafetyChanged: (Boolean) -> Unit,
    onGoalChanged: (RecoveryGoalType) -> Unit,
    onGoalDetailsChanged: (Double?, String?) -> Unit,
    onBaselineChanged: (Double?) -> Unit,
    onTriggerToggled: (String) -> Unit,
    onRookToneChanged: (RookTone) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onComplete: () -> Unit,
) {
    val draft = state.onboarding
    val coordinator = remember { OnboardingCoordinator() }
    val selectedProgram = state.availablePrograms.firstOrNull { it.id.value == draft.primaryProgramId }
    val canContinue = coordinator.canContinue(draft)

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (draft.step != OnboardingStep.WELCOME) {
                        TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                    }
                    Button(
                        onClick = if (draft.step == OnboardingStep.SUMMARY) onComplete else onNext,
                        enabled = canContinue,
                        modifier = Modifier.weight(2f).testTag("onboarding_continue"),
                    ) {
                        Text(if (draft.step == OnboardingStep.SUMMARY) "Create my Recovery Track" else "Continue")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Private setup", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(draft.step.title(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Step ${OnboardingCoordinator.ORDER.indexOf(draft.step) + 1} of ${OnboardingCoordinator.ORDER.size - 1}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (draft.step) {
                OnboardingStep.WELCOME -> item {
                    SectionCard("Welcome to DeAddict") {
                        Text("Build one private recovery journey first. You can add supporting tracks later.")
                        Text("DeAddict supports self-management. It is not emergency, detox, or medical care.")
                    }
                }
                OnboardingStep.PRIVACY -> item {
                    SectionCard("Your data stays under your control") {
                        Text("Check-ins and notes are stored on this device first. Private notes are never included in sync payloads.")
                        CheckRow("I understand and want to continue", draft.privacyAccepted, onPrivacyChanged)
                    }
                }
                OnboardingStep.USAGE_MODE -> item {
                    SectionCard("Choose how to begin") {
                        ChoiceChip(
                            selected = draft.usageMode == OnboardingUsageMode.PRIVATE_ON_DEVICE,
                            label = "Private on this device",
                            onClick = { onUsageModeChanged(OnboardingUsageMode.PRIVATE_ON_DEVICE) },
                        )
                        ChoiceChip(
                            selected = draft.usageMode == OnboardingUsageMode.SIGN_IN_AND_SYNC,
                            label = "Sign in and sync later",
                            onClick = { onUsageModeChanged(OnboardingUsageMode.SIGN_IN_AND_SYNC) },
                        )
                        Text("Both modes use the same recovery features. Sign-in remains optional.")
                    }
                }
                OnboardingStep.MOTIVATION -> item {
                    SectionCard("What matters to you?") {
                        OutlinedTextField(
                            value = draft.motivation,
                            onValueChange = onMotivationChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("One reason to make a change") },
                            minLines = 3,
                        )
                    }
                }
                OnboardingStep.PRIMARY_PROGRAM -> {
                    ProgramCategory.entries.forEach { category ->
                        item { Text(category.label(), style = MaterialTheme.typography.titleMedium) }
                        items(
                            state.availablePrograms.filter { it.category == category },
                            key = { it.id.value },
                        ) { program ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onPrimaryProgramChanged(program) },
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(program.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(program.safety.tier.supportLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Checkbox(
                                        checked = draft.primaryProgramId == program.id.value,
                                        onCheckedChange = { onPrimaryProgramChanged(program) },
                                    )
                                }
                            }
                        }
                    }
                }
                OnboardingStep.SAFETY_DISCLOSURE -> item {
                    SectionCard(selectedProgram?.displayName ?: "Safety boundary") {
                        Text(selectedProgram?.safety?.tier?.safetyCopy() ?: "Choose a primary recovery focus first.")
                        CheckRow("I understand this safety boundary", draft.safetyAcknowledged, onSafetyChanged)
                    }
                }
                OnboardingStep.GOAL_TYPE -> item {
                    SectionCard("Choose a starting goal") {
                        RecoveryGoalType.entries.forEach { goal ->
                            ChoiceChip(
                                selected = draft.goalType == goal,
                                label = goal.label(),
                                onClick = { onGoalChanged(goal) },
                            )
                        }
                    }
                }
                OnboardingStep.GOAL_DETAILS -> item {
                    GoalDetails(draft, onGoalDetailsChanged)
                }
                OnboardingStep.BASELINE -> item {
                    var baselineText by remember(draft.baselineValue) {
                        mutableStateOf(draft.baselineValue?.toString().orEmpty())
                    }
                    SectionCard("Optional baseline") {
                        Text("A baseline helps DeAddict show change over time. You can skip it.")
                        OutlinedTextField(
                            value = baselineText,
                            onValueChange = { value ->
                                baselineText = value.filter { it.isDigit() || it == '.' }.take(10)
                                onBaselineChanged(baselineText.toDoubleOrNull())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Typical amount, time, or cost") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                }
                OnboardingStep.TRIGGERS -> item {
                    SectionCard("What tends to trigger it?") {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            listOf("stress", "boredom", "social", "routine", "access").forEach { trigger ->
                                FilterChip(
                                    selected = trigger in draft.triggerKeys,
                                    onClick = { onTriggerToggled(trigger) },
                                    label = { Text(trigger.replaceFirstChar(Char::uppercase)) },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                        Text("These answers stay private and can be changed later.")
                    }
                }
                OnboardingStep.SUPPORTING_TRACKS -> item {
                    SectionCard("Supporting tracks") {
                        Text("Start with one clear primary journey. Add supporting tracks from the Tracks tab after setup.")
                    }
                }
                OnboardingStep.ROOK -> item {
                    val highRisk = selectedProgram?.safety?.tier == SafetyTier.MEDICALLY_HIGH_RISK
                    SectionCard("Choose Rook’s style") {
                        ChoiceChip(draft.rookTone == RookTone.DIRECT, "Direct", { onRookToneChanged(RookTone.DIRECT) })
                        ChoiceChip(draft.rookTone == RookTone.QUIET, "Quiet", { onRookToneChanged(RookTone.QUIET) })
                        if (!highRisk) {
                            ChoiceChip(
                                draft.rookTone == RookTone.BRUTAL_BANTER,
                                "Brutal Banter — explicit opt-in",
                                { onRookToneChanged(RookTone.BRUTAL_BANTER) },
                            )
                        }
                        Text("Rook challenges excuses and patterns, never your worth. Safety language always overrides tone.")
                    }
                }
                OnboardingStep.NOTIFICATIONS -> item {
                    SectionCard("Private reminders") {
                        CheckRow(
                            "Remember my preference to enable a daily check-in",
                            draft.notificationsEnabled,
                            onNotificationsChanged,
                        )
                        Text("Android permission is requested only after setup. Reminder text never names a habit.")
                    }
                }
                OnboardingStep.FIRST_CHECK_IN -> item {
                    SectionCard("Your first action") {
                        Text("After setup, Today will offer a short private check-in for this Recovery Track.")
                    }
                }
                OnboardingStep.SUMMARY -> item {
                    SectionCard("Ready to begin") {
                        Metric("Primary track", selectedProgram?.displayName ?: "Not selected")
                        Metric("Goal", draft.goalType?.label() ?: "Not selected")
                        Metric("Rook", draft.rookTone.label())
                        Metric("Storage", if (draft.usageMode == OnboardingUsageMode.PRIVATE_ON_DEVICE) "On device" else "Local first")
                        Text("Creating the track is transactional. Your saved answers remain if setup cannot finish.")
                    }
                }
                OnboardingStep.COMPLETE -> item {
                    SectionCard("Setup complete") { Text("Your Recovery Track is ready.") }
                }
            }
        }
    }
}

@Composable
private fun GoalDetails(
    draft: OnboardingDraft,
    onChanged: (Double?, String?) -> Unit,
) {
    if (!draft.goalType.requiresTarget()) {
        SectionCard("No number required") {
            Text("This goal can begin without a numeric target. You can refine it later.")
        }
        return
    }
    var target by remember(draft.goalType, draft.goalTarget) {
        mutableStateOf(draft.goalTarget?.toString().orEmpty())
    }
    var unit by remember(draft.goalType, draft.goalUnit) { mutableStateOf(draft.goalUnit.orEmpty()) }
    SectionCard("Set a realistic target") {
        OutlinedTextField(
            value = target,
            onValueChange = { value ->
                target = value.filter { it.isDigit() || it == '.' }.take(10)
                onChanged(target.toDoubleOrNull(), unit)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Target") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = unit,
            onValueChange = { value ->
                unit = value.take(30)
                onChanged(target.toDoubleOrNull(), unit)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Unit, for example minutes or dollars") },
        )
    }
}

@Composable
private fun MainScaffold(
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
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.selectedTab) {
                AppTab.TODAY -> TodayScreen(state)
                AppTab.TRACKS -> TracksScreen(
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
                AppTab.TOOLS -> ToolsScreen(
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
                AppTab.INSIGHTS -> InsightsScreen(state, selectedTrack)
                AppTab.YOU -> YouScreen(
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
private fun TodayScreen(state: AppUiState) {
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
        primary?.let { item { TrackSummaryCard(it, true) } }
        items(supporting, key = { it.id }) { TrackSummaryCard(it, false) }
        item {
            SectionCard("Next action") {
                Text("Open Tracks for a private check-in, or Tools when an urge needs immediate attention.")
            }
        }
    }
}

@Composable
private fun TrackSummaryCard(track: RecoveryTrackUi, primary: Boolean) {
    SectionCard(track.title) {
        Text(
            if (primary) "Primary recovery track" else "Supporting recovery track",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text("${track.status.label()} · Progress and lapses stay independent.")
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
private fun TracksScreen(
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
                        label = { Text(if (track.isPrimary) "${track.title} · Primary" else track.title) },
                        modifier = Modifier.testTag("track_${track.id}"),
                    )
                }
            }
        }
        item {
            LifecycleCard(
                track = selectedTrack,
                onMakePrimary = onMakePrimary,
                onPauseTrack = onPauseTrack,
                onResumeTrack = onResumeTrack,
                onMaintenanceTrack = onMaintenanceTrack,
                onArchiveRequested = { archiveCandidate = it },
            )
        }
        item { TrackingCard(selectedTrack, onTrackingRecorded) }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add recovery track") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(addablePrograms, key = { it.id.value }) { program ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
private fun LifecycleCard(
    track: RecoveryTrackUi,
    onMakePrimary: (String) -> Unit,
    onPauseTrack: (String) -> Unit,
    onResumeTrack: (String) -> Unit,
    onMaintenanceTrack: (String) -> Unit,
    onArchiveRequested: (RecoveryTrackUi) -> Unit,
) {
    SectionCard(track.title) {
        Text("${track.role.label()} · ${track.status.label()}")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            if (!track.isPrimary && track.status in setOf(RecoveryTrackStatus.ACTIVE, RecoveryTrackStatus.MAINTENANCE)) {
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
private fun TrackingCard(track: RecoveryTrackUi, onTrackingRecorded: (TrackingEntry) -> Unit) {
    var intensity by remember(track.id) { mutableIntStateOf(3) }
    var selectedKind by remember(track.id) { mutableStateOf(TrackingEventKind.URGE) }
    var numericValue by remember(track.id) { mutableStateOf("") }
    var selectedTrigger by remember(track.id) { mutableStateOf<String?>(null) }
    val needsIntensity = selectedKind in setOf(TrackingEventKind.URGE, TrackingEventKind.CRAVING, TrackingEventKind.SLIP)
    val needsValue = selectedKind in setOf(TrackingEventKind.QUANTITY, TrackingEventKind.TIME, TrackingEventKind.COST)
    val parsedValue = numericValue.toDoubleOrNull()
    val canSave = !needsValue || (parsedValue != null && parsedValue > 0)

    SectionCard("Private check-in") {
        Text(
            "Saving to ${track.title}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("tracking_destination"),
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
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
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        if (needsIntensity) IntensityPicker(intensity) { intensity = it }
        if (needsValue) {
            OutlinedTextField(
                value = numericValue,
                onValueChange = { value -> numericValue = value.filter { it.isDigit() || it == '.' }.take(10) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (selectedKind == TrackingEventKind.TIME) "Minutes" else "Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf("stress", "boredom", "social", "routine").forEach { trigger ->
                FilterChip(
                    selected = selectedTrigger == trigger,
                    onClick = { selectedTrigger = if (selectedTrigger == trigger) null else trigger },
                    label = { Text(trigger.replaceFirstChar(Char::uppercase)) },
                    modifier = Modifier.padding(end = 8.dp),
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
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_tracking"),
        ) { Text(if (selectedKind == TrackingEventKind.SLIP) "Record slip" else "Save to ${track.title}") }
    }
}

@Composable
private fun ToolsScreen(
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
    val rescueTrack = state.recoveryTracks.firstOrNull { it.id == rescue.recoveryTrackId }
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
        val ownerLabel = rescueTrack?.title ?: selectedTrack?.title
        ownerLabel?.let {
            Text(
                if (rescue.step == RescueStep.READY) "Rescue will be saved to $it" else "Rescue for $it",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("rescue_destination"),
            )
        }
        when (rescue.step) {
            RescueStep.READY -> {
                Text("Tools", style = MaterialTheme.typography.displaySmall)
                Text("You do not have to decide everything right now.", textAlign = TextAlign.Center)
                Button(onClick = onBegin, enabled = selectedTrack != null) { Text("Begin two-minute Rescue") }
                Text("Works offline")
            }
            RescueStep.PAUSE -> {
                Text("Pause", style = MaterialTheme.typography.displaySmall)
                Text(rescue.secondsRemaining.toString(), style = MaterialTheme.typography.displayLarge)
                Text(if (rescue.secondsRemaining % 10 < 5) "Breathe in slowly" else "Breathe out gently")
                Button(enabled = rescue.secondsRemaining == 0, onClick = onContinue) { Text("Continue") }
            }
            RescueStep.MOTIVATION -> {
                Text("Remember why", style = MaterialTheme.typography.headlineLarge)
                Text(rescue.motivation, textAlign = TextAlign.Center)
                Button(onClick = onContinue) { Text("Keep going") }
            }
            RescueStep.INITIAL_URGE -> {
                Text("How strong is the urge?", style = MaterialTheme.typography.headlineMedium)
                IntensityPicker(rescue.initialUrge, onInitialUrge)
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
                    Button(onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth()) { Text(action) }
                }
            }
            RescueStep.RECHECK -> {
                Text("Check in again", style = MaterialTheme.typography.headlineMedium)
                IntensityPicker(rescue.finalUrge, onFinalUrge)
                Button(onClick = onComplete) { Text("Record outcome") }
            }
            RescueStep.COMPLETE -> {
                Text("Recovery continues", style = MaterialTheme.typography.headlineLarge)
                Text("The history remains attached to ${rescueTrack?.title ?: "this Recovery Track"}.")
                if (rescue.program?.safety?.tier == SafetyTier.MEDICALLY_HIGH_RISK) {
                    Text(
                        "For immediate danger, severe withdrawal, or overdose risk, contact local emergency services.",
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
private fun InsightsScreen(state: AppUiState, selectedTrack: RecoveryTrackUi?) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Insights", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Only ${selectedTrack?.title ?: "the selected journey"} is included.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val insights = state.insights
        if (insights == null) {
            item { SectionCard("Not enough data") { Text("Record a few check-ins to begin seeing patterns.") } }
        } else {
            item {
                SectionCard("Seven-day summary") {
                    Metric("Check-ins", insights.checkInCount.toString())
                    Metric("Slips recorded", insights.slipCount.toString())
                    Metric("Average intensity", insights.averageUrge?.let { "%.1f / 5".format(it) } ?: "Not enough data")
                    Metric("Rescue sessions", insights.rescueCount.toString())
                    Text(insights.explanation)
                }
            }
        }
    }
}

@Composable
private fun YouScreen(
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
            text = { Text("Recovery Tracks, check-ins, Rescue sessions, and local insights will be removed from this device.") },
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
            Text("Privacy, reminders, usage access, and subscription.")
        }
        item {
            SectionCard("DeAddict Plus") {
                if (state.billing.entitlement == EntitlementTier.PLUS) {
                    Text("Plus is active", color = MaterialTheme.colorScheme.primary)
                } else {
                    state.billing.offers.forEach { offer ->
                        Button(onClick = { onPurchasePlus(offer.offerToken) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${offer.basePlanId}: ${offer.formattedPrice}")
                        }
                    }
                    if (state.billing.offers.isEmpty()) Text(if (state.billing.loading) "Loading plans…" else "Plans unavailable")
                    TextButton(onClick = onRestorePurchases) { Text("Restore purchases") }
                }
                if (state.billing.verification in setOf(
                        PurchaseVerificationStatus.PENDING,
                        PurchaseVerificationStatus.BACKEND_UNAVAILABLE,
                    )
                ) {
                    Text("Purchase verification is required before entitlement activation.")
                }
            }
        }
        item {
            SectionCard("Digital usage") {
                if (!state.usageAccessGranted) {
                    Text("Optional Android usage access can estimate app time and openings.")
                    Button(onClick = onRequestUsageAccess) { Text("Choose usage access") }
                } else {
                    Text("Usage access is enabled")
                    state.dailyUsage?.let { usage ->
                        Metric("Estimated app time", "${usage.totalForegroundMinutes} min")
                        Metric("Estimated openings", usage.totalOpeningEstimate.toString())
                    }
                    TextButton(onClick = onRequestUsageAccess) { Text("Manage in Android settings") }
                }
            }
        }
        item {
            SectionCard("Privacy and security") {
                ToggleRow(
                    "Biometric app lock",
                    state.privacyPreferences.biometricLockEnabled,
                    { if (it) onEnableBiometricLock() else onDisableBiometricLock() },
                )
                ToggleRow("Protect screenshots", state.privacyPreferences.screenProtectionEnabled, onScreenProtectionChanged)
                ToggleRow("Usage monitoring", state.privacyPreferences.usageMonitoringEnabled, onUsageMonitoringChanged)
                ToggleRow("Anonymous analytics", state.privacyPreferences.analyticsEnabled, onAnalyticsChanged)
            }
        }
        item {
            SectionCard("Private reminders") {
                Text("Quiet hours: ${state.notificationPreferences.quietStartHour}:00–${state.notificationPreferences.quietEndHour}:00")
                if (state.notificationPreferences.dailyCheckInEnabled) {
                    TextButton(onClick = onDisableDailyNotifications) { Text("Turn off daily check-in") }
                } else {
                    Button(onClick = onEnableDailyNotifications) { Text("Enable daily check-in") }
                }
            }
        }
        item {
            SectionCard("Your data") {
                Text("Private notes remain local. Delete local recovery data separately from account deletion.")
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete local recovery data", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChanged)
        Text(label, Modifier.weight(1f))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChanged, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable Column.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IntensityPicker(selected: Int, onSelected: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (1..5).forEach { value ->
            if (selected == value) Button(onClick = { onSelected(value) }) { Text(value.toString()) }
            else TextButton(onClick = { onSelected(value) }) { Text(value.toString()) }
        }
    }
}

@Composable
private fun LockedScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DeAddict is locked", style = MaterialTheme.typography.headlineMedium)
            Text("Confirm your identity to continue.")
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

private fun OnboardingStep.title(): String = when (this) {
    OnboardingStep.WELCOME -> "Begin privately"
    OnboardingStep.PRIVACY -> "Privacy first"
    OnboardingStep.USAGE_MODE -> "Choose your mode"
    OnboardingStep.MOTIVATION -> "Your reason"
    OnboardingStep.PRIMARY_PROGRAM -> "Primary recovery focus"
    OnboardingStep.SAFETY_DISCLOSURE -> "Safety boundary"
    OnboardingStep.GOAL_TYPE -> "Starting goal"
    OnboardingStep.GOAL_DETAILS -> "Goal details"
    OnboardingStep.BASELINE -> "Starting baseline"
    OnboardingStep.TRIGGERS -> "Common triggers"
    OnboardingStep.SUPPORTING_TRACKS -> "Supporting tracks"
    OnboardingStep.ROOK -> "Meet Rook"
    OnboardingStep.NOTIFICATIONS -> "Private reminders"
    OnboardingStep.FIRST_CHECK_IN -> "First action"
    OnboardingStep.SUMMARY -> "Review setup"
    OnboardingStep.COMPLETE -> "Complete"
}

private fun RecoveryGoalType.label(): String = when (this) {
    RecoveryGoalType.QUIT_COMPLETELY -> "Quit completely"
    RecoveryGoalType.REDUCE_QUANTITY -> "Reduce quantity"
    RecoveryGoalType.DAILY_LIMIT -> "Daily limit"
    RecoveryGoalType.WEEKLY_LIMIT -> "Weekly limit"
    RecoveryGoalType.TIME_LIMIT -> "Time limit"
    RecoveryGoalType.SPENDING_LIMIT -> "Spending limit"
    RecoveryGoalType.DELAY_FIRST_USE -> "Delay first use"
    RecoveryGoalType.NO_USE_PERIOD -> "No-use period"
    RecoveryGoalType.AWARENESS_ONLY -> "Awareness only"
    RecoveryGoalType.CUSTOM -> "Custom"
}

private fun RecoveryGoalType?.requiresTarget(): Boolean = this in setOf(
    RecoveryGoalType.REDUCE_QUANTITY,
    RecoveryGoalType.DAILY_LIMIT,
    RecoveryGoalType.WEEKLY_LIMIT,
    RecoveryGoalType.TIME_LIMIT,
    RecoveryGoalType.SPENDING_LIMIT,
    RecoveryGoalType.DELAY_FIRST_USE,
)

private fun RookTone.label(): String = when (this) {
    RookTone.DIRECT -> "Direct"
    RookTone.BRUTAL_BANTER -> "Brutal Banter"
    RookTone.QUIET -> "Quiet"
}

private fun RecoveryTrackRole.label(): String = when (this) {
    RecoveryTrackRole.PRIMARY -> "Primary"
    RecoveryTrackRole.SUPPORTING -> "Supporting"
}

private fun RecoveryTrackStatus.label(): String = when (this) {
    RecoveryTrackStatus.ACTIVE -> "Active"
    RecoveryTrackStatus.PAUSED -> "Paused"
    RecoveryTrackStatus.MAINTENANCE -> "Maintenance"
    RecoveryTrackStatus.ARCHIVED -> "Archived"
}

private fun ProgramCategory.label(): String = when (this) {
    ProgramCategory.SUBSTANCE -> "Substances"
    ProgramCategory.DIGITAL -> "Digital habits"
    ProgramCategory.BEHAVIOURAL -> "Behavioural habits"
}

private fun SafetyTier.supportLabel(): String = when (this) {
    SafetyTier.GENERAL_SELF_MANAGEMENT -> "Self-management support"
    SafetyTier.CLINICALLY_SENSITIVE -> "Sensitive support"
    SafetyTier.MEDICALLY_HIGH_RISK -> "Professional guidance recommended"
}

private fun SafetyTier.safetyCopy(): String = when (this) {
    SafetyTier.GENERAL_SELF_MANAGEMENT ->
        "DeAddict can support awareness, limits, replacement actions, and private check-ins."
    SafetyTier.CLINICALLY_SENSITIVE ->
        "This area may benefit from qualified professional support. DeAddict does not diagnose or provide treatment."
    SafetyTier.MEDICALLY_HIGH_RISK ->
        "Stopping suddenly can carry serious medical risk. DeAddict does not provide detox or taper instructions. Seek qualified medical guidance before major changes."
}
