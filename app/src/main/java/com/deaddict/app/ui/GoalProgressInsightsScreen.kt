package com.deaddict.app.ui

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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deaddict.app.insights.GoalProgressMode
import com.deaddict.app.insights.GoalProgressSegment
import com.deaddict.app.insights.GoalProgressTrend
import com.deaddict.model.RecoveryGoalType

@Composable
internal fun GoalProgressInsightsScreen(
    appState: AppUiState,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTrack = appState.selectedRecoveryTrack
    val insights = appState.insights
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == AppTab.INSIGHTS,
                        onClick = { onTabSelected(tab) },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("insights_tab_${tab.name.lowercase()}"),
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("goal_progress_insights_screen"),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Insights", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Only ${selectedTrack?.title ?: "the selected Recovery Track"} is included.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Goals are evaluated separately when they change. No cross-addiction score is created.",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                if (insights == null) {
                    item {
                        ProgressSectionCard("Not enough data") {
                            Text("Complete daily check-ins for this track to begin seeing goal-aware progress.")
                        }
                    }
                } else {
                    val progress = insights.goalProgress
                    val current = progress?.currentGoal
                    if (current == null) {
                        item {
                            ProgressSectionCard("Current goal progress") {
                                Text("No current goal progress is available yet.")
                            }
                        }
                    } else {
                        item { CurrentGoalProgressCard(current) }
                    }

                    if (!progress?.previousGoals.isNullOrEmpty()) {
                        item {
                            ProgressSectionCard("Earlier goals in this window") {
                                Text(
                                    "These results stay separate because the goal definition changed.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                progress.previousGoals.forEach { previous ->
                                    HistoricalGoalRow(previous)
                                }
                            }
                        }
                    }

                    item {
                        ProgressSectionCard("Behavior and Rescue summary") {
                            ProgressMetric("Tracking entries", insights.checkInCount.toString())
                            ProgressMetric("Slips recorded", insights.slipCount.toString())
                            ProgressMetric(
                                "Average urge intensity",
                                insights.averageUrge?.let { "%.1f / 5".format(it) } ?: "Not enough data",
                            )
                            ProgressMetric("Rescue sessions", insights.rescueCount.toString())
                            insights.topTrigger?.let { ProgressMetric("Most recorded trigger", it) }
                            insights.peakRiskPeriod?.let { ProgressMetric("Most active risk period", it) }
                            Text(insights.explanation)
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun CurrentGoalProgressCard(progress: GoalProgressSegment) {
    ProgressSectionCard("Current goal progress") {
        Text(
            progress.title ?: progress.goalType.goalLabel(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        progress.targetValue?.let { target ->
            ProgressMetric(
                "Target",
                buildString {
                    append(formatNumber(target))
                    progress.unitKey?.let { append(" $it") }
                    progress.periodType?.let { append(" / ${it.name.lowercase()}") }
                },
            )
        }
        when (progress.mode) {
            GoalProgressMode.AWARENESS -> {
                ProgressMetric(
                    "Logging consistency",
                    progress.consistencyPercent?.let { "$it%" } ?: "Not enough data",
                )
                ProgressMetric("Confirmed days", progress.confirmedDays.toString())
                ProgressMetric("Latest logging run", "${progress.latestRunDays} day(s)")
                ProgressMetric("Best logging run", "${progress.bestRunDays} day(s)")
            }
            GoalProgressMode.ADHERENCE -> {
                ProgressMetric(
                    "Goal adherence",
                    progress.adherencePercent?.let { "$it%" } ?: "Not enough data",
                )
                progress.eligibleDays?.let { eligible ->
                    ProgressMetric("Confirmed days", "${progress.confirmedDays} of $eligible")
                } ?: ProgressMetric("Confirmed days", progress.confirmedDays.toString())
                ProgressMetric("Goal met", progress.goalMetDays.toString())
                ProgressMetric("Partly met", progress.partlyMetDays.toString())
                ProgressMetric("Not met", progress.goalNotMetDays.toString())
                ProgressMetric("Slip days", progress.slipDays.toString())
                ProgressMetric("Latest met run", "${progress.latestRunDays} day(s)")
                ProgressMetric("Best met run", "${progress.bestRunDays} day(s)")
                ProgressMetric("Outcome trend", progress.trend.label())
            }
            GoalProgressMode.UNSCOPED -> Text(progress.explanation)
        }
        progress.averageMeasuredValue?.let { average ->
            ProgressMetric(
                "Average measured result",
                buildString {
                    append(formatNumber(average))
                    progress.measurementUnit?.let { append(" $it") }
                    append(" across ${progress.measuredDays} day(s)")
                },
            )
        }
        progress.averagePeakUrge?.let { average ->
            ProgressMetric("Average peak urge", "%.1f / 5".format(average))
        }
        Text(progress.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoricalGoalRow(progress: GoalProgressSegment) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            progress.title ?: progress.goalType.goalLabel(),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            when (progress.mode) {
                GoalProgressMode.AWARENESS ->
                    "${progress.confirmedDays} confirmed awareness day(s)"
                GoalProgressMode.ADHERENCE ->
                    "${progress.confirmedDays} confirmed day(s) · " +
                        (progress.adherencePercent?.let { "$it% adherence" } ?: "not enough scored outcomes")
                GoalProgressMode.UNSCOPED -> "Historical result kept, but not scored"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun ProgressMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun RecoveryGoalType?.goalLabel(): String = when (this) {
    RecoveryGoalType.QUIT_COMPLETELY -> "Quit completely"
    RecoveryGoalType.REDUCE_QUANTITY -> "Reduce quantity"
    RecoveryGoalType.DAILY_LIMIT -> "Daily limit"
    RecoveryGoalType.WEEKLY_LIMIT -> "Weekly limit"
    RecoveryGoalType.TIME_LIMIT -> "Time limit"
    RecoveryGoalType.SPENDING_LIMIT -> "Spending limit"
    RecoveryGoalType.DELAY_FIRST_USE -> "Delay first use"
    RecoveryGoalType.NO_USE_PERIOD -> "No-use period"
    RecoveryGoalType.AWARENESS_ONLY -> "Awareness only"
    RecoveryGoalType.CUSTOM -> "Custom goal"
    null -> "Earlier goal"
}

private fun GoalProgressTrend.label(): String = when (this) {
    GoalProgressTrend.IMPROVING -> "Improving"
    GoalProgressTrend.STEADY -> "Steady"
    GoalProgressTrend.DECLINING -> "Declining"
    GoalProgressTrend.NOT_ENOUGH_DATA -> "Not enough data"
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
