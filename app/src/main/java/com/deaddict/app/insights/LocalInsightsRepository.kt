package com.deaddict.app.insights

import com.deaddict.database.DeAddictDatabase
import com.deaddict.model.RecoveryTrackId
import java.time.Duration

class LocalInsightsRepository(
    private val database: DeAddictDatabase,
) {
    suspend fun sevenDays(
        recoveryTrackId: RecoveryTrackId,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights = analyze(recoveryTrackId, InsightWindow.SEVEN_DAYS, nowMillis)

    suspend fun thirtyDays(
        recoveryTrackId: RecoveryTrackId,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights = analyze(recoveryTrackId, InsightWindow.THIRTY_DAYS, nowMillis)

    suspend fun ninetyDays(
        recoveryTrackId: RecoveryTrackId,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights = analyze(recoveryTrackId, InsightWindow.NINETY_DAYS, nowMillis)

    suspend fun analyze(
        recoveryTrackId: RecoveryTrackId,
        window: InsightWindow,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights {
        val since = nowMillis - Duration.ofDays(window.days).toMillis()
        return InsightAnalyzer.analyze(
            tracking = database.trackingDao().sinceTrack(recoveryTrackId.value, since),
            rescues = database.rescueDao().sinceTrack(recoveryTrackId.value, since),
            window = window,
        )
    }
}
