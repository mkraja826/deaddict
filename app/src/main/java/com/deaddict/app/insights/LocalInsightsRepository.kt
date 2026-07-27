package com.deaddict.app.insights

import com.deaddict.database.DeAddictDatabase
import com.deaddict.programs.ProgramId
import java.time.Duration

class LocalInsightsRepository(
    private val database: DeAddictDatabase,
) {
    suspend fun sevenDays(
        programId: ProgramId,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights = analyze(programId, InsightWindow.SEVEN_DAYS, nowMillis)

    suspend fun thirtyDays(
        programId: ProgramId,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights = analyze(programId, InsightWindow.THIRTY_DAYS, nowMillis)

    suspend fun ninetyDays(
        programId: ProgramId,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights = analyze(programId, InsightWindow.NINETY_DAYS, nowMillis)

    suspend fun analyze(
        programId: ProgramId,
        window: InsightWindow,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights {
        val since = nowMillis - Duration.ofDays(window.days).toMillis()
        return InsightAnalyzer.analyze(
            tracking = database.trackingDao().since(programId.value, since),
            rescues = database.rescueDao().since(programId.value, since),
            window = window,
        )
    }
}
