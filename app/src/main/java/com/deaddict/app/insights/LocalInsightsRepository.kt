package com.deaddict.app.insights

import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.repository.RecoveryOwnerContext
import com.deaddict.model.RecoveryTrackId
import com.deaddict.programs.ProgramId
import java.time.Duration

class LocalInsightsRepository(
    private val database: DeAddictDatabase,
    private val ownerContext: RecoveryOwnerContext? = null,
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

    /**
     * Temporary compatibility bridge for the current AppViewModel. The selected permanent
     * Recovery Track remains authoritative; the program ID is validated but never used to
     * combine separate journeys.
     */
    suspend fun sevenDays(
        programId: ProgramId,
        nowMillis: Long = System.currentTimeMillis(),
    ): SevenDayInsights {
        val selection = checkNotNull(ownerContext?.current()) {
            "Selected Recovery Track context is unavailable"
        }
        val track = checkNotNull(
            database.recoveryTrackDao().byId(selection.recoveryTrackId.value),
        ) { "Selected Recovery Track no longer exists" }
        check(track.ownerKey == selection.ownerKey.value) {
            "Selected Recovery Track belongs to another owner"
        }
        check(track.programId == programId.value) {
            "Selected Recovery Track does not match the requested program"
        }
        return sevenDays(selection.recoveryTrackId, nowMillis)
    }
}
