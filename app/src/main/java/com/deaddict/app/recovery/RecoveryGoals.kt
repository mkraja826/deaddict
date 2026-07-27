package com.deaddict.app.recovery

import kotlin.math.max
import kotlin.math.min

/** Editable recovery goal independent of storage and UI layers. */
data class RecoveryGoal(
    val programId: String,
    val targetKind: GoalTargetKind,
    val targetValue: Double,
    val period: GoalPeriod,
    val startsAtEpochMillis: Long,
    val title: String,
) {
    init {
        require(programId.isNotBlank())
        require(targetValue > 0.0)
        require(title.isNotBlank())
    }
}

enum class GoalTargetKind { ABSTINENT_DAYS, MAX_QUANTITY, MAX_MINUTES, MAX_COST_MINOR_UNITS, CHECK_INS }
enum class GoalPeriod { DAILY, WEEKLY, MONTHLY, OPEN_ENDED }

data class GoalProgress(
    val currentValue: Double,
    val targetValue: Double,
    val lowerIsBetter: Boolean,
) {
    val fraction: Double
        get() = if (lowerIsBetter) {
            if (currentValue <= targetValue) 1.0 else (targetValue / currentValue).coerceIn(0.0, 1.0)
        } else {
            (currentValue / targetValue).coerceIn(0.0, 1.0)
        }

    val completed: Boolean
        get() = if (lowerIsBetter) currentValue <= targetValue else currentValue >= targetValue
}

data class RecoveryMilestone(val days: Int, val label: String)

object RecoveryGoalEngine {
    private val milestones = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)

    fun progress(goal: RecoveryGoal, observedValue: Double): GoalProgress = GoalProgress(
        currentValue = max(0.0, observedValue),
        targetValue = goal.targetValue,
        lowerIsBetter = goal.targetKind in setOf(
            GoalTargetKind.MAX_QUANTITY,
            GoalTargetKind.MAX_MINUTES,
            GoalTargetKind.MAX_COST_MINOR_UNITS,
        ),
    )

    fun achievedMilestones(abstinentDays: Int): List<RecoveryMilestone> = milestones
        .filter { it <= max(0, abstinentDays) }
        .map { RecoveryMilestone(it, milestoneLabel(it)) }

    fun nextMilestone(abstinentDays: Int): RecoveryMilestone? {
        val next = milestones.firstOrNull { it > max(0, abstinentDays) } ?: return null
        return RecoveryMilestone(next, milestoneLabel(next))
    }

    fun daysRemaining(abstinentDays: Int): Int? = nextMilestone(abstinentDays)?.let {
        max(0, it.days - max(0, abstinentDays))
    }

    fun normalizedScore(goalProgress: List<GoalProgress>): Int {
        if (goalProgress.isEmpty()) return 0
        val average = goalProgress.map { it.fraction }.average()
        return min(100, max(0, (average * 100).toInt()))
    }

    private fun milestoneLabel(days: Int): String = when (days) {
        1 -> "First day"
        3 -> "Three days"
        7 -> "One week"
        14 -> "Two weeks"
        30 -> "One month"
        60 -> "Two months"
        90 -> "Three months"
        180 -> "Six months"
        365 -> "One year"
        else -> "$days days"
    }
}
