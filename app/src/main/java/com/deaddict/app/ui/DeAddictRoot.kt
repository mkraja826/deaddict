package com.deaddict.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.app.rescue.RescueStep
import kotlinx.coroutines.delay
import com.deaddict.app.insights.TrendDirection
import com.deaddict.programs.ProgramCategory
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.SafetyTier
import com.deaddict.app.billing.EntitlementTier
import com.deaddict.app.billing.PurchaseVerificationStatus
import com.deaddict.app.R

@Composable
fun DeAddictRoot(
    state: AppUiState,
    isAppUnlocked: Boolean,
    onTabSelected: (AppTab) -> Unit,
    onProgramSelected: (ProgramDefinition) -> Unit,
    onTrackingRecorded: (ProgramDefinition, TrackingEntry) -> Unit,
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
    if (!isAppUnlocked) {
        LockedScreen()
        return
    }
    when {
        state.isLoading -> LoadingScreen()
        state.requiresOnboarding -> ProgramOnboarding(
            programs = state.availablePrograms,
            onSelected = onProgramSelected,
        )
        else -> AppScaffold(
            state,
            onTabSelected,
            onTrackingRecorded,
            onRequestUsageAccess,
            onBeginRescue,
            onRescueTick,
            onRescueContinue,
            onRescueInitialUrge,
            onRescueTrigger,
            onRescueAction,
            onRescueFinalUrge,
            onRescueComplete,
            onRescueReset,
            onEnableDailyNotifications,
            onDisableDailyNotifications,
            onEnableBiometricLock,
            onDisableBiometricLock,
            onScreenProtectionChanged,
            onAnalyticsChanged,
            onUsageMonitoringChanged,
            onDeleteLocalData,
            onPurchasePlus,
            onRestorePurchases,
        )
    }
}

@Composable
private fun AppScaffold(
    state: AppUiState,
    onTabSelected: (AppTab) -> Unit,
    onTrackingRecorded: (ProgramDefinition, TrackingEntry) -> Unit,
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
    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Text(
                                when (tab) {
                                    AppTab.HOME -> "●"
                                    AppTab.TRACK -> "+"
                                    AppTab.RESCUE -> "◉"
                                    AppTab.INSIGHTS -> "↗"
                                    AppTab.PROFILE -> "○"
                                },
                            )
                        },
                        label = { Text(tab.localizedLabel()) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state.selectedTab) {
                AppTab.HOME -> HomeScreen(state.activePrograms)
                AppTab.TRACK -> TrackScreen(state.activePrograms, onTrackingRecorded)
                AppTab.RESCUE -> RescueScreen(
                    state = state,
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
                AppTab.INSIGHTS -> InsightsScreen(state)
                AppTab.PROFILE -> ProfileScreen(
                    state,
                    onRequestUsageAccess,
                    onEnableDailyNotifications,
                    onDisableDailyNotifications,
                    onEnableBiometricLock,
                    onDisableBiometricLock,
                    onScreenProtectionChanged,
                    onAnalyticsChanged,
                    onUsageMonitoringChanged,
                    onDeleteLocalData,
                    onPurchasePlus,
                    onRestorePurchases,
                )
            }
            state.message?.let {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramOnboarding(
    programs: List<ProgramDefinition>,
    onSelected: (ProgramDefinition) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("What would you like support with?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose one to begin privately. You can change this later.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProgramCategory.entries.forEach { category ->
            item {
                Text(
                    text = category.displayName(),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(
                items = programs.filter { it.category == category },
                key = { it.id.value },
            ) { program ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelected(program) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(program.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            program.safety.tier.supportLabel(),
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
private fun HomeScreen(activePrograms: List<ProgramDefinition>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Today", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Notice what is happening. One small choice is enough.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(activePrograms, key = { it.id.value }) { program ->
            ProgramProgressCard(program)
            RecoveryPlanCard(program)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Next action", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Take a ten-second check-in and name what you need right now.")
                }
            }
        }
    }
}

@Composable
private fun ProgramProgressCard(program: ProgramDefinition) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(program.displayName, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Your progress starts with noticing, not perfection.")
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                "Recovery continues, including after difficult moments.",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RecoveryPlanCard(program: ProgramDefinition) {
    val guidance = when (program.safety.tier) {
        SafetyTier.GENERAL_SELF_MANAGEMENT ->
            "Observe patterns · Set a realistic limit · Prepare a replacement action"
        SafetyTier.CLINICALLY_SENSITIVE ->
            "Notice triggers · Reduce easy access · Consider trusted professional support"
        SafetyTier.MEDICALLY_HIGH_RISK ->
            "Track safely · Use Rescue for urges · Speak with a qualified professional before changing use"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Your recovery plan", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(guidance)
            if (program.safety.tier == SafetyTier.MEDICALLY_HIGH_RISK) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Stopping suddenly can carry medical risk for some substances. DeAddict does not provide detox or taper instructions.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TrackScreen(
    activePrograms: List<ProgramDefinition>,
    onTrackingRecorded: (ProgramDefinition, TrackingEntry) -> Unit,
) {
    var intensity by remember { mutableIntStateOf(3) }
    var selectedKind by remember { mutableStateOf(TrackingEventKind.URGE) }
    var numericValue by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf<String?>(null) }
    val program = activePrograms.first()
    val needsIntensity = selectedKind in setOf(
        TrackingEventKind.URGE,
        TrackingEventKind.CRAVING,
        TrackingEventKind.SLIP,
    )
    val needsValue = selectedKind in setOf(
        TrackingEventKind.QUANTITY,
        TrackingEventKind.TIME,
        TrackingEventKind.COST,
    )
    val parsedValue = numericValue.toDoubleOrNull()
    val canSave = !needsValue || (parsedValue != null && parsedValue > 0)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Track", style = MaterialTheme.typography.headlineLarge)
            Text(program.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("What would you like to record?", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TrackingEventKind.URGE to "Urge",
                    TrackingEventKind.CRAVING to "Craving",
                    TrackingEventKind.SLIP to "Slip",
                    TrackingEventKind.QUANTITY to "Quantity",
                    TrackingEventKind.TIME to "Time",
                    TrackingEventKind.COST to "Cost",
                    TrackingEventKind.ACTIVITY to "Activity",
                ).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (kind, label) ->
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
                }
            }
        }
        if (needsIntensity) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            when (selectedKind) {
                                TrackingEventKind.SLIP -> "How difficult did this moment feel?"
                                else -> "How strong is it?"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            (1..5).forEach { value ->
                                if (value == intensity) {
                                    Button(onClick = { intensity = value }) { Text("$value") }
                                } else {
                                    TextButton(onClick = { intensity = value }) { Text("$value") }
                                }
                            }
                        }
                        Text(
                            "1 is mild · 5 is intense",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        if (needsValue) {
            item {
                OutlinedTextField(
                    value = numericValue,
                    onValueChange = { candidate ->
                        if (candidate.length <= 10 && candidate.count { it == '.' } <= 1) {
                            numericValue = candidate.filter { it.isDigit() || it == '.' }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (selectedKind) {
                                TrackingEventKind.TIME -> "Minutes"
                                TrackingEventKind.COST -> "Cost in your local currency"
                                else -> "Quantity"
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = {
                        if (numericValue.isNotEmpty() && !canSave) Text("Enter a value greater than zero")
                    },
                    isError = numericValue.isNotEmpty() && !canSave,
                )
            }
        }
        item {
            Text("What was happening?", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("stress", "boredom", "social", "routine").forEach { trigger ->
                    FilterChip(
                        selected = selectedTrigger == trigger,
                        onClick = {
                            selectedTrigger = if (selectedTrigger == trigger) null else trigger
                        },
                        label = { Text(trigger.replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
        }
        item {
            Button(
                enabled = canSave,
                onClick = {
                    onTrackingRecorded(
                        program,
                        TrackingEntry(
                            kind = selectedKind,
                            value = parsedValue,
                            urgeIntensity = if (needsIntensity) intensity else null,
                            triggerKey = selectedTrigger,
                        ),
                    )
                    numericValue = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (selectedKind == TrackingEventKind.SLIP) "Record slip" else "Save check-in")
            }
            Text(
                "Saved privately on this device.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RescueScreen(
    state: AppUiState,
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (rescue.step) {
            RescueStep.READY -> {
                Text("Rescue", style = MaterialTheme.typography.displaySmall)
                Text(
                    "You do not have to decide everything right now.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = onBegin) { Text("Begin two-minute Rescue") }
                Text("Works offline", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RescueStep.PAUSE -> {
                Text("Pause", style = MaterialTheme.typography.displaySmall)
                Text(
                    rescue.secondsRemaining.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (rescue.secondsRemaining % 10 < 5) "Breathe in slowly" else "Breathe out gently",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text("Let the next minute create a little space.", textAlign = TextAlign.Center)
                Button(
                    enabled = rescue.secondsRemaining == 0,
                    onClick = onContinue,
                ) { Text("Continue") }
            }
            RescueStep.MOTIVATION -> {
                Text("Remember why", style = MaterialTheme.typography.headlineLarge)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        rescue.motivation,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
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
                    Button(
                        onClick = { onTrigger(trigger) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(trigger.replaceFirstChar(Char::uppercase))
                    }
                }
            }
            RescueStep.REPLACEMENT -> {
                Text("Choose one small action", style = MaterialTheme.typography.headlineMedium)
                Text("Any of these can help create distance from the urge.")
                rescue.replacementActions.forEach { action ->
                    Button(
                        onClick = { onAction(action) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) { Text(action) }
                }
            }
            RescueStep.RECHECK -> {
                Text("Check in again", style = MaterialTheme.typography.headlineMedium)
                Text("How strong is the urge now?")
                IntensityPicker(rescue.finalUrge, onFinalUrge)
                Button(onClick = onComplete) { Text("Record outcome") }
            }
            RescueStep.COMPLETE -> {
                Text("Recovery continues", style = MaterialTheme.typography.headlineLarge)
                Text(
                    when {
                        rescue.finalUrge < rescue.initialUrge ->
                            "The urge eased. You created space and learned what helped."
                        rescue.finalUrge == rescue.initialUrge ->
                            "The urge is still here, and your pause still counts."
                        else ->
                            "This feels difficult right now. Your progress still counts."
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (rescue.program?.safety?.tier == SafetyTier.MEDICALLY_HIGH_RISK) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.safety_immediate_danger),
                            modifier = Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Button(onClick = onReset) { Text("Done") }
            }
        }
    }
}

@Composable
private fun IntensityPicker(selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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
private fun ComingSoon(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(description, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileScreen(
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
            text = {
                Text("Programs, check-ins, Rescue sessions, and local insights will be permanently removed from this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteLocalData()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.tab_profile), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.profile_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.plus_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (state.billing.entitlement == EntitlementTier.PLUS) {
                        Text(stringResource(R.string.plus_active), color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(
                            stringResource(R.string.plus_always_free),
                        )
                        Spacer(Modifier.height(10.dp))
                        state.billing.offers.forEach { offer ->
                            Button(
                                onClick = { onPurchasePlus(offer.offerToken) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${offer.basePlanId}: ${offer.formattedPrice}")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (state.billing.offers.isEmpty()) {
                            Text(
                                if (state.billing.loading) {
                                    stringResource(R.string.plus_loading)
                                } else {
                                    stringResource(R.string.plus_unavailable)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    state.billing.message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    if (
                        state.billing.verification == PurchaseVerificationStatus.PENDING ||
                        state.billing.verification == PurchaseVerificationStatus.BACKEND_UNAVAILABLE
                    ) {
                        Text(
                            stringResource(R.string.plus_verification_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onRestorePurchases) {
                        Text(stringResource(R.string.restore_purchases))
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Digital usage", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (!state.usageAccessGranted) {
                        Text(
                            "Optional usage access lets DeAddict estimate app time, openings, sessions, and risk periods.",
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRequestUsageAccess) {
                            Text("Choose usage access")
                        }
                    } else {
                        Text("Usage access is enabled", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        state.dailyUsage?.let { usage ->
                            UsageMetric("Estimated app time today", "${usage.totalForegroundMinutes} min")
                            UsageMetric("Estimated openings", usage.totalOpeningEstimate.toString())
                            UsageMetric("Approximate sessions", usage.totalSessionEstimate.toString())
                            UsageMetric("Rapid reopenings", usage.rapidReopenings.toString())
                            UsageMetric("Late-night openings", usage.lateNightOpenings.toString())
                            UsageMetric("Morning openings", usage.morningOpenings.toString())
                        } ?: Text("No usage estimate is available yet.")
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onRequestUsageAccess) {
                            Text("Manage in Android settings")
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.privacy_security), style = MaterialTheme.typography.titleLarge)
                    PrivacyToggle(
                        label = "Biometric app lock",
                        description = "Require biometrics or your device credential when returning.",
                        checked = state.privacyPreferences.biometricLockEnabled,
                        onChanged = {
                            if (it) onEnableBiometricLock() else onDisableBiometricLock()
                        },
                    )
                    HorizontalDivider()
                    PrivacyToggle(
                        label = "Protect screenshots and previews",
                        description = "Blocks screenshots and obscures the recent-app preview.",
                        checked = state.privacyPreferences.screenProtectionEnabled,
                        onChanged = onScreenProtectionChanged,
                    )
                    HorizontalDivider()
                    PrivacyToggle(
                        label = "Usage monitoring",
                        description = "Controls whether DeAddict uses granted app-level usage access.",
                        checked = state.privacyPreferences.usageMonitoringEnabled,
                        onChanged = onUsageMonitoringChanged,
                    )
                    HorizontalDivider()
                    PrivacyToggle(
                        label = "Anonymous analytics",
                        description = "Off by default. Journal and sensitive content are never included.",
                        checked = state.privacyPreferences.analyticsEnabled,
                        onChanged = onAnalyticsChanged,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Your data", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Delete recovery data stored on this device. Privacy settings are retained.")
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.delete_local_data), color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        "Cloud and account deletion are unavailable while cloud services are disconnected.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Private reminders", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Daily check-ins use discreet wording and never include a habit or program name.",
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Quiet hours: ${state.notificationPreferences.quietStartHour}:00–${state.notificationPreferences.quietEndHour}:00",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.notificationPreferences.dailyCheckInEnabled) {
                        Text("Daily check-in enabled", color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = onDisableDailyNotifications) {
                            Text("Turn off")
                        }
                    } else {
                        Button(onClick = onEnableDailyNotifications) {
                            Text("Enable daily check-in")
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("What DeAddict can see", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Approximate app-level time and foreground events provided by Android.")
                    Spacer(Modifier.height(12.dp))
                    Text("What DeAddict cannot see", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Messages, passwords, photos, searches, screen contents, browsing content, or exact Reels and Shorts activity.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyToggle(
    label: String,
    description: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun LockedScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DeAddict is locked", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Confirm your identity to continue.")
        }
    }
}

@Composable
private fun InsightsScreen(state: AppUiState) {
    val insights = state.insights
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Insights", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Explainable patterns from your last seven days.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (insights == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Record a few check-ins to begin seeing patterns.",
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Seven-day summary", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        UsageMetric("Check-ins", insights.checkInCount.toString())
                        UsageMetric("Slips recorded", insights.slipCount.toString())
                        UsageMetric(
                            "Average recorded intensity",
                            insights.averageUrge?.let { "%.1f / 5".format(it) } ?: "Not enough data",
                        )
                        UsageMetric("Trend", insights.trend.displayName())
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Patterns noticed", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        UsageMetric(
                            "Most recorded trigger",
                            insights.topTrigger?.replaceFirstChar(Char::uppercase) ?: "Not enough data",
                        )
                        UsageMetric(
                            "Most active period",
                            insights.peakRiskPeriod?.replaceFirstChar(Char::uppercase) ?: "Not enough data",
                        )
                        UsageMetric("Rescue sessions", insights.rescueCount.toString())
                        UsageMetric(
                            "Rescues where intensity eased",
                            insights.rescuesWithReducedUrge.toString(),
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Why you’re seeing this", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(insights.explanation)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "These are recorded patterns, not a diagnosis or prediction.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun TrendDirection.displayName(): String = when (this) {
    TrendDirection.IMPROVING -> "Intensity easing"
    TrendDirection.STEADY -> "Mostly steady"
    TrendDirection.INCREASING -> "Intensity increasing"
    TrendDirection.NOT_ENOUGH_DATA -> "Not enough data"
}

@Composable
private fun UsageMetric(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun ProgramCategory.displayName(): String = when (this) {
    ProgramCategory.SUBSTANCE -> "Substances"
    ProgramCategory.DIGITAL -> "Digital habits"
    ProgramCategory.BEHAVIOURAL -> "Behavioural habits"
}

private fun SafetyTier.supportLabel(): String = when (this) {
    SafetyTier.GENERAL_SELF_MANAGEMENT -> "Self-management support"
    SafetyTier.CLINICALLY_SENSITIVE -> "Sensitive support"
    SafetyTier.MEDICALLY_HIGH_RISK -> "Professional guidance recommended"
}

@Composable
private fun AppTab.localizedLabel(): String = stringResource(
    when (this) {
        AppTab.HOME -> R.string.tab_home
        AppTab.TRACK -> R.string.tab_track
        AppTab.RESCUE -> R.string.tab_rescue
        AppTab.INSIGHTS -> R.string.tab_insights
        AppTab.PROFILE -> R.string.tab_profile
    },
)
