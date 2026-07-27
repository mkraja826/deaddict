package com.deaddict.app.insights

import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import java.time.Instant
import java.time.ZoneId

enum class TrendDirection { IMPROVING, STEADY, INCREASING, NOT_ENOUGH_DATA }

data class SevenDayInsights(
    val checkInCount: Int,
    val slipCount: Int,
    val averageUrge: Double?,
    val topTrigger: String?,
    val peakRiskPeriod: String?,
    val trend: TrendDirection,
    val rescueCount: Int,
    val rescuesWithReducedUrge: Int,
    val explanation: String,
)

object InsightAnalyzer {
    fun analyze(
        tracking: List<TrackingEventEntity>,
        rescues: List<RescueSessionEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): SevenDayInsights {
        val intensityEvents = tracking.filter { it.urgeIntensity != null }
        val averageUrge = intensityEvents.mapNotNull { it.urgeIntensity }.average().takeUnless(Double::isNaN)
        val topTrigger = tracking.mapNotNull { it.triggerKey }
            .groupingBy(String::lowercase)
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key })
            ?.key
        val periodCounts = tracking.groupingBy {
            riskPeriod(Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zoneId).hour)
        }.eachCount()
        val peakPeriod = periodCounts.maxWithOrNull(
            compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key },
        )?.key
        val midpoint = tracking.map(TrackingEventEntity::occurredAtEpochMillis)
            .minOrNull()
            ?.let { earliest ->
                val latest = tracking.maxOfOrNull(TrackingEventEntity::occurredAtEpochMillis) ?: earliest
                earliest + (latest - earliest) / 2
            }
        val trend = if (intensityEvents.size < 4 || midpoint == null) {
            TrendDirection.NOT_ENOUGH_DATA
        } else {
            val earlier = intensityEvents.filter { it.occurredAtEpochMillis <= midpoint }
                .mapNotNull { it.urgeIntensity }
                .average()
            val later = intensityEvents.filter { it.occurredAtEpochMillis > midpoint }
                .mapNotNull { it.urgeIntensity }
                .average()
            when {
                earlier.isNaN() || later.isNaN() -> TrendDirection.NOT_ENOUGH_DATA
                later <= earlier - 0.5 -> TrendDirection.IMPROVING
                later >= earlier + 0.5 -> TrendDirection.INCREASING
                else -> TrendDirection.STEADY
            }
        }
        val reducedRescues = rescues.count {
            val finalUrge = it.finalUrge
            finalUrge != null && finalUrge < it.initialUrge
        }
        val explanation = buildString {
            append("${tracking.size} check-ins were reviewed from the last seven days.")
            topTrigger?.let { append(" The most recorded trigger was $it.") }
            peakPeriod?.let { append(" Check-ins were most frequent in the $it.") }
            if (rescues.isNotEmpty()) {
                append(" Urge intensity decreased in $reducedRescues of ${rescues.size} completed Rescue sessions.")
            }
        }
        return SevenDayInsights(
            checkInCount = tracking.size,
            slipCount = tracking.count { it.kind == TrackingEventKind.SLIP },
            averageUrge = averageUrge,
            topTrigger = topTrigger,
            peakRiskPeriod = peakPeriod,
            trend = trend,
            rescueCount = rescues.size,
            rescuesWithReducedUrge = reducedRescues,
            explanation = explanation,
        )
    }

    private fun riskPeriod(hour: Int): String = when (hour) {
        in 5..10 -> "morning"
        in 11..16 -> "afternoon"
        in 17..22 -> "evening"
        else -> "late night"
    }
}
