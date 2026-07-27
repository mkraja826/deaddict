package com.deaddict.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.os.Build
import java.time.Instant
import java.time.ZoneId

data class AppUsageEstimate(
    val packageName: String,
    val foregroundMinutes: Long,
    val openingEstimate: Int,
    val sessionEstimate: Int,
    val rapidReopenings: Int,
)

data class DailyUsageEstimate(
    val totalForegroundMinutes: Long,
    val totalOpeningEstimate: Int,
    val totalSessionEstimate: Int,
    val rapidReopenings: Int,
    val morningOpenings: Int,
    val lateNightOpenings: Int,
    val topApps: List<AppUsageEstimate>,
)

class DigitalUsageRepository(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(UsageStatsManager::class.java),
    private val appOpsManager: AppOpsManager =
        context.getSystemService(AppOpsManager::class.java),
) {
    fun hasUsageAccess(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun estimateDay(nowMillis: Long = System.currentTimeMillis()): DailyUsageEstimate? {
        if (!hasUsageAccess()) return null
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(nowMillis)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val durations = usageStatsManager
            .queryAndAggregateUsageStats(start, nowMillis)
            .filterKeys { it != context.packageName }
            .mapValues { (_, stats) -> stats.totalTimeInForeground.coerceAtLeast(0L) }
        val openings = mutableMapOf<String, MutableList<Long>>()
        val events = usageStatsManager.queryEvents(start, nowMillis)
        val event = UsageEvents.Event()
        while (events?.hasNextEvent() == true) {
            events.getNextEvent(event)
            if (
                event.packageName != context.packageName &&
                isForegroundEvent(event.eventType)
            ) {
                openings.getOrPut(event.packageName) { mutableListOf() }.add(event.timeStamp)
            }
        }
        var morning = 0
        var lateNight = 0
        val appEstimates = (durations.keys + openings.keys).map { packageName ->
            val appOpenings = openings[packageName].orEmpty().sorted()
            appOpenings.forEach { timestamp ->
                when (Instant.ofEpochMilli(timestamp).atZone(zone).hour) {
                    in 5..9 -> morning++
                    in 23..23, in 0..4 -> lateNight++
                }
            }
            val sessions = appOpenings.countStartsAfter(SESSION_GAP_MILLIS)
            val rapid = appOpenings.countStartsWithin(RAPID_REOPEN_MILLIS)
            AppUsageEstimate(
                packageName = packageName,
                foregroundMinutes = (durations[packageName] ?: 0L) / 60_000L,
                openingEstimate = appOpenings.size,
                sessionEstimate = sessions,
                rapidReopenings = rapid,
            )
        }.sortedByDescending(AppUsageEstimate::foregroundMinutes)
        return DailyUsageEstimate(
            totalForegroundMinutes = appEstimates.sumOf(AppUsageEstimate::foregroundMinutes),
            totalOpeningEstimate = appEstimates.sumOf(AppUsageEstimate::openingEstimate),
            totalSessionEstimate = appEstimates.sumOf(AppUsageEstimate::sessionEstimate),
            rapidReopenings = appEstimates.sumOf(AppUsageEstimate::rapidReopenings),
            morningOpenings = morning,
            lateNightOpenings = lateNight,
            topApps = appEstimates.take(5),
        )
    }

    private companion object {
        const val SESSION_GAP_MILLIS = 5 * 60 * 1_000L
        const val RAPID_REOPEN_MILLIS = 60 * 1_000L
    }
}

private fun isForegroundEvent(eventType: Int): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        eventType == UsageEvents.Event.ACTIVITY_RESUMED
    } else {
        @Suppress("DEPRECATION")
        eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
    }
