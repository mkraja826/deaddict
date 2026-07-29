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
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramDefinition
import com.deaddict.programs.SafetyTier

@Composable
fun MultiAddictionDeAddictRoot(
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
            state.recoveryTracks.isNotEmpty()
        ) {
            RecoveryTrackAndRookHeader(
                state = state,
                onProgramSelected = onProgramSelected,
                onTrackSelected = onTrackSelected,
                onMakePrimary = onMakePrimary,
                onPauseTrack = onPauseTrack,
                onResumeTrack = onResumeTrack,
                onMaintenanceTrack = onMaintenanceTrack,
                onArchiveTrack = onArchiveTrack,
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
    onMakePrimary: (String) -> Unit,
    onPauseTrack: (String) -> Unit,
    onResumeTrack: (String) -> Unit,
    onMaintenanceTrack: (String) -> Unit,
    onArchiveTrack: (String) -> Unit,
    onRookToneChanged: (RookTone) -> Unit,
) {
    val selected = state.selectedRecoveryTrack ?: state.recoveryTracks.firstOrNull() ?: return
    val openProgramIds = state.recoveryTracks.mapTo(mutableSetOf()) { it.program.id.value }
    val availablePrograms = state.availablePrograms.filterNot { it.id.value in openProgramIds }
    var showAddTrackDialog by remember { mutableStateOf(false) }
    var archiveCandidate by remember { mutableStateOf<RecoveryTrackUi?>(null) }

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
                programName = selected.title,
                activeTrackCount = state.recoveryTracks.size,
                requestedTone = state.rookPreferences.tone,
                medicallyHighRisk = selected.program.safety.tier == SafetyTier.MEDICALLY_HIGH_RISK,
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
                Column {
                    Text(
                        text = if (state.recoveryTracks.size == 1) {
                            "Recovery track"
                        } else {
                            "Recovery tracks"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${selected.role.label()} · ${selected.status.label()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                state.recoveryTracks.forEach { track ->
                    FilterChip(
                        selected = track.id == state.selectedRecoveryTrackId,
                        onClick = { onTrackSelected(track.id) },
                        label = {
                            Text(
                                if (track.role == RecoveryTrackRole.PRIMARY) {
                                    "${track.title} · Primary"
                                } else {
                                    track.title
                                },
                            )
                        },
                    )
                }
            }

            TrackLifecycleActions(
                track = selected,
                onMakePrimary = onMakePrimary,
                onPauseTrack = onPauseTrack,
                onResumeTrack = onResumeTrack,
                onMaintenanceTrack = onMaintenanceTrack,
                onArchiveRequested = { archiveCandidate = it },
            )

            rookMessage?.let { rook ->
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
                            text = rook.text,
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

    archiveCandidate?.let { track ->
        AlertDialog(
            onDismissRequest = { archiveCandidate = null },
            title = { Text("Archive ${track.title}?") },
            text = {
                Text("Its history will be preserved. Continuing later will create a new recovery journey.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        archiveCandidate = null
                        onArchiveTrack(track.id)
                    },
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveCandidate = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TrackLifecycleActions(
    track: RecoveryTrackUi,
    onMakePrimary: (String) -> Unit,
    onPauseTrack: (String) -> Unit,
    onResumeTrack: (String) -> Unit,
    onMaintenanceTrack: (String) -> Unit,
    onArchiveRequested: (RecoveryTrackUi) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (
            track.role != RecoveryTrackRole.PRIMARY &&
            track.status in setOf(RecoveryTrackStatus.ACTIVE, RecoveryTrackStatus.MAINTENANCE)
        ) {
            TextButton(onClick = { onMakePrimary(track.id) }) {
                Text("Make primary")
            }
        }
        when (track.status) {
            RecoveryTrackStatus.ACTIVE -> {
                TextButton(onClick = { onPauseTrack(track.id) }) { Text("Pause") }
                TextButton(onClick = { onMaintenanceTrack(track.id) }) { Text("Maintenance") }
            }
            RecoveryTrackStatus.PAUSED -> {
                TextButton(onClick = { onResumeTrack(track.id) }) { Text("Resume") }
            }
            RecoveryTrackStatus.MAINTENANCE -> {
                TextButton(onClick = { onResumeTrack(track.id) }) { Text("Return active") }
                TextButton(onClick = { onPauseTrack(track.id) }) { Text("Pause") }
            }
            RecoveryTrackStatus.ARCHIVED -> Unit
        }
        if (track.status != RecoveryTrackStatus.ARCHIVED) {
            TextButton(onClick = { onArchiveRequested(track) }) { Text("Archive") }
        }
    }
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
