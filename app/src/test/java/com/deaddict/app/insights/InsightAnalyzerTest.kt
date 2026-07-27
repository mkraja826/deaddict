package com.deaddict.app.insights

import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class InsightAnalyzerTest {
    @Test
    fun `analysis explains trigger period trend and rescue effectiveness`() {
        val times = listOf(
            "2026-07-21T18:00:00Z",
            "2026-07-22T19:00:00Z",
            "2026-07-25T18:30:00Z",
            "2026-07-26T20:00:00Z",
        ).map(Instant::parse).map(Instant::toEpochMilli)
        val events = listOf(
            event("1", times[0], 5, "stress", TrackingEventKind.URGE),
            event("2", times[1], 4, "stress", TrackingEventKind.SLIP),
            event("3", times[2], 2, "routine", TrackingEventKind.URGE),
            event("4", times[3], 2, "stress", TrackingEventKind.CRAVING),
        )
        val rescue = RescueSessionEntity(
            id = "rescue-1",
            programId = "gaming",
            startedAtEpochMillis = times[3],
            completedAtEpochMillis = times[3] + 120_000,
            initialUrge = 5,
            finalUrge = 2,
            triggerKey = "stress",
            actionKeys = listOf("walk"),
            outcome = RescueOutcome.REDUCED,
            syncState = SyncState.LOCAL_ONLY,
        )

        val result = InsightAnalyzer.analyze(events, listOf(rescue), ZoneId.of("UTC"))

        assertEquals("stress", result.topTrigger)
        assertEquals("evening", result.peakRiskPeriod)
        assertEquals(TrendDirection.IMPROVING, result.trend)
        assertEquals(1, result.slipCount)
        assertEquals(1, result.rescuesWithReducedUrge)
        assertTrue(result.explanation.contains("4 check-ins"))
    }

    @Test
    fun `small sample does not invent a trend`() {
        val result = InsightAnalyzer.analyze(
            tracking = listOf(
                event("1", 1_000, 5, null, TrackingEventKind.URGE),
            ),
            rescues = emptyList(),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(TrendDirection.NOT_ENOUGH_DATA, result.trend)
    }

    private fun event(
        id: String,
        timestamp: Long,
        intensity: Int,
        trigger: String?,
        kind: TrackingEventKind,
    ) = TrackingEventEntity(
        id = id,
        programId = "gaming",
        kind = kind,
        quantity = null,
        unit = null,
        costMinorUnits = null,
        urgeIntensity = intensity,
        triggerKey = trigger,
        occurredAtEpochMillis = timestamp,
        createdAtEpochMillis = timestamp,
        privateNote = null,
        syncState = SyncState.LOCAL_ONLY,
    )
}

