package com.deaddict.app.onboarding

import com.deaddict.app.coach.RookTone
import com.deaddict.model.RecoveryGoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {
    private val coordinator = OnboardingCoordinator()

    @Test
    fun `privacy step cannot continue before explicit acceptance`() {
        val draft = OnboardingDraft(step = OnboardingStep.PRIVACY)

        assertFalse(coordinator.canContinue(draft))
        assertTrue(runCatching { coordinator.next(draft) }.isFailure)
    }

    @Test
    fun `numeric goals require a positive target and unit`() {
        val incomplete = OnboardingDraft(
            step = OnboardingStep.GOAL_DETAILS,
            goalType = RecoveryGoalType.DAILY_LIMIT,
        )
        val complete = incomplete.copy(goalTarget = 60.0, goalUnit = "minutes")

        assertFalse(coordinator.canContinue(incomplete))
        assertTrue(coordinator.canContinue(complete))
    }

    @Test
    fun `awareness goal does not invent a numeric target`() {
        val draft = OnboardingDraft(
            step = OnboardingStep.GOAL_DETAILS,
            goalType = RecoveryGoalType.AWARENESS_ONLY,
        )

        assertTrue(coordinator.canContinue(draft))
    }

    @Test
    fun `back preserves draft answers`() {
        val draft = OnboardingDraft(
            step = OnboardingStep.ROOK,
            privacyAccepted = true,
            primaryProgramId = "gaming",
            goalType = RecoveryGoalType.TIME_LIMIT,
            goalTarget = 60.0,
            goalUnit = "minutes",
            rookTone = RookTone.QUIET,
        )

        val previous = coordinator.previous(draft)

        assertEquals(OnboardingStep.SUPPORTING_TRACKS, previous.step)
        assertEquals("gaming", previous.primaryProgramId)
        assertEquals(60.0, previous.goalTarget)
        assertEquals(RookTone.QUIET, previous.rookTone)
    }

    @Test
    fun `summary requires privacy program and goal`() {
        val incomplete = OnboardingDraft(step = OnboardingStep.SUMMARY)
        val complete = incomplete.copy(
            privacyAccepted = true,
            primaryProgramId = "gaming",
            goalType = RecoveryGoalType.AWARENESS_ONLY,
        )

        assertFalse(coordinator.canContinue(incomplete))
        assertTrue(coordinator.canContinue(complete))
        assertEquals(OnboardingStep.COMPLETE, coordinator.next(complete).step)
    }
}
