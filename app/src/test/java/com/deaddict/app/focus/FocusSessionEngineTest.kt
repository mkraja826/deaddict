package com.deaddict.app.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionEngineTest {
    private val start = 1_000_000L

    @Test
    fun progress_isClampedBeforeStartAndAfterExpiry() {
        val session = session(durationMinutes = 10)

        val before = FocusSessionEngine.progress(session, start - 1)
        val after = FocusSessionEngine.progress(session, start + 20 * 60_000L)

        assertEquals(0L, before.elapsedMillis)
        assertEquals(600_000L, before.remainingMillis)
        assertFalse(before.expired)
        assertEquals(600_000L, after.elapsedMillis)
        assertEquals(0L, after.remainingMillis)
        assertEquals(1.0, after.fraction, 0.0)
        assertTrue(after.expired)
    }

    @Test
    fun completedSession_usesTerminalTimestampForProgress() {
        val completed = FocusSessionEngine.complete(session(durationMinutes = 10), start + 4 * 60_000L)

        val progress = FocusSessionEngine.progress(completed, start + 30 * 60_000L)

        assertEquals(FocusSessionState.COMPLETED, completed.state)
        assertEquals(240_000L, progress.elapsedMillis)
        assertEquals(360_000L, progress.remainingMillis)
    }

    @Test
    fun terminalTransitions_rejectAlreadyTerminalSessions() {
        val completed = FocusSessionEngine.complete(session(), start + 60_000L)

        assertThrows(IllegalArgumentException::class.java) {
            FocusSessionEngine.interrupt(completed, start + 120_000L)
        }
    }

    @Test
    fun stats_calculatesCompletionAndCurrentDayStreak() {
        val day = 86_400_000L
        val now = 10 * day
        val sessions = listOf(
            session(id = "a", durationMinutes = 10, startedAt = 8 * day).copy(completedAtEpochMillis = 8 * day + 1),
            session(id = "b", durationMinutes = 20, startedAt = 9 * day).copy(completedAtEpochMillis = 9 * day + 1),
            session(id = "c", durationMinutes = 30, startedAt = 10 * day).copy(completedAtEpochMillis = 10 * day + 1),
            session(id = "d", startedAt = 10 * day).copy(interruptedAtEpochMillis = 10 * day + 1),
        )

        val stats = FocusSessionEngine.stats(sessions, now)

        assertEquals(4, stats.started)
        assertEquals(3, stats.completed)
        assertEquals(1, stats.interrupted)
        assertEquals(0.75, stats.completionRate!!, 0.0)
        assertEquals(60, stats.completedMinutes)
        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun emptyStats_haveNoCompletionRate() {
        val stats = FocusSessionEngine.stats(emptyList(), start)

        assertEquals(0, stats.started)
        assertNull(stats.completionRate)
    }

    @Test
    fun model_rejectsConflictingTerminalStates() {
        assertThrows(IllegalArgumentException::class.java) {
            session().copy(
                completedAtEpochMillis = start + 1,
                interruptedAtEpochMillis = start + 2,
            )
        }
    }

    private fun session(
        id: String = "focus-1",
        durationMinutes: Int = 10,
        startedAt: Long = start,
    ) = FocusSession(
        id = id,
        programId = "program-1",
        durationMinutes = durationMinutes,
        startedAtEpochMillis = startedAt,
    )
}
