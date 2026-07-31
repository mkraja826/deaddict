package com.deaddict.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.deaddict.app.coach.RookCoachEngine
import com.deaddict.app.coach.RookCoachMessage
import com.deaddict.app.coach.RookCoachRequest
import com.deaddict.app.coach.RookMoment
import com.deaddict.app.coach.RookPreferences
import com.deaddict.app.coach.RookTone
import com.deaddict.app.coach.RookTrend
import com.deaddict.app.insights.GoalProgressTrend
import com.deaddict.app.rescue.RescueStep
import com.deaddict.programs.SafetyTier

@Composable
fun RookCoachAction(
    appState: AppUiState,
    dailyCheckInState: DailyCheckInUiState,
    insightsState: InsightsControlsUiState,
    visible: Boolean,
) {
    if (!visible || !appState.rookPreferences.enabled) return

    val request = remember(
        appState.selectedTab,
        appState.selectedRecoveryTrackId,
        appState.rookPreferences,
        appState.rescue,
        dailyCheckInState.localDateEpochDay,
        dailyCheckInState.updatedAtEpochMillis,
        dailyCheckInState.entries,
        insightsState.window,
        insightsState.insights,
    ) {
        buildRookRequest(appState, dailyCheckInState, insightsState)
    } ?: return

    val message = remember(request) { RookCoachEngine.message(request) }
    var expanded by remember(request) { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("rook_coach_action")
                .semantics {
                    contentDescription = "Open Rook coaching for ${request.trackTitle}"
                },
        ) {
            Text(if (appState.rookPreferences.avatarVisible) "Rook · ${message.title}" else message.title)
        }
    }

    if (expanded) {
        RookMessageDialog(
            message = message,
            trackTitle = request.trackTitle,
            avatarVisible = appState.rookPreferences.avatarVisible,
            onDismiss = { expanded = false },
        )
    }
}

@Composable
fun RookSettingsAction(
    visible: Boolean,
    preferences: RookPreferences,
    selectedTrack: RecoveryTrackUi?,
    onEnabledChanged: (Boolean) -> Unit,
    onDefaultToneChanged: (RookTone) -> Unit,
    onAvatarVisibleChanged: (Boolean) -> Unit,
    onTrackToneChanged: (String, RookTone?) -> Unit,
) {
    if (!visible) return

    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier
                .padding(start = 20.dp, bottom = 148.dp)
                .testTag("rook_settings_action")
                .semantics { contentDescription = "Open Rook coach settings" },
        ) {
            Text("Rook settings")
        }
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = {
                Text(
                    "Rook coach",
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "Rook challenges excuses and patterns, never your worth. Coaching is generated locally and private notes are never analyzed.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SettingsToggle(
                        label = "Enable Rook coaching",
                        checked = preferences.enabled,
                        onChanged = onEnabledChanged,
                    )
                    SettingsToggle(
                        label = "Show Rook name and avatar",
                        checked = preferences.avatarVisible,
                        onChanged = onAvatarVisibleChanged,
                    )

                    Text("Default tone", style = MaterialTheme.typography.titleMedium)
                    ToneChoices(
                        selected = preferences.tone,
                        allowDefault = false,
                        brutalAllowed = true,
                        onSelected = { tone -> tone?.let(onDefaultToneChanged) },
                    )
                    Text(
                        "Brutal Banter is blunt and playful, not degrading. Safety guidance always overrides tone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    selectedTrack?.let { track ->
                        Text(
                            "${track.title} override",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "Use a separate Rook tone for this Recovery Track while other tracks keep the default.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ToneChoices(
                            selected = preferences.trackTones[track.id],
                            allowDefault = true,
                            brutalAllowed = track.program.safety.tier == SafetyTier.GENERAL_SELF_MANAGEMENT,
                            onSelected = { tone -> onTrackToneChanged(track.id, tone) },
                        )
                        if (track.program.safety.tier != SafetyTier.GENERAL_SELF_MANAGEMENT) {
                            Text(
                                "Direct safety language is enforced for this track when risk guidance is needed.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) { Text("Done") }
            },
        )
    }
}

@Composable
private fun RookMessageDialog(
    message: RookCoachMessage,
    trackTitle: String,
    avatarVisible: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (avatarVisible) "Rook · ${message.title}" else message.title,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Rook coaching for $trackTitle. ${message.body}"
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = if (message.safetyOverride) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        message.body,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = if (message.safetyOverride) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
                Text(
                    "Tone used: ${message.toneUsed.label()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun ToneChoices(
    selected: RookTone?,
    allowDefault: Boolean,
    brutalAllowed: Boolean,
    onSelected: (RookTone?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (allowDefault) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text("Use default") },
                modifier = Modifier.fillMaxWidth().testTag("rook_tone_default"),
            )
        }
        listOf(RookTone.DIRECT, RookTone.QUIET, RookTone.BRUTAL_BANTER)
            .filter { it != RookTone.BRUTAL_BANTER || brutalAllowed }
            .forEach { tone ->
                FilterChip(
                    selected = selected == tone,
                    onClick = { onSelected(tone) },
                    label = { Text(tone.label()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rook_tone_${tone.name.lowercase()}"),
                )
            }
    }
}

private fun buildRookRequest(
    appState: AppUiState,
    dailyCheckInState: DailyCheckInUiState,
    insightsState: InsightsControlsUiState,
): RookCoachRequest? {
    val selected = appState.selectedRecoveryTrack ?: return null
    val activeRescueTrack = appState.rescue.recoveryTrackId?.let { rescueTrackId ->
        appState.recoveryTracks.firstOrNull { it.id == rescueTrackId }
    }
    val track = if (appState.selectedTab == AppTab.TOOLS) activeRescueTrack ?: selected else selected
    val tone = appState.rookPreferences.toneFor(track.id)

    return when (appState.selectedTab) {
        AppTab.TODAY -> RookCoachRequest(
            moment = RookMoment.TODAY,
            tone = tone,
            trackId = track.id,
            trackTitle = track.title,
            safetyTier = track.program.safety.tier,
            outcome = dailyCheckInState.entries[track.id]?.outcome,
            variationKey = dailyCheckInState.localDateEpochDay + dailyCheckInState.updatedAtEpochMillis,
        )

        AppTab.TRACKS -> RookCoachRequest(
            moment = RookMoment.TRACKS,
            tone = tone,
            trackId = track.id,
            trackTitle = track.title,
            safetyTier = track.program.safety.tier,
            variationKey = track.id.hashCode().toLong(),
        )

        AppTab.TOOLS -> RookCoachRequest(
            moment = when (appState.rescue.step) {
                RescueStep.READY -> RookMoment.RESCUE_READY
                RescueStep.COMPLETE -> RookMoment.RESCUE_COMPLETE
                else -> RookMoment.RESCUE_ACTIVE
            },
            tone = tone,
            trackId = track.id,
            trackTitle = track.title,
            safetyTier = track.program.safety.tier,
            initialUrge = appState.rescue.initialUrge,
            finalUrge = appState.rescue.finalUrge,
            variationKey = appState.rescue.startedAtEpochMillis ?: 0L,
        )

        AppTab.INSIGHTS -> {
            val currentGoal = insightsState.insights?.goalProgress?.currentGoal
            RookCoachRequest(
                moment = RookMoment.INSIGHTS,
                tone = tone,
                trackId = track.id,
                trackTitle = track.title,
                safetyTier = track.program.safety.tier,
                adherencePercent = currentGoal?.adherencePercent ?: currentGoal?.consistencyPercent,
                streakDays = currentGoal?.latestRunDays ?: 0,
                trend = when (currentGoal?.trend) {
                    GoalProgressTrend.IMPROVING -> RookTrend.IMPROVING
                    GoalProgressTrend.STEADY -> RookTrend.STEADY
                    GoalProgressTrend.DECLINING -> RookTrend.DECLINING
                    GoalProgressTrend.NOT_ENOUGH_DATA,
                    null,
                    -> RookTrend.NOT_ENOUGH_DATA
                },
                variationKey = insightsState.window.days.toLong(),
            )
        }

        AppTab.YOU -> null
    }
}

private fun RookTone.label(): String = when (this) {
    RookTone.DIRECT -> "Direct"
    RookTone.BRUTAL_BANTER -> "Brutal Banter"
    RookTone.QUIET -> "Quiet"
}
