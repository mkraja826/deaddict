package com.deaddict.app.insights

import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightAnalyzerEdgeCasesTest {
    @Test fun emptyInputProducesSafeEmptyInsights() {
        val result = InsightAnalyzer.analyze(emptyList(), emptyList(), ZoneId.of("UTC"))
        assertEquals(0, result.checkInCount)
        assertEquals(TrendDirection.NOT_ENOUGH_DATA, result.trend)
        assertNull(result.averageUrge)
        assertNull(result.topTrigger)
        assertNull(result.peakRiskPeriod)
        assertNull(result.rescueEffectivenessPercent)
    }

    @Test fun triggerNormalizationIsStableAndDeterministic() {
        val events = listOf(
            event("1", " Stress ", 1_000, 3),
            event("2", "stress", 2_000, 2),
            event("3", "Boredom", 3_000, null),
            event("4", "boredom", 4_000, null),
        )
        val result = InsightAnalyzer.analyze(events, emptyList(), ZoneId.of("UTC"))
        assertEquals("boredom", result.topTrigger)
        assertEquals(listOf("boredom", "stress"), result.triggerFrequencies.map { it.trigger })
    }

    @Test fun rescueEffectivenessCountsOnlyReducedUrges() {
        val rescues = listOf(
            rescue("1", 5, 2),
            rescue("2", 3, 3),
            rescue("3", 2, null),
        )
        val result = InsightAnalyzer.analyze(emptyList(), rescues, ZoneId.of("UTC"))
        assertEquals(1, result.rescuesWithReducedUrge)
        assertEquals(33, result.rescueEffectivenessPercent)
    }

    private fun event(id: String, trigger: String?, at: Long, urge: Int?) = TrackingEventEntity(
        id = id,
        programId = "program",
        kind = TrackingEventKind.URGE,
        quantity = null,
        unit = null,
        costMinorUnits = null,
        urgeIntensity = urge,
        triggerKey = trigger,
        occurredAtEpochMillis = at,
        createdAtEpochMillis = at,
        privateNote = null,
        syncState = SyncState.SYNCED,
    )

    private fun rescue(id: String, initial: Int, final: Int?) = RescueSessionEntity(
        id = id,
        programId = "program",
        startedAtEpochMillis = id.toLong(),
        completedAtEpochMillis = id.toLong() + 1,
        initialUrge = initial,
        finalUrge = final,
        triggerKey = null,
        actionKeys = emptyList(),
        outcome = final?.let { if (it < initial) RescueOutcome.REDUCED else RescueOutcome.SAME },
        syncState = SyncState.SYNCED,
    )
}
