package com.deaddict.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deaddict.app.coach.RookContext
import com.deaddict.app.coach.RookMessageEngine
import com.deaddict.app.coach.RookMoment
import com.deaddict.app.coach.RookTone
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
                onProgramSelected = onProgramSelected,
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
    onProgramSelected: (ProgramDefinition) -> Unit,
    onTrackSelected: (String) -> Unit,
    onRookToneChanged: (RookTone) -> Unit,
) {
    val selected = state.activePrograms.firstOrNull() ?: return
    val activeIds = state.activePrograms.mapTo(mutableSetOf()) { it.id.value }
    val availablePrograms = state.availablePrograms.filterNot { it.id.value in activeIds }
    var showAddTrackDialog by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (state.activePrograms.size == 1) "Recovery track" else "Recovery tracks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (availablePrograms.isNotEmpty()) {
                    TextButton(onClick = { showAddTrackDialog = true }) {
                        Text("Add track")
                    }
                }
            }

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

    if (showAddTrackDialog) {
        AlertDialog(
            onDismissRequest = { showAddTrackDialog = false },
            title = { Text("Add recovery track") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availablePrograms, key = { it.id.value }) { program ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showAddTrackDialog = false
                                onProgramSelected(program)
                            },
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    text = program.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "Progress, slips, Rescue sessions, and insights stay independent.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddTrackDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
