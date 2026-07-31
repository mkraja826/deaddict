package com.deaddict.app.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RookTrackToneCodecTest {
    @Test
    fun roundTripPreservesIndependentTrackTones() {
        val original = mapOf(
            TRACK_ONE to RookTone.BRUTAL_BANTER,
            TRACK_TWO to RookTone.QUIET,
        )

        assertEquals(original, RookTrackToneCodec.decode(RookTrackToneCodec.encode(original)))
    }

    @Test
    fun malformedValuesAreIgnored() {
        val decoded = RookTrackToneCodec.decode(
            setOf(
                "missing-separator",
                "$TRACK_ONE=NOT_A_TONE",
                "$TRACK_TWO=${RookTone.DIRECT.name}",
            ),
        )

        assertEquals(RookTone.DIRECT, decoded[TRACK_TWO])
        assertFalse(TRACK_ONE in decoded)
    }

    @Test
    fun trackOverrideWinsOverDefaultTone() {
        val preferences = RookPreferences(
            tone = RookTone.DIRECT,
            trackTones = mapOf(TRACK_ONE to RookTone.QUIET),
        )

        assertEquals(RookTone.QUIET, preferences.toneFor(TRACK_ONE))
        assertEquals(RookTone.DIRECT, preferences.toneFor(TRACK_TWO))
    }

    private companion object {
        const val TRACK_ONE = "00000000-0000-0000-0000-000000000101"
        const val TRACK_TWO = "00000000-0000-0000-0000-000000000102"
    }
}
