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
    ): SevenDayInsights {
        val since = nowMillis - Duration.ofDays(7).toMillis()
        return InsightAnalyzer.analyze(
            tracking = database.trackingDao().since(programId.value, since),
            rescues = database.rescueDao().since(programId.value, since),
        )
    }
}

