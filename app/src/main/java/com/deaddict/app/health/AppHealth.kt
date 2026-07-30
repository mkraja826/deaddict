package com.deaddict.app.health

import android.content.Context
import java.io.File
import kotlin.system.exitProcess

enum class AppHealthMetric(val storageKey: String) {
    APP_START("app_start"),
    PREVIOUS_CRASH("previous_crash"),
    DAILY_CHECK_IN_SAVE_SUCCESS("daily_check_in_save_success"),
    DAILY_CHECK_IN_SAVE_FAILURE("daily_check_in_save_failure"),
    DAILY_CHECK_IN_SAVE_SLOW("daily_check_in_save_slow"),
    INSIGHTS_LOAD_SUCCESS("insights_load_success"),
    INSIGHTS_LOAD_FAILURE("insights_load_failure"),
    INSIGHTS_LOAD_SLOW("insights_load_slow"),
}

object AppHealthThresholds {
    const val DAILY_CHECK_IN_SLOW_MILLIS = 1_500L
    const val INSIGHTS_LOAD_SLOW_MILLIS = 2_000L
    const val MINIMUM_OPERATION_SAMPLE = 20L
    const val WATCH_FAILURE_RATE = 0.02
    const val HOLD_FAILURE_RATE = 0.05
    const val WATCH_SLOW_RATE = 0.10
    const val HOLD_SLOW_RATE = 0.20
    const val WATCH_CRASHES = 1L
    const val HOLD_CRASHES = 2L
}

data class AppHealthSnapshot(
    val counts: Map<AppHealthMetric, Long> = emptyMap(),
) {
    fun count(metric: AppHealthMetric): Long = counts[metric] ?: 0L

    val operationSuccesses: Long
        get() = count(AppHealthMetric.DAILY_CHECK_IN_SAVE_SUCCESS) +
            count(AppHealthMetric.INSIGHTS_LOAD_SUCCESS)

    val operationFailures: Long
        get() = count(AppHealthMetric.DAILY_CHECK_IN_SAVE_FAILURE) +
            count(AppHealthMetric.INSIGHTS_LOAD_FAILURE)

    val slowOperations: Long
        get() = count(AppHealthMetric.DAILY_CHECK_IN_SAVE_SLOW) +
            count(AppHealthMetric.INSIGHTS_LOAD_SLOW)

    val operationSamples: Long
        get() = operationSuccesses + operationFailures

    val failureRate: Double
        get() = operationFailures.toRate(operationSamples)

    val slowRate: Double
        get() = slowOperations.toRate(operationSamples)
}

enum class RolloutDecision {
    PROCEED,
    WATCH,
    HOLD,
}

data class RolloutAssessment(
    val decision: RolloutDecision,
    val reasons: List<String>,
)

object ReleaseHealthGuardrail {
    fun assess(snapshot: AppHealthSnapshot): RolloutAssessment {
        val holdReasons = mutableListOf<String>()
        val watchReasons = mutableListOf<String>()
        val crashes = snapshot.count(AppHealthMetric.PREVIOUS_CRASH)
        val hasEnoughOperations = snapshot.operationSamples >= AppHealthThresholds.MINIMUM_OPERATION_SAMPLE

        if (crashes >= AppHealthThresholds.HOLD_CRASHES) {
            holdReasons += "$crashes previous crashes were detected."
        } else if (crashes >= AppHealthThresholds.WATCH_CRASHES) {
            watchReasons += "A previous crash was detected."
        }

        if (hasEnoughOperations && snapshot.failureRate >= AppHealthThresholds.HOLD_FAILURE_RATE) {
            holdReasons += "Operation failure rate is ${snapshot.failureRate.asPercent()}."
        } else if (hasEnoughOperations && snapshot.failureRate >= AppHealthThresholds.WATCH_FAILURE_RATE) {
            watchReasons += "Operation failure rate is ${snapshot.failureRate.asPercent()}."
        }

        if (hasEnoughOperations && snapshot.slowRate >= AppHealthThresholds.HOLD_SLOW_RATE) {
            holdReasons += "Slow-operation rate is ${snapshot.slowRate.asPercent()}."
        } else if (hasEnoughOperations && snapshot.slowRate >= AppHealthThresholds.WATCH_SLOW_RATE) {
            watchReasons += "Slow-operation rate is ${snapshot.slowRate.asPercent()}."
        }

        return when {
            holdReasons.isNotEmpty() -> RolloutAssessment(RolloutDecision.HOLD, holdReasons)
            watchReasons.isNotEmpty() -> RolloutAssessment(RolloutDecision.WATCH, watchReasons)
            else -> RolloutAssessment(
                RolloutDecision.PROCEED,
                listOf("No local release-health threshold is currently exceeded."),
            )
        }
    }
}

interface AppHealthReporter {
    fun record(metric: AppHealthMetric)
    fun snapshot(): AppHealthSnapshot
    fun clear()
}

class SharedPreferencesAppHealthReporter(context: Context) : AppHealthReporter {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    override fun record(metric: AppHealthMetric) {
        synchronized(lock) {
            val next = preferences.getLong(metric.storageKey, 0L) + 1L
            preferences.edit().putLong(metric.storageKey, next).apply()
        }
    }

    override fun snapshot(): AppHealthSnapshot = synchronized(lock) {
        AppHealthSnapshot(
            counts = AppHealthMetric.entries.associateWith { metric ->
                preferences.getLong(metric.storageKey, 0L)
            },
        )
    }

    override fun clear() {
        synchronized(lock) {
            preferences.edit().clear().apply()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "app_health_counters"
    }
}

class CrashMarkerStore(private val markerFile: File) {
    fun markCrash(timestampEpochMillis: Long = System.currentTimeMillis()) {
        markerFile.parentFile?.mkdirs()
        markerFile.writeText(timestampEpochMillis.toString())
    }

    fun consumeCrashMarker(): Boolean {
        if (!markerFile.exists()) return false
        runCatching { markerFile.delete() }
        return true
    }
}

class AppStabilityMonitor(
    context: Context,
    private val reporter: AppHealthReporter,
) {
    private val crashMarkerStore = CrashMarkerStore(
        File(context.noBackupFilesDir, "stability/previous_crash.marker"),
    )
    private var installed = false

    fun start() {
        reporter.record(AppHealthMetric.APP_START)
        if (crashMarkerStore.consumeCrashMarker()) {
            reporter.record(AppHealthMetric.PREVIOUS_CRASH)
        }
        installCrashMarkerHandler()
    }

    @Synchronized
    private fun installCrashMarkerHandler() {
        if (installed) return
        installed = true
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { crashMarkerStore.markCrash() }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(10)
            }
        }
    }
}

private fun Long.toRate(total: Long): Double =
    if (total <= 0L) 0.0 else toDouble() / total.toDouble()

private fun Double.asPercent(): String = "%.1f%%".format(this * 100.0)
