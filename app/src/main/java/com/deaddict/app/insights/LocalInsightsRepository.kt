package com.deaddict.app.insights

import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.repository.RecoveryOwnerContext
import com.deaddict.model.RecoveryTrackId
import com.deaddict.programs.ProgramId
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

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
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): SevenDayInsights {
        val track = checkNotNull(database.recoveryTrackDao().byId(recoveryTrackId.value)) {
            "Recovery Track no longer exists"
        }
        val since = nowMillis - Duration.ofDays(window.days).toMillis()
        val endEpochDay = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .toEpochDay()
        val startEpochDay = endEpochDay - window.days + 1
        val goalProgress = GoalProgressAnalyzer.analyze(
            rows = database.dailyCheckInDao().progressRows(
                ownerKey = track.ownerKey,
                recoveryTrackId = recoveryTrackId.value,
                startEpochDay = startEpochDay,
                endEpochDay = endEpochDay,
            ),
            currentGoal = database.recoveryGoalDao().current(recoveryTrackId.value),
            window = window,
            nowMillis = nowMillis,
            zoneId = zoneId,
        )
        return InsightAnalyzer.analyze(
            tracking = database.trackingDao().sinceTrack(recoveryTrackId.value, since),
            rescues = database.rescueDao().sinceTrack(recoveryTrackId.value, since),
            zoneId = zoneId,
            window = window,
        ).copy(goalProgress = goalProgress)
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
