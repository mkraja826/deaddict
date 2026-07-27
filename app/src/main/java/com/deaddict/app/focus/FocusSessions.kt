package com.deaddict.app.focus

import kotlin.math.max

/** Pure focus-session model that can be persisted or rendered by any UI layer. */
data class FocusSession(
    val id: String,
    val programId: String,
    val durationMinutes: Int,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val interruptedAtEpochMillis: Long? = null,
    val activity: FocusActivity = FocusActivity.BREATHE,
) {
    init {
        require(id.isNotBlank())
        require(programId.isNotBlank())
        require(durationMinutes in 1..240)
        require(completedAtEpochMillis == null || interruptedAtEpochMillis == null)
        require(completedAtEpochMillis == null || completedAtEpochMillis >= startedAtEpochMillis)
        require(interruptedAtEpochMillis == null || interruptedAtEpochMillis >= startedAtEpochMillis)
    }

    val state: FocusSessionState
        get() = when {
            completedAtEpochMillis != null -> FocusSessionState.COMPLETED
            interruptedAtEpochMillis != null -> FocusSessionState.INTERRUPTED
            else -> FocusSessionState.ACTIVE
        }
}

enum class FocusActivity { BREATHE, WALK, JOURNAL, HYDRATE, DELAY, CUSTOM }
enum class FocusSessionState { ACTIVE, COMPLETED, INTERRUPTED }

data class FocusSessionProgress(
    val elapsedMillis: Long,
    val remainingMillis: Long,
    val fraction: Double,
    val expired: Boolean,
)

data class FocusSessionStats(
    val started: Int,
    val completed: Int,
    val interrupted: Int,
    val completionRate: Double?,
    val completedMinutes: Int,
    val currentStreak: Int,
)

object FocusSessionEngine {
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_DAY = 86_400_000L

    fun progress(session: FocusSession, nowEpochMillis: Long): FocusSessionProgress {
        val totalMillis = session.durationMinutes * MILLIS_PER_MINUTE
        val terminalTime = session.completedAtEpochMillis ?: session.interruptedAtEpochMillis
        val effectiveNow = terminalTime ?: nowEpochMillis
        val elapsed = (effectiveNow - session.startedAtEpochMillis).coerceIn(0L, totalMillis)
        val remaining = max(0L, totalMillis - elapsed)
        return FocusSessionProgress(
            elapsedMillis = elapsed,
            remainingMillis = remaining,
            fraction = elapsed.toDouble() / totalMillis,
            expired = elapsed >= totalMillis,
        )
    }

    fun complete(session: FocusSession, nowEpochMillis: Long): FocusSession {
        require(session.state == FocusSessionState.ACTIVE) { "Only active sessions can be completed" }
        require(nowEpochMillis >= session.startedAtEpochMillis)
        return session.copy(completedAtEpochMillis = nowEpochMillis)
    }

    fun interrupt(session: FocusSession, nowEpochMillis: Long): FocusSession {
        require(session.state == FocusSessionState.ACTIVE) { "Only active sessions can be interrupted" }
        require(nowEpochMillis >= session.startedAtEpochMillis)
        return session.copy(interruptedAtEpochMillis = nowEpochMillis)
    }

    fun stats(sessions: List<FocusSession>, nowEpochMillis: Long): FocusSessionStats {
        val completed = sessions.filter { it.state == FocusSessionState.COMPLETED }
        val interrupted = sessions.count { it.state == FocusSessionState.INTERRUPTED }
        val started = sessions.size
        val completionRate = started.takeIf { it > 0 }?.let { completed.size.toDouble() / it }
        val completedMinutes = completed.sumOf { it.durationMinutes }
        val completedDays = completed
            .map { (it.completedAtEpochMillis ?: it.startedAtEpochMillis) / MILLIS_PER_DAY }
            .distinct()
            .toSet()
        var streak = 0
        var day = nowEpochMillis / MILLIS_PER_DAY
        while (day in completedDays) {
            streak++
            day--
        }
        return FocusSessionStats(
            started = started,
            completed = completed.size,
            interrupted = interrupted,
            completionRate = completionRate,
            completedMinutes = completedMinutes,
            currentStreak = streak,
        )
    }
}
