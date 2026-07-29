package com.deaddict.app.rescue

import com.deaddict.programs.DefaultProgramRegistry
import com.deaddict.programs.ProgramId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RescueFlowTest {
    private val flow = RescueFlow()
    private val program = DefaultProgramRegistry().find(ProgramId.of("gaming"))!!

    @Test
    fun `pause cannot be skipped`() {
        val started = flow.begin(TRACK_ID, program, STARTED_AT)

        val result = runCatching { flow.continueAfterPause(started) }

        assertTrue(result.isFailure)
    }

    @Test
    fun `complete flow preserves recovery track ownership and learning inputs`() {
        var state = flow.begin(TRACK_ID, program, STARTED_AT)
        repeat(60) { state = flow.tick(state) }
        state = flow.continueAfterPause(state)
        state = flow.acknowledgeMotivation(state)
        state = flow.setInitialUrge(state, 5)
        state = flow.continueToTrigger(state)
        state = flow.chooseTrigger(state, "stress")
        val shownActions = state.replacementActions
        state = flow.chooseReplacement(state, shownActions.first())
        state = flow.setFinalUrge(state, 3)
        state = flow.complete(state)

        assertEquals(RescueStep.COMPLETE, state.step)
        assertEquals(TRACK_ID, state.recoveryTrackId)
        assertEquals(STARTED_AT, state.startedAtEpochMillis)
        assertEquals(program.id, state.program?.id)
        assertEquals(3, shownActions.size)
        assertEquals("stress", state.triggerKey)
        assertEquals(2, state.initialUrge - state.finalUrge)
    }

    @Test
    fun `invalid ownership cannot begin`() {
        assertTrue(runCatching { flow.begin("", program, STARTED_AT) }.isFailure)
        assertTrue(runCatching { flow.begin(TRACK_ID, program, 0L) }.isFailure)
    }

    @Test
    fun `invalid intensity is rejected`() {
        var state = flow.begin(TRACK_ID, program, STARTED_AT)
        repeat(60) { state = flow.tick(state) }
        state = flow.continueAfterPause(state)
        state = flow.acknowledgeMotivation(state)

        assertTrue(runCatching { flow.setInitialUrge(state, 6) }.isFailure)
    }

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000101"
        const val STARTED_AT = 1_000L
    }
}
