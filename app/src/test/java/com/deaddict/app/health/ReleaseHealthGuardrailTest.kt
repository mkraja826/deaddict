package com.deaddict.app.health

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseHealthGuardrailTest {
    @Test
    fun healthySampleCanProceed() {
        val snapshot = snapshot(
            successes = 40,
            failures = 0,
            slow = 2,
            crashes = 0,
        )

        val assessment = ReleaseHealthGuardrail.assess(snapshot)

        assertEquals(RolloutDecision.PROCEED, assessment.decision)
    }

    @Test
    fun elevatedFailureRateHoldsRollout() {
        val snapshot = snapshot(
            successes = 38,
            failures = 2,
            slow = 0,
            crashes = 0,
        )

        val assessment = ReleaseHealthGuardrail.assess(snapshot)

        assertEquals(RolloutDecision.HOLD, assessment.decision)
        assertTrue(assessment.reasons.any { it.contains("failure rate") })
    }

    @Test
    fun oneCrashRequiresWatchAndTwoRequireHold() {
        assertEquals(
            RolloutDecision.WATCH,
            ReleaseHealthGuardrail.assess(snapshot(10, 0, 0, 1)).decision,
        )
        assertEquals(
            RolloutDecision.HOLD,
            ReleaseHealthGuardrail.assess(snapshot(10, 0, 0, 2)).decision,
        )
    }

    @Test
    fun smallSamplesDoNotCreateRateClaims() {
        val assessment = ReleaseHealthGuardrail.assess(
            snapshot(successes = 3, failures = 1, slow = 2, crashes = 0),
        )

        assertEquals(RolloutDecision.PROCEED, assessment.decision)
    }

    @Test
    fun crashMarkerIsConsumedWithoutPersistingThrowableDetails() {
        val directory = Files.createTempDirectory("deaddict-crash-marker").toFile()
        val marker = File(directory, "previous_crash.marker")
        val store = CrashMarkerStore(marker)

        store.markCrash(timestampEpochMillis = 1234L)

        assertTrue(marker.exists())
        assertEquals("1234", marker.readText())
        assertTrue(store.consumeCrashMarker())
        assertFalse(marker.exists())
        assertFalse(store.consumeCrashMarker())
        directory.deleteRecursively()
    }

    private fun snapshot(
        successes: Long,
        failures: Long,
        slow: Long,
        crashes: Long,
    ): AppHealthSnapshot = AppHealthSnapshot(
        counts = mapOf(
            AppHealthMetric.DAILY_CHECK_IN_SAVE_SUCCESS to successes,
            AppHealthMetric.DAILY_CHECK_IN_SAVE_FAILURE to failures,
            AppHealthMetric.DAILY_CHECK_IN_SAVE_SLOW to slow,
            AppHealthMetric.PREVIOUS_CRASH to crashes,
        ),
    )
}
