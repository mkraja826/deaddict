package com.deaddict.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deaddict.app.coach.RookContext
import com.deaddict.app.coach.RookMessageEngine
import com.deaddict.app.coach.RookMoment
import com.deaddict.app.coach.RookTone
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.SafetyTier

@Composable
fun MultiAddictionDeAddictRoot(
    state: AppUiState,
    isAppUnlocked: Boolean,
    onTabSelected: (AppTab) -> Unit,
    onProgramSelected: (ProgramDefinition) -> Unit,
    onTrackSelected: (String) -> Unit,
    onRookToneChanged: (RookTone) -> Unit,
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
    Column(modifier = Modifier.fillMaxSize()) {
        if (
            isAppUnlocked &&
            !state.isLoading &&
            !state.requiresOnboarding &&
            state.activePrograms.isNotEmpty()
        ) {
            RecoveryTrackAndRookHeader(
                state = state,
                onTrackSelected = onTrackSelected,
                onRookToneChanged = onRookToneChanged,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            DeAddictRoot(
                state = state,
                isAppUnlocked = isAppUnlocked,
                onTabSelected = onTabSelected,
                onProgramSelected = onProgramSelected,
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
}

@Composable
private fun RecoveryTrackAndRookHeader(
    state: AppUiState,
    onTrackSelected: (String) -> Unit,
    onRookToneChanged: (RookTone) -> Unit,
) {
    val selected = state.activePrograms.firstOrNull() ?: return
    val rookMessage = if (state.rookPreferences.enabled) {
        RookMessageEngine.message(
            RookContext(
                moment = when (state.selectedTab) {
                    AppTab.HOME -> RookMoment.TODAY
                    AppTab.TRACK -> RookMoment.TRACK
                    AppTab.RESCUE -> RookMoment.RESCUE
                    AppTab.INSIGHTS -> RookMoment.INSIGHTS
                    AppTab.PROFILE -> RookMoment.TODAY
                },
                programName = selected.displayName,
                activeTrackCount = state.activePrograms.size,
                requestedTone = state.rookPreferences.tone,
                medicallyHighRisk = selected.safety.tier == SafetyTier.MEDICALLY_HIGH_RISK,
                slipRecorded = state.message?.contains("slip", ignoreCase = true) == true,
            ),
        )
    } else {
        null
    }

    Surface(shadowElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (state.activePrograms.size == 1) "Recovery track" else "Recovery tracks",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.activePrograms.forEachIndexed { index, program ->
                    FilterChip(
                        selected = index == 0,
                        onClick = { onTrackSelected(program.id.value) },
                        label = { Text(program.displayName) },
                    )
                }
            }

            rookMessage?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Rook",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    RookTone.DIRECT to "Direct",
                    RookTone.BRUTAL_BANTER to "Brutal banter",
                    RookTone.QUIET to "Quiet",
                ).forEach { (tone, label) ->
                    FilterChip(
                        selected = state.rookPreferences.tone == tone,
                        onClick = { onRookToneChanged(tone) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}
