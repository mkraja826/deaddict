package com.deaddict.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DailyCheckInTest {
    @Test
    fun awarenessGoalProducesObservedOutcome() {
        val outcome = TrackCheckInOutcomeResolver.resolve(
            goal = goal(RecoveryGoalType.AWARENESS_ONLY),
            answer = TrackCheckInAnswer(),
        )

        assertEquals(TrackCheckInOutcome.OBSERVED, outcome)
    }

    @Test
    fun explicitSlipOverridesGoalCalculation() {
        val outcome = TrackCheckInOutcomeResolver.resolve(
            goal = goal(RecoveryGoalType.DAILY_LIMIT, target = 5.0),
            answer = TrackCheckInAnswer(slipped = true, quantity = 2.0),
        )

        assertEquals(TrackCheckInOutcome.SLIP, outcome)
    }

    @Test
    fun managedUrgeOverridesAlignedLimit() {
        val outcome = TrackCheckInOutcomeResolver.resolve(
            goal = goal(RecoveryGoalType.DAILY_LIMIT, target = 5.0),
            answer = TrackCheckInAnswer(urgeManaged = true, quantity = 2.0),
        )

        assertEquals(TrackCheckInOutcome.URGE_MANAGED, outcome)
    }

    @Test
    fun missingMeasurementIsObservedRatherThanFailed() {
        val outcome = TrackCheckInOutcomeResolver.resolve(
            goal = goal(RecoveryGoalType.TIME_LIMIT, target = 60.0),
            answer = TrackCheckInAnswer(),
        )

        assertEquals(TrackCheckInOutcome.OBSERVED, outcome)
    }

    @Test
    fun dailyLimitUsesInclusiveTarget() {
        val goal = goal(RecoveryGoalType.DAILY_LIMIT, target = 5.0)

        assertEquals(
            TrackCheckInOutcome.ALIGNED,
            TrackCheckInOutcomeResolver.resolve(goal, TrackCheckInAnswer(quantity = 5.0)),
        )
        assertEquals(
            TrackCheckInOutcome.LIMIT_EXCEEDED,
            TrackCheckInOutcomeResolver.resolve(goal, TrackCheckInAnswer(quantity = 6.0)),
        )
    }

    @Test
    fun delayGoalRequiresMeetingMinimumDelay() {
        val goal = goal(RecoveryGoalType.DELAY_FIRST_USE, target = 120.0)

        assertEquals(
            TrackCheckInOutcome.ALIGNED,
            TrackCheckInOutcomeResolver.resolve(goal, TrackCheckInAnswer(durationMinutes = 120)),
        )
        assertEquals(
            TrackCheckInOutcome.LIMIT_EXCEEDED,
            TrackCheckInOutcomeResolver.resolve(goal, TrackCheckInAnswer(durationMinutes = 90)),
        )
    }

    @Test
    fun untrackedAnswerRemainsUnknown() {
        val outcome = TrackCheckInOutcomeResolver.resolve(
            goal = goal(RecoveryGoalType.QUIT_COMPLETELY),
            answer = TrackCheckInAnswer(explicitlyTracked = false),
        )

        assertEquals(TrackCheckInOutcome.NOT_TRACKED, outcome)
    }

    @Test
    fun dailyCheckInRejectsInvalidWellbeingScale() {
        assertThrows(IllegalArgumentException::class.java) {
            DailyCheckIn(
                id = DailyCheckInId.new(),
                ownerKey = OwnerKey.guest("test"),
                localDate = LocalDate.of(2026, 7, 30),
                timezoneId = "Asia/Kolkata",
                mood = 6,
                completedAtEpochMillis = 1,
            )
        }
    }

    @Test
    fun trackEntryRequiresCostCurrencyPair() {
        assertThrows(IllegalArgumentException::class.java) {
            TrackCheckInEntry(
                id = TrackCheckInEntryId.new(),
                dailyCheckInId = DailyCheckInId.new(),
                recoveryTrackId = RecoveryTrackId.random(),
                goalVersionId = RecoveryGoalVersionId.random(),
                outcome = TrackCheckInOutcome.OBSERVED,
                costMinorUnits = 500,
            )
        }
    }

    private fun goal(
        type: RecoveryGoalType,
        target: Double? = null,
    ): RecoveryGoalVersion {
        val now = Instant.parse("2026-07-30T18:00:00Z")
        return RecoveryGoalVersion(
            id = RecoveryGoalVersionId.random(),
            recoveryTrackId = RecoveryTrackId.random(),
            goalType = type,
            targetValue = target,
            unitKey = target?.let { "units" },
            periodType = target?.let { GoalPeriodType.DAY },
            title = null,
            effectiveFrom = now,
            effectiveUntil = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
