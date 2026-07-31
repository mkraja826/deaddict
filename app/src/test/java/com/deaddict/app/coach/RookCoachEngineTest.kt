package com.deaddict.app.coach

import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.programs.SafetyTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RookCoachEngineTest {
    @Test
    fun medicallyHighRiskTrackForcesDirectSafetyMessage() {
        val message = RookCoachEngine.message(
            request(
                moment = RookMoment.RESCUE_ACTIVE,
                tone = RookTone.BRUTAL_BANTER,
                safetyTier = SafetyTier.MEDICALLY_HIGH_RISK,
            ),
        )

        assertTrue(message.safetyOverride)
        assertEquals(RookTone.DIRECT, message.toneUsed)
        assertTrue(message.body.contains("qualified medical guidance"))
        assertTrue(message.body.contains("emergency services"))
    }

    @Test
    fun clinicallySensitiveTrackDowngradesBrutalBanter() {
        val message = RookCoachEngine.message(
            request(
                moment = RookMoment.TODAY,
                tone = RookTone.BRUTAL_BANTER,
                safetyTier = SafetyTier.CLINICALLY_SENSITIVE,
                outcome = TrackCheckInOutcome.SLIP,
            ),
        )

        assertFalse(message.safetyOverride)
        assertEquals(RookTone.DIRECT, message.toneUsed)
        assertTrue(message.body.contains("qualified professional"))
    }

    @Test
    fun toneChangesCopyWithoutChangingTrackOwnership() {
        val direct = RookCoachEngine.message(
            request(
                moment = RookMoment.TODAY,
                tone = RookTone.DIRECT,
                outcome = TrackCheckInOutcome.GOAL_MET,
            ),
        )
        val brutal = RookCoachEngine.message(
            request(
                moment = RookMoment.TODAY,
                tone = RookTone.BRUTAL_BANTER,
                outcome = TrackCheckInOutcome.GOAL_MET,
            ),
        )

        assertNotEquals(direct.body, brutal.body)
        assertTrue(direct.body.contains(TRACK_TITLE))
        assertTrue(brutal.body.contains(TRACK_TITLE))
    }

    @Test
    fun rescueCompletionUsesRecordedUrgeChange() {
        val message = RookCoachEngine.message(
            request(
                moment = RookMoment.RESCUE_COMPLETE,
                tone = RookTone.QUIET,
                initialUrge = 5,
                finalUrge = 2,
            ),
        )

        assertTrue(message.body.contains("3 point"))
        assertEquals(RookTone.QUIET, message.toneUsed)
    }

    @Test
    fun insightsDeclineProducesActionableCopy() {
        val message = RookCoachEngine.message(
            request(
                moment = RookMoment.INSIGHTS,
                tone = RookTone.DIRECT,
                adherencePercent = 42,
                trend = RookTrend.DECLINING,
            ),
        )

        assertTrue(message.body.contains("42%"))
        assertTrue(message.body.contains("interrupt"))
    }

    private fun request(
        moment: RookMoment,
        tone: RookTone,
        safetyTier: SafetyTier = SafetyTier.GENERAL_SELF_MANAGEMENT,
        outcome: TrackCheckInOutcome? = null,
        initialUrge: Int? = null,
        finalUrge: Int? = null,
        adherencePercent: Int? = null,
        trend: RookTrend = RookTrend.NOT_ENOUGH_DATA,
    ) = RookCoachRequest(
        moment = moment,
        tone = tone,
        trackId = TRACK_ID,
        trackTitle = TRACK_TITLE,
        safetyTier = safetyTier,
        outcome = outcome,
        initialUrge = initialUrge,
        finalUrge = finalUrge,
        adherencePercent = adherencePercent,
        trend = trend,
        variationKey = 7L,
    )

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000777"
        const val TRACK_TITLE = "Gaming"
    }
}
