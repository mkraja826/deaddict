package com.deaddict.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.deaddict.app.insights.GoalProgressMode
import com.deaddict.app.insights.GoalProgressSegment
import com.deaddict.app.insights.GoalProgressSummary
import com.deaddict.app.insights.GoalProgressTrend
import com.deaddict.app.insights.InsightWindow
import com.deaddict.app.insights.SevenDayInsights
import com.deaddict.app.insights.TrendDirection
import com.deaddict.app.ui.theme.DeAddictTheme
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.DefaultProgramRegistry
import com.deaddict.programs.ProgramId
import org.junit.Rule
import org.junit.Test

class GoalProgressInsightsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun currentAndHistoricalGoalsAreRenderedSeparately() {
        val gaming = checkNotNull(DefaultProgramRegistry().find(ProgramId.of("gaming")))
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
        val state = AppUiState(
            isLoading = false,
            ownerKey = "guest:test-profile",
            selectedTab = AppTab.INSIGHTS,
            availablePrograms = listOf(gaming),
            recoveryTracks = listOf(
                RecoveryTrackUi(
                    id = TRACK_ID,
                    program = gaming,
                    displayAlias = null,
                    role = RecoveryTrackRole.PRIMARY,
                    status = RecoveryTrackStatus.ACTIVE,
                ),
            ),
            selectedRecoveryTrackId = TRACK_ID,
            insights = SevenDayInsights(
                checkInCount = 2,
                slipCount = 1,
                averageUrge = 3.0,
                topTrigger = "stress",
                peakRiskPeriod = "evening",
                trend = TrendDirection.STEADY,
                rescueCount = 1,
                rescuesWithReducedUrge = 1,
                explanation = "Selected-track behavioral summary.",
                goalProgress = GoalProgressSummary(
                    currentGoal = current,
                    previousGoals = listOf(previous),
                    totalConfirmedDays = 6,
                    goalChangesInWindow = 1,
                    window = InsightWindow.SEVEN_DAYS,
                ),
            ),
        )

        compose.setContent {
            DeAddictTheme {
                GoalProgressInsightsScreen(
                    appState = state,
                    onTabSelected = {},
                )
            }
        }

        compose.onNodeWithText("Goal adherence").assertIsDisplayed()
        compose.onNodeWithText("75%").assertIsDisplayed()
        compose.onNodeWithText("Earlier goals in this window")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Awareness only")
            .performScrollTo()
            .assertIsDisplayed()
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
        const val CURRENT_GOAL_ID = "00000000-0000-0000-0000-000000000402"
        const val PREVIOUS_GOAL_ID = "00000000-0000-0000-0000-000000000403"
    }
}
