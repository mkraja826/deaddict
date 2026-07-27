package com.deaddict.app.insights

import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyInsightPoint(
    val date: LocalDate,
    val checkIns: Int,
    val slips: Int,
    val averageUrge: Double?,
    val rescues: Int,
    val successfulRescues: Int,
)

data class LongTermInsights(
    val days: Int,
    val totalCheckIns: Int,
    val totalSlips: Int,
    val averageUrge: Double?,
    val topTriggers: List<Pair<String, Int>>,
    val trend: TrendDirection,
    val rescueSuccessRate: Double?,
    val daily: List<DailyInsightPoint>,
)

object LongTermInsightAnalyzer {
    fun analyze(
        days: Int,
        tracking: List<TrackingEventEntity>,
        rescues: List<RescueSessionEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LongTermInsights {
        require(days > 0) { "days must be positive" }

        val intensityEvents = tracking.filter { it.urgeIntensity != null }
        val averageUrge = intensityEvents.mapNotNull { it.urgeIntensity }
            .average()
            .takeUnless(Double::isNaN)
        val topTriggers = tracking.mapNotNull { it.triggerKey?.trim()?.lowercase() }
            .filter(String::isNotEmpty)
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(5)
            .map { it.key to it.value }
        val successfulRescues = rescues.count {
            val finalUrge = it.finalUrge
            finalUrge != null && finalUrge < it.initialUrge
        }
        val rescueSuccessRate = rescues.takeIf(List<*>::isNotEmpty)
            ?.let { successfulRescues.toDouble() / it.size }

        val trackingByDay = tracking.groupBy {
            Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zoneId).toLocalDate()
        }
        val rescueByDay = rescues.groupBy {
            Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(zoneId).toLocalDate()
        }
        val dates = (trackingByDay.keys + rescueByDay.keys).distinct().sorted()
        val daily = dates.map { date ->
            val dayTracking = trackingByDay[date].orEmpty()
            val dayRescues = rescueByDay[date].orEmpty()
            DailyInsightPoint(
                date = date,
                checkIns = dayTracking.size,
                slips = dayTracking.count { it.kind == TrackingEventKind.SLIP },
                averageUrge = dayTracking.mapNotNull { it.urgeIntensity }
                    .average()
                    .takeUnless(Double::isNaN),
                rescues = dayRescues.size,
                successfulRescues = dayRescues.count {
                    val finalUrge = it.finalUrge
                    finalUrge != null && finalUrge < it.initialUrge
                },
            )
        }

        return LongTermInsights(
            days = days,
            totalCheckIns = tracking.size,
            totalSlips = tracking.count { it.kind == TrackingEventKind.SLIP },
            averageUrge = averageUrge,
            topTriggers = topTriggers,
            trend = trend(intensityEvents),
            rescueSuccessRate = rescueSuccessRate,
            daily = daily,
        )
    }

    private fun trend(events: List<TrackingEventEntity>): TrendDirection {
        if (events.size < 6) return TrendDirection.NOT_ENOUGH_DATA
        val ordered = events.sortedBy { it.occurredAtEpochMillis }
        val split = ordered.size / 2
        val earlier = ordered.take(split).mapNotNull { it.urgeIntensity }.average()
        val later = ordered.drop(split).mapNotNull { it.urgeIntensity }.average()
        return when {
            earlier.isNaN() || later.isNaN() -> TrendDirection.NOT_ENOUGH_DATA
            later <= earlier - 0.5 -> TrendDirection.IMPROVING
            later >= earlier + 0.5 -> TrendDirection.INCREASING
            else -> TrendDirection.STEADY
        }
    }
}
