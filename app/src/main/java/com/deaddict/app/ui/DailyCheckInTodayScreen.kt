package com.deaddict.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.SafetyTier
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class DailyTrackEditor(
    val outcome: TrackCheckInOutcome? = null,
    val measuredValue: String = "",
    val unitKey: String = "",
    val peakUrge: Int? = null,
    val privateNote: String = "",
) {
    val measurementValid: Boolean
        get() {
            val value = measuredValue.trim()
            val unit = unitKey.trim()
            if (value.isEmpty() && unit.isEmpty()) return true
            if (value.isEmpty() || unit.isEmpty()) return false
            return value.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true
        }
}

@Composable
internal fun DailyCheckInTodayScreen(
    appState: AppUiState,
    checkInState: DailyCheckInUiState,
    onTabSelected: (AppTab) -> Unit,
    onSave: (DailyCheckInSubmission) -> Unit,
    modifier: Modifier = Modifier,
) {
    val eligibleTracks = appState.recoveryTracks.filter {
        it.status == RecoveryTrackStatus.ACTIVE || it.status == RecoveryTrackStatus.MAINTENANCE
    }
    val editorKey = buildString {
        append(checkInState.localDateEpochDay)
        append(':')
        append(checkInState.updatedAtEpochMillis)
        eligibleTracks.forEach { track ->
            append(':')
            append(track.id)
            append(':')
            append(track.status.name)
        }
    }
    var mood by remember(editorKey) { mutableStateOf(checkInState.mood) }
    var stress by remember(editorKey) { mutableStateOf(checkInState.stress) }
    var energy by remember(editorKey) { mutableStateOf(checkInState.energy) }
    var sleepQuality by remember(editorKey) { mutableStateOf(checkInState.sleepQuality) }
    val editors = remember(editorKey) {
        mutableStateMapOf<String, DailyTrackEditor>().apply {
            eligibleTracks.forEach { track ->
                val existing = checkInState.entries[track.id]
                put(
                    track.id,
                    DailyTrackEditor(
                        outcome = existing?.outcome,
                        measuredValue = existing?.measuredValue?.toString().orEmpty(),
                        unitKey = existing?.unitKey.orEmpty(),
                        peakUrge = existing?.peakUrge,
                        privateNote = existing?.privateNote.orEmpty(),
                    ),
                )
            }
        }
    }
    val canSave = eligibleTracks.isNotEmpty() &&
        eligibleTracks.all { track ->
            val editor = editors[track.id]
            editor?.outcome != null && editor.measurementValid
        } &&
        !checkInState.isSaving

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == AppTab.TODAY,
                        onClick = { onTabSelected(tab) },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("daily_tab_${tab.name.lowercase()}"),
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                checkInState.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading today’s private check-in…")
                }

                eligibleTracks.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No active tracks to check in", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Resume or create a Recovery Track before completing a daily check-in.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("daily_check_in_screen"),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text("Today", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            LocalDate.ofEpochDay(checkInState.localDateEpochDay)
                                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (checkInState.checkInId == null) {
                                "Check in once across every active recovery journey."
                            } else {
                                "Your saved check-in is ready to review or update."
                            },
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    item {
                        DailySectionCard("How are you doing overall?") {
                            Text(
                                "These four answers are shared context. They do not combine or score your addictions.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ContextScale("Mood", mood) { mood = it }
                            ContextScale("Stress", stress) { stress = it }
                            ContextScale("Energy", energy) { energy = it }
                            ContextScale("Sleep quality", sleepQuality) { sleepQuality = it }
                        }
                    }

                    items(eligibleTracks, key = { it.id }) { track ->
                        val editor = checkNotNull(editors[track.id])
                        DailySectionCard(track.title) {
                            Text(
                                if (track.isPrimary) "Primary Recovery Track" else "Supporting Recovery Track",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("How did this track go today?")
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TrackCheckInOutcome.entries.forEach { outcome ->
                                    FilterChip(
                                        selected = editor.outcome == outcome,
                                        onClick = {
                                            editors[track.id] = editor.copy(outcome = outcome)
                                        },
                                        label = { Text(outcome.label()) },
                                        modifier = Modifier.testTag("outcome_${track.id}_${outcome.name}"),
                                    )
                                }
                            }

                            Text("Peak urge (optional)")
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                (1..5).forEach { urge ->
                                    FilterChip(
                                        selected = editor.peakUrge == urge,
                                        onClick = {
                                            editors[track.id] = editor.copy(peakUrge = urge)
                                        },
                                        label = { Text(urge.toString()) },
                                    )
                                }
                                if (editor.peakUrge != null) {
                                    TextButton(onClick = {
                                        editors[track.id] = editor.copy(peakUrge = null)
                                    }) { Text("Clear") }
                                }
                            }

                            Text("Measured result (optional)")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedTextField(
                                    value = editor.measuredValue,
                                    onValueChange = { value ->
                                        editors[track.id] = editor.copy(measuredValue = value.take(20))
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Value") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = !editor.measurementValid,
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = editor.unitKey,
                                    onValueChange = { value ->
                                        editors[track.id] = editor.copy(unitKey = value.take(30))
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Unit") },
                                    isError = !editor.measurementValid,
                                    singleLine = true,
                                )
                            }
                            if (!editor.measurementValid) {
                                Text(
                                    "Enter both a non-negative value and its unit, or leave both blank.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            OutlinedTextField(
                                value = editor.privateNote,
                                onValueChange = { value ->
                                    editors[track.id] = editor.copy(privateNote = value.take(2_000))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Private note (optional)") },
                                minLines = 2,
                                supportingText = { Text("Stored only on this device in this phase.") },
                            )

                            if (track.program.safety.tier == SafetyTier.MEDICALLY_HIGH_RISK) {
                                Text(
                                    "DeAddict does not provide detox or taper instructions. Use professional guidance for major changes.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    checkInState.feedback?.let { feedback ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    feedback,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                onSave(
                                    DailyCheckInSubmission(
                                        mood = mood,
                                        stress = stress,
                                        energy = energy,
                                        sleepQuality = sleepQuality,
                                        entries = eligibleTracks.map { track ->
                                            val current = checkNotNull(editors[track.id])
                                            DailyTrackCheckInSubmission(
                                                recoveryTrackId = track.id,
                                                outcome = checkNotNull(current.outcome),
                                                measuredValue = current.measuredValue.trim()
                                                    .takeIf(String::isNotEmpty)
                                                    ?.toDouble(),
                                                unitKey = current.unitKey.trim().takeIf(String::isNotEmpty),
                                                peakUrge = current.peakUrge,
                                                privateNote = current.privateNote,
                                            )
                                        },
                                    ),
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_daily_check_in"),
                            enabled = canSave,
                        ) {
                            Text(
                                when {
                                    checkInState.isSaving -> "Saving…"
                                    checkInState.checkInId == null -> "Save today’s check-in"
                                    else -> "Update today’s check-in"
                                },
                            )
                        }
                        if (!canSave && !checkInState.isSaving) {
                            Text(
                                "Choose one outcome for every active track.",
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun ContextScale(
    label: String,
    selected: Int?,
    onSelected: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Medium)
            if (selected != null) {
                TextButton(onClick = { onSelected(null) }) { Text("Clear") }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..5).forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(value.toString()) },
                    modifier = Modifier.testTag("${label.lowercase().replace(' ', '_')}_$value"),
                )
            }
        }
    }
}

private fun TrackCheckInOutcome.label(): String = when (this) {
    TrackCheckInOutcome.GOAL_MET -> "Goal met"
    TrackCheckInOutcome.GOAL_PARTLY_MET -> "Partly met"
    TrackCheckInOutcome.GOAL_NOT_MET -> "Not met"
    TrackCheckInOutcome.SLIP -> "Slip"
    TrackCheckInOutcome.AWARENESS_LOGGED -> "Logged"
}
