package com.deaddict.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.deaddict.app.insights.CrossTrackInsightSummary
import com.deaddict.app.insights.CrossTrackPairInsight
import com.deaddict.app.insights.CrossTrackPattern
import com.deaddict.app.insights.GoalProgressMode
import com.deaddict.app.insights.GoalProgressSegment
import com.deaddict.app.insights.GoalProgressSummary
import com.deaddict.app.insights.GoalProgressTrend
import com.deaddict.app.insights.InsightWindow
import com.deaddict.app.insights.ReplacementActionInsight
import com.deaddict.app.insights.SevenDayInsights
import com.deaddict.app.insights.TrendDirection
import com.deaddict.app.ui.theme.DeAddictTheme
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.DefaultProgramRegistry
import com.deaddict.programs.ProgramId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoalProgressInsightsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sectionsAndControlsRemainIndependent() {
        var selectedWindow: InsightWindow? = null
        var hiddenTrackId: String? = null

        compose.setContent {
            DeAddictTheme {
                GoalProgressInsightsScreen(
                    appState = appState(),
                    insightsState = InsightsControlsUiState(
                        isLoading = false,
                        selectedRecoveryTrackId = TRACK_ID,
                        window = InsightWindow.SEVEN_DAYS,
                        insights = insights(),
                    ),
                    onTabSelected = {},
                    onWindowSelected = { selectedWindow = it },
                    onHideComparison = { hiddenTrackId = it },
                    onRestoreComparisons = {},
                )
            }
        }

        compose.onNodeWithTag("insight_window_30").performClick()
        compose.onNodeWithText("Goal adherence").assertIsDisplayed()
        compose.onNodeWithText("75%").assertIsDisplayed()
        compose.onNodeWithText("Earlier goals in this window")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Across your Recovery Tracks")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Coffee")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("hide_cross_track_$OTHER_TRACK_ID")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Replacement actions")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Slow Breathing")
            .performScrollTo()
            .assertIsDisplayed()

        compose.runOnIdle {
            assertEquals(InsightWindow.THIRTY_DAYS, selectedWindow)
            assertEquals(OTHER_TRACK_ID, hiddenTrackId)
        }
    }

    @Test
    fun hiddenComparisonCanBeRestoredWithoutRemovingOtherInsights() {
        var restored = false
        compose.setContent {
            DeAddictTheme {
                GoalProgressInsightsScreen(
                    appState = appState(),
                    insightsState = InsightsControlsUiState(
                        isLoading = false,
                        selectedRecoveryTrackId = TRACK_ID,
                        window = InsightWindow.NINETY_DAYS,
                        hiddenOtherTrackIds = setOf(OTHER_TRACK_ID),
                        insights = insights(),
                    ),
                    onTabSelected = {},
                    onWindowSelected = {},
                    onHideComparison = {},
                    onRestoreComparisons = { restored = true },
                )
            }
        }

        compose.onNodeWithText("Coffee").assertDoesNotExist()
        compose.onNodeWithText("All cross-track comparisons for this Recovery Track are hidden.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("restore_cross_track_comparisons")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Replacement actions")
            .performScrollTo()
            .assertIsDisplayed()

        compose.runOnIdle { assertTrue(restored) }
    }

    private fun appState(): AppUiState {
        val registry = DefaultProgramRegistry()
        val gaming = checkNotNull(registry.find(ProgramId.of("gaming")))
        val caffeine = checkNotNull(registry.find(ProgramId.of("caffeine")))
        return AppUiState(
            isLoading = false,
            ownerKey = "guest:test-profile",
            selectedTab = AppTab.INSIGHTS,
            availablePrograms = listOf(gaming, caffeine),
            recoveryTracks = listOf(
                RecoveryTrackUi(
                    id = TRACK_ID,
                    program = gaming,
                    displayAlias = null,
                    role = RecoveryTrackRole.PRIMARY,
                    status = RecoveryTrackStatus.ACTIVE,
                ),
                RecoveryTrackUi(
                    id = OTHER_TRACK_ID,
                    program = caffeine,
                    displayAlias = "Coffee",
                    role = RecoveryTrackRole.SUPPORTING,
                    status = RecoveryTrackStatus.ACTIVE,
                ),
            ),
            selectedRecoveryTrackId = TRACK_ID,
        )
    }

    private fun insights(): SevenDayInsights {
        val current = progressSegment(
            id = CURRENT_GOAL_ID,
            type = RecoveryGoalType.TIME_LIMIT,
            current = true,
            adherence = 75,
            confirmed = 4,
        )
        val previous = progressSegment(
            id = PREVIOUS_GOAL_ID,
            type = RecoveryGoalType.AWARENESS_ONLY,
            current = false,
            adherence = null,
            confirmed = 2,
            mode = GoalProgressMode.AWARENESS,
        )
        return SevenDayInsights(
            checkInCount = 2,
            slipCount = 1,
            averageUrge = 3.0,
            topTrigger = "stress",
            peakRiskPeriod = "evening",
            trend = TrendDirection.STEADY,
            rescueCount = 2,
            rescuesWithReducedUrge = 1,
            explanation = "Selected-track behavioral summary.",
            goalProgress = GoalProgressSummary(
                currentGoal = current,
                previousGoals = listOf(previous),
                totalConfirmedDays = 6,
                goalChangesInWindow = 1,
                window = InsightWindow.SEVEN_DAYS,
            ),
            crossTrackInsights = CrossTrackInsightSummary(
                pairings = listOf(
                    CrossTrackPairInsight(
                        otherTrackId = OTHER_TRACK_ID,
                        pairedDays = 5,
                        comparableDays = 5,
                        bothMetDays = 1,
                        bothDifficultDays = 1,
                        selectedMetOtherDifficultDays = 3,
                        selectedDifficultOtherMetDays = 0,
                        pattern = CrossTrackPattern.POSSIBLE_SHIFT_TOWARD_OTHER,
                        dominantPatternPercent = 60,
                    ),
                ),
                replacementActions = listOf(
                    ReplacementActionInsight(
                        actionKey = "slow_breathing",
                        attempts = 2,
                        reducedUrgeCount = 2,
                        reducedUrgePercent = 100,
                        averageUrgeDrop = 1.5,
                    ),
                ),
                sharedDifficultDays = 1,
                possibleShiftDays = 3,
                averageStressOnSharedDifficultDays = 4.5,
                averageSleepOnSharedDifficultDays = 2.0,
                explanation = "Associations are not proof of causation.",
            ),
        )
    }

    private fun progressSegment(
        id: String,
        type: RecoveryGoalType,
        current: Boolean,
        adherence: Int?,
        confirmed: Int,
        mode: GoalProgressMode = GoalProgressMode.ADHERENCE,
    ) = GoalProgressSegment(
        goalVersionId = id,
        goalType = type,
        title = null,
        targetValue = if (type == RecoveryGoalType.TIME_LIMIT) 30.0 else null,
        unitKey = if (type == RecoveryGoalType.TIME_LIMIT) "minutes" else null,
        periodType = if (type == RecoveryGoalType.TIME_LIMIT) GoalPeriodType.DAY else null,
        isCurrent = current,
        mode = mode,
        eligibleDays = if (current) 7 else null,
        confirmedDays = confirmed,
        goalMetDays = 3,
        partlyMetDays = 1,
        goalNotMetDays = 0,
        slipDays = 0,
        awarenessDays = if (mode == GoalProgressMode.AWARENESS) confirmed else 0,
        adherencePercent = adherence,
        consistencyPercent = if (mode == GoalProgressMode.AWARENESS) 29 else 57,
        latestRunDays = 2,
        bestRunDays = 3,
        averagePeakUrge = 3.0,
        measuredDays = 2,
        averageMeasuredValue = if (type == RecoveryGoalType.TIME_LIMIT) 25.0 else null,
        measurementUnit = if (type == RecoveryGoalType.TIME_LIMIT) "minutes" else null,
        trend = GoalProgressTrend.STEADY,
        explanation = "Goal-version explanation.",
    )

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000401"
        const val OTHER_TRACK_ID = "00000000-0000-0000-0000-000000000404"
        const val CURRENT_GOAL_ID = "00000000-0000-0000-0000-000000000402"
        const val PREVIOUS_GOAL_ID = "00000000-0000-0000-0000-000000000403"
    }
}
