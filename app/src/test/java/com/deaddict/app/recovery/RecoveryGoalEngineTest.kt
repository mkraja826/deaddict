package com.deaddict.app.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryGoalEngineTest {
    private fun goal(
        kind: GoalTargetKind,
        target: Double = 7.0,
    ) = RecoveryGoal(
        programId = "program-1",
        targetKind = kind,
        targetValue = target,
        period = GoalPeriod.WEEKLY,
        startsAtEpochMillis = 1L,
        title = "Test goal",
    )

    @Test
    fun `progress goals complete when observed value reaches target`() {
        val progress = RecoveryGoalEngine.progress(
            goal(GoalTargetKind.ABSTINENT_DAYS),
            observedValue = 7.0,
        )

        assertTrue(progress.completed)
        assertEquals(1.0, progress.fraction, 0.0)
    }

    @Test
    fun `limit goals complete while observed value stays at or below target`() {
        val progress = RecoveryGoalEngine.progress(
            goal(GoalTargetKind.MAX_MINUTES, target = 60.0),
            observedValue = 45.0,
        )

        assertTrue(progress.completed)
        assertEquals(1.0, progress.fraction, 0.0)
    }

    @Test
    fun `limit goals report proportional progress when exceeded`() {
        val progress = RecoveryGoalEngine.progress(
            goal(GoalTargetKind.MAX_QUANTITY, target = 4.0),
            observedValue = 8.0,
        )

        assertFalse(progress.completed)
        assertEquals(0.5, progress.fraction, 0.0)
    }

    @Test
    fun `negative observations are clamped to zero`() {
        val progress = RecoveryGoalEngine.progress(
            goal(GoalTargetKind.CHECK_INS, target = 5.0),
            observedValue = -3.0,
        )

        assertEquals(0.0, progress.currentValue, 0.0)
        assertEquals(0.0, progress.fraction, 0.0)
    }

    @Test
    fun `milestones include achieved values and expose next target`() {
        assertEquals(listOf(1, 3, 7, 14), RecoveryGoalEngine.achievedMilestones(14).map { it.days })
        assertEquals(30, RecoveryGoalEngine.nextMilestone(14)?.days)
        assertEquals(16, RecoveryGoalEngine.daysRemaining(14))
    }

    @Test
    fun `one year has no later built in milestone`() {
        assertNull(RecoveryGoalEngine.nextMilestone(365))
        assertNull(RecoveryGoalEngine.daysRemaining(365))
    }

    @Test
    fun `normalized score averages and clamps progress`() {
        val score = RecoveryGoalEngine.normalizedScore(
            listOf(
                GoalProgress(5.0, 10.0, lowerIsBetter = false),
                GoalProgress(2.0, 2.0, lowerIsBetter = false),
            ),
        )

        assertEquals(75, score)
        assertEquals(0, RecoveryGoalEngine.normalizedScore(emptyList()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `goal rejects non positive targets`() {
        goal(GoalTargetKind.CHECK_INS, target = 0.0)
    }
}
