package com.deaddict.app.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RookMessageEngineTest {
    @Test
    fun `quiet mode produces no message`() {
        val message = RookMessageEngine.message(
            context(requestedTone = RookTone.QUIET),
        )

        assertNull(message)
    }

    @Test
    fun `medically high risk track disables brutal banter`() {
        val message = RookMessageEngine.message(
            context(
                requestedTone = RookTone.BRUTAL_BANTER,
                medicallyHighRisk = true,
            ),
        )

        assertEquals(RookTone.DIRECT, message?.effectiveTone)
        assertTrue(message?.text?.contains("safe decision") == true)
    }

    @Test
    fun `multi track slip preserves other recovery tracks`() {
        val message = RookMessageEngine.message(
            context(
                requestedTone = RookTone.DIRECT,
                activeTrackCount = 3,
                slipRecorded = true,
            ),
        )

        assertTrue(message?.text?.contains("other recovery tracks are unchanged") == true)
    }

    @Test
    fun `brutal message challenges behavior without identity attack`() {
        val message = RookMessageEngine.message(
            context(requestedTone = RookTone.BRUTAL_BANTER),
        )

        val text = message?.text.orEmpty().lowercase()
        assertTrue(text.isNotBlank())
        assertTrue("loser" !in text)
        assertTrue("pathetic" !in text)
        assertTrue("worthless" !in text)
        assertTrue("disgusting" !in text)
    }

    private fun context(
        requestedTone: RookTone,
        medicallyHighRisk: Boolean = false,
        activeTrackCount: Int = 2,
        slipRecorded: Boolean = false,
    ) = RookContext(
        moment = LegacyRookMoment.RESCUE,
        programName = "Smoking",
        activeTrackCount = activeTrackCount,
        requestedTone = requestedTone,
        medicallyHighRisk = medicallyHighRisk,
        slipRecorded = slipRecorded,
    )
}
