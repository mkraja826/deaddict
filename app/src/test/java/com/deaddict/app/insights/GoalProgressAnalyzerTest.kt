package com.deaddict.app.insights

import com.deaddict.database.dao.TrackCheckInProgressRow
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.RecoveryGoalType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressAnalyzerTest {
    @Test
    fun `adherence uses confirmed outcomes and keeps slips at zero credit`() {
        val endDay = LocalDate.of(2026, 7, 31).toEpochDay()
        val summary = GoalProgressAnalyzer.analyze(
            rows = listOf(
                row(endDay - 2, CURRENT_GOAL_ID, TrackCheckInOutcome.GOAL_MET),
                row(endDay - 1, CURRENT_GOAL_ID, TrackCheckInOutcome.GOAL_PARTLY_MET),
                row(endDay, CURRENT_GOAL_ID, TrackCheckInOutcome.SLIP),
            ),
            currentGoal = goal(RecoveryGoalType.QUIT_COMPLETELY),
            window = InsightWindow.SEVEN_DAYS,
            nowMillis = NOW_MILLIS,
            zoneId = ZoneOffset.UTC,
        )!!

        val current = summary.currentGoal!!
        assertEquals(GoalProgressMode.ADHERENCE, current.mode)
        assertEquals(50, current.adherencePercent)
        assertEquals(43, current.consistencyPercent)
        assertEquals(1, current.goalMetDays)
        assertEquals(1, current.partlyMetDays)
        assertEquals(1, current.slipDays)
        assertEquals(0, current.latestRunDays)
        assertEquals(1, current.bestRunDays)
    }

    @Test
    fun `awareness goal reports consistency without a success score`() {
        val endDay = LocalDate.of(2026, 7, 31).toEpochDay()
        val summary = GoalProgressAnalyzer.analyze(
            rows = listOf(
                row(endDay - 2, CURRENT_GOAL_ID, TrackCheckInOutcome.AWARENESS_LOGGED),
                row(endDay - 1, CURRENT_GOAL_ID, TrackCheckInOutcome.AWARENESS_LOGGED),
                row(endDay, CURRENT_GOAL_ID, TrackCheckInOutcome.AWARENESS_LOGGED),
            ),
            currentGoal = goal(RecoveryGoalType.AWARENESS_ONLY),
            window = InsightWindow.SEVEN_DAYS,
            nowMillis = NOW_MILLIS,
            zoneId = ZoneOffset.UTC,
        )!!

        val current = summary.currentGoal!!
        assertEquals(GoalProgressMode.AWARENESS, current.mode)
        assertNull(current.adherencePercent)
        assertEquals(43, current.consistencyPercent)
        assertEquals(3, current.latestRunDays)
        assertEquals(3, current.bestRunDays)
        assertTrue(current.explanation.contains("not success or failure"))
    }

    @Test
    fun `goal changes are kept in separate progress segments`() {
        val endDay = LocalDate.of(2026, 7, 31).toEpochDay()
        val summary = GoalProgressAnalyzer.analyze(
            rows = listOf(
                row(
                    day = endDay - 4,
                    goalVersionId = PREVIOUS_GOAL_ID,
                    outcome = TrackCheckInOutcome.GOAL_MET,
                    goalType = RecoveryGoalType.REDUCE_QUANTITY,
                    targetValue = 3.0,
                    goalUnit = "units",
                ),
                row(endDay - 1, CURRENT_GOAL_ID, TrackCheckInOutcome.GOAL_MET),
                row(endDay, CURRENT_GOAL_ID, TrackCheckInOutcome.GOAL_NOT_MET),
            ),
            currentGoal = goal(RecoveryGoalType.QUIT_COMPLETELY),
            window = InsightWindow.SEVEN_DAYS,
            nowMillis = NOW_MILLIS,
            zoneId = ZoneOffset.UTC,
        )!!

        assertEquals(2, summary.currentGoal!!.confirmedDays)
        assertEquals(1, summary.previousGoals.single().confirmedDays)
        assertEquals(1, summary.goalChangesInWindow)
        assertEquals(3, summary.totalConfirmedDays)
        assertEquals(RecoveryGoalType.REDUCE_QUANTITY, summary.previousGoals.single().goalType)
    }

    @Test
    fun `measurements are averaged only when units match the goal`() {
        val endDay = LocalDate.of(2026, 7, 31).toEpochDay()
        val summary = GoalProgressAnalyzer.analyze(
            rows = listOf(
                row(
                    day = endDay - 2,
                    goalVersionId = CURRENT_GOAL_ID,
                    outcome = TrackCheckInOutcome.GOAL_MET,
                    measuredValue = 20.0,
                    recordedUnit = "minutes",
                ),
                row(
                    day = endDay - 1,
                    goalVersionId = CURRENT_GOAL_ID,
                    outcome = TrackCheckInOutcome.GOAL_MET,
                    measuredValue = 40.0,
                    recordedUnit = "MINUTES",
                ),
                row(
                    day = endDay,
                    goalVersionId = CURRENT_GOAL_ID,
                    outcome = TrackCheckInOutcome.GOAL_NOT_MET,
                    measuredValue = 2.0,
                    recordedUnit = "hours",
                ),
            ),
            currentGoal = goal(
                type = RecoveryGoalType.TIME_LIMIT,
                targetValue = 30.0,
                unitKey = "minutes",
                periodType = GoalPeriodType.DAY,
            ),
            window = InsightWindow.SEVEN_DAYS,
            nowMillis = NOW_MILLIS,
            zoneId = ZoneOffset.UTC,
        )!!

        val current = summary.currentGoal!!
        assertEquals(2, current.measuredDays)
        assertEquals(30.0, current.averageMeasuredValue!!, 0.001)
        assertEquals("minutes", current.measurementUnit)
    }

    private fun row(
        day: Long,
        goalVersionId: String,
        outcome: TrackCheckInOutcome,
        measuredValue: Double? = null,
        recordedUnit: String? = null,
        goalType: RecoveryGoalType = RecoveryGoalType.QUIT_COMPLETELY,
        targetValue: Double? = null,
        goalUnit: String? = null,
    ) = TrackCheckInProgressRow(
        localDateEpochDay = day,
        goalVersionId = goalVersionId,
        outcome = outcome,
        measuredValue = measuredValue,
        unitKey = recordedUnit,
        peakUrge = null,
        goalType = goalType,
        targetValue = targetValue,
        goalUnitKey = goalUnit,
        periodType = null,
        goalTitle = null,
    )

    private fun goal(
        type: RecoveryGoalType,
        targetValue: Double? = null,
        unitKey: String? = null,
        periodType: GoalPeriodType? = null,
    ) = RecoveryGoalVersionEntity(
        id = CURRENT_GOAL_ID,
        recoveryTrackId = TRACK_ID,
        goalType = type,
        targetValue = targetValue,
        unitKey = unitKey,
        periodType = periodType,
        title = null,
        effectiveFromEpochMillis = Instant.parse("2026-07-20T00:00:00Z").toEpochMilli(),
        effectiveUntilEpochMillis = null,
        createdAtEpochMillis = Instant.parse("2026-07-20T00:00:00Z").toEpochMilli(),
        updatedAtEpochMillis = Instant.parse("2026-07-20T00:00:00Z").toEpochMilli(),
        revision = 0,
        syncState = SyncState.SYNCED,
    )

    private companion object {
        const val CURRENT_GOAL_ID = "00000000-0000-0000-0000-000000000001"
        const val PREVIOUS_GOAL_ID = "00000000-0000-0000-0000-000000000002"
        const val TRACK_ID = "00000000-0000-0000-0000-000000000003"
        val NOW_MILLIS: Long = Instant.parse("2026-07-31T12:00:00Z").toEpochMilli()
    }
}
