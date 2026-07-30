package com.deaddict.app.insights

import com.deaddict.database.dao.TrackCheckInProgressRow
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.RecoveryGoalType
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

enum class GoalProgressMode {
    ADHERENCE,
    AWARENESS,
    UNSCOPED,
}

enum class GoalProgressTrend {
    IMPROVING,
    STEADY,
    DECLINING,
    NOT_ENOUGH_DATA,
}

data class GoalProgressSegment(
    val goalVersionId: String?,
    val goalType: RecoveryGoalType?,
    val title: String?,
    val targetValue: Double?,
    val unitKey: String?,
    val periodType: GoalPeriodType?,
    val isCurrent: Boolean,
    val mode: GoalProgressMode,
    val eligibleDays: Int?,
    val confirmedDays: Int,
    val goalMetDays: Int,
    val partlyMetDays: Int,
    val goalNotMetDays: Int,
    val slipDays: Int,
    val awarenessDays: Int,
    val adherencePercent: Int?,
    val consistencyPercent: Int?,
    val latestRunDays: Int,
    val bestRunDays: Int,
    val averagePeakUrge: Double?,
    val measuredDays: Int,
    val averageMeasuredValue: Double?,
    val measurementUnit: String?,
    val trend: GoalProgressTrend,
    val explanation: String,
)

data class GoalProgressSummary(
    val currentGoal: GoalProgressSegment?,
    val previousGoals: List<GoalProgressSegment>,
    val totalConfirmedDays: Int,
    val goalChangesInWindow: Int,
    val window: InsightWindow,
)

object GoalProgressAnalyzer {
    fun analyze(
        rows: List<TrackCheckInProgressRow>,
        currentGoal: RecoveryGoalVersionEntity?,
        window: InsightWindow,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): GoalProgressSummary? {
        if (currentGoal == null && rows.isEmpty()) return null

        val endEpochDay = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .toEpochDay()
        val startEpochDay = endEpochDay - window.days + 1
        val currentGoalId = currentGoal?.id
        val currentRows = if (currentGoalId == null) {
            emptyList()
        } else {
            rows.filter { it.goalVersionId == currentGoalId }
        }
        val current = currentGoal?.let { goal ->
            val effectiveStart = Instant.ofEpochMilli(goal.effectiveFromEpochMillis)
                .atZone(zoneId)
                .toLocalDate()
                .toEpochDay()
            val eligibleStart = maxOf(startEpochDay, effectiveStart)
            val eligibleDays = (endEpochDay - eligibleStart + 1)
                .coerceAtLeast(0)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            buildSegment(
                rows = currentRows,
                goalVersionId = goal.id,
                goalType = goal.goalType,
                title = goal.title,
                targetValue = goal.targetValue,
                unitKey = goal.unitKey,
                periodType = goal.periodType,
                isCurrent = true,
                eligibleDays = eligibleDays,
            )
        }

        val previous = rows
            .filter { it.goalVersionId != currentGoalId }
            .groupBy { it.goalVersionId }
            .map { (goalVersionId, goalRows) ->
                val sample = goalRows.first()
                buildSegment(
                    rows = goalRows,
                    goalVersionId = goalVersionId,
                    goalType = sample.goalType,
                    title = sample.goalTitle,
                    targetValue = sample.targetValue,
                    unitKey = sample.goalUnitKey,
                    periodType = sample.periodType,
                    isCurrent = false,
                    eligibleDays = null,
                )
            }
            .sortedWith(
                compareByDescending<GoalProgressSegment> { segment ->
                    rows.filter { it.goalVersionId == segment.goalVersionId }
                        .maxOfOrNull(TrackCheckInProgressRow::localDateEpochDay)
                        ?: Long.MIN_VALUE
                }.thenBy { it.goalVersionId.orEmpty() },
            )

        return GoalProgressSummary(
            currentGoal = current,
            previousGoals = previous,
            totalConfirmedDays = rows.map(TrackCheckInProgressRow::localDateEpochDay).distinct().size,
            goalChangesInWindow = (listOfNotNull(current) + previous)
                .map(GoalProgressSegment::goalVersionId)
                .distinct()
                .size
                .minus(1)
                .coerceAtLeast(0),
            window = window,
        )
    }

    private fun buildSegment(
        rows: List<TrackCheckInProgressRow>,
        goalVersionId: String?,
        goalType: RecoveryGoalType?,
        title: String?,
        targetValue: Double?,
        unitKey: String?,
        periodType: GoalPeriodType?,
        isCurrent: Boolean,
        eligibleDays: Int?,
    ): GoalProgressSegment {
        val sortedRows = rows.sortedBy(TrackCheckInProgressRow::localDateEpochDay)
        val mode = when (goalType) {
            RecoveryGoalType.AWARENESS_ONLY -> GoalProgressMode.AWARENESS
            null -> GoalProgressMode.UNSCOPED
            else -> GoalProgressMode.ADHERENCE
        }
        val met = sortedRows.count { it.outcome == TrackCheckInOutcome.GOAL_MET }
        val partial = sortedRows.count { it.outcome == TrackCheckInOutcome.GOAL_PARTLY_MET }
        val notMet = sortedRows.count { it.outcome == TrackCheckInOutcome.GOAL_NOT_MET }
        val slips = sortedRows.count { it.outcome == TrackCheckInOutcome.SLIP }
        val awareness = sortedRows.count { it.outcome == TrackCheckInOutcome.AWARENESS_LOGGED }
        val scoredDays = met + partial + notMet + slips
        val adherence = if (mode == GoalProgressMode.ADHERENCE && scoredDays > 0) {
            (((met + partial * PARTIAL_CREDIT) / scoredDays) * 100).roundToInt()
        } else {
            null
        }
        val consistency = eligibleDays
            ?.takeIf { it > 0 }
            ?.let { denominator ->
                ((sortedRows.map(TrackCheckInProgressRow::localDateEpochDay).distinct().size.toDouble() /
                    denominator) * 100)
                    .roundToInt()
                    .coerceIn(0, 100)
            }
        val qualifyingDates = sortedRows
            .filter { row ->
                when (mode) {
                    GoalProgressMode.AWARENESS -> true
                    GoalProgressMode.ADHERENCE -> row.outcome == TrackCheckInOutcome.GOAL_MET
                    GoalProgressMode.UNSCOPED -> false
                }
            }
            .map(TrackCheckInProgressRow::localDateEpochDay)
            .distinct()
            .sorted()
        val runs = consecutiveRuns(qualifyingDates)
        val latestConfirmedDate = sortedRows.lastOrNull()?.localDateEpochDay
        val latestRun = if (qualifyingDates.lastOrNull() == latestConfirmedDate) {
            runs.lastOrNull() ?: 0
        } else {
            0
        }
        val matchingMeasurements = sortedRows.filter { row ->
            val measured = row.measuredValue
            measured != null && unitsMatch(row.unitKey, unitKey)
        }
        val averageMeasured = matchingMeasurements
            .mapNotNull(TrackCheckInProgressRow::measuredValue)
            .average()
            .takeUnless(Double::isNaN)
        val averagePeakUrge = sortedRows
            .mapNotNull(TrackCheckInProgressRow::peakUrge)
            .average()
            .takeUnless(Double::isNaN)
        val trend = adherenceTrend(sortedRows, mode)
        val explanation = when (mode) {
            GoalProgressMode.AWARENESS -> {
                "Awareness goals are shown as check-in consistency, not success or failure. " +
                    "${sortedRows.size} confirmed day${if (sortedRows.size == 1) "" else "s"} used this goal version."
            }
            GoalProgressMode.ADHERENCE -> {
                "$scoredDays confirmed outcome${if (scoredDays == 1) "" else "s"} were evaluated only " +
                    "against this goal version. Goal changes are kept separate."
            }
            GoalProgressMode.UNSCOPED -> {
                "This older check-in no longer has its goal definition, so it is kept visible but not scored."
            }
        }

        return GoalProgressSegment(
            goalVersionId = goalVersionId,
            goalType = goalType,
            title = title,
            targetValue = targetValue,
            unitKey = unitKey,
            periodType = periodType,
            isCurrent = isCurrent,
            mode = mode,
            eligibleDays = eligibleDays,
            confirmedDays = sortedRows.map(TrackCheckInProgressRow::localDateEpochDay).distinct().size,
            goalMetDays = met,
            partlyMetDays = partial,
            goalNotMetDays = notMet,
            slipDays = slips,
            awarenessDays = awareness,
            adherencePercent = adherence,
            consistencyPercent = consistency,
            latestRunDays = latestRun,
            bestRunDays = runs.maxOrNull() ?: 0,
            averagePeakUrge = averagePeakUrge,
            measuredDays = matchingMeasurements.size,
            averageMeasuredValue = averageMeasured,
            measurementUnit = unitKey?.trim()?.takeIf(String::isNotEmpty),
            trend = trend,
            explanation = explanation,
        )
    }

    private fun adherenceTrend(
        rows: List<TrackCheckInProgressRow>,
        mode: GoalProgressMode,
    ): GoalProgressTrend {
        if (mode != GoalProgressMode.ADHERENCE) return GoalProgressTrend.NOT_ENOUGH_DATA
        val scores = rows.mapNotNull { row ->
            when (row.outcome) {
                TrackCheckInOutcome.GOAL_MET -> 1.0
                TrackCheckInOutcome.GOAL_PARTLY_MET -> PARTIAL_CREDIT
                TrackCheckInOutcome.GOAL_NOT_MET,
                TrackCheckInOutcome.SLIP,
                -> 0.0
                TrackCheckInOutcome.AWARENESS_LOGGED -> null
            }
        }
        if (scores.size < MIN_TREND_SAMPLES) return GoalProgressTrend.NOT_ENOUGH_DATA
        val split = scores.size / 2
        val earlier = scores.take(split).average()
        val later = scores.drop(split).average()
        return when {
            later >= earlier + TREND_THRESHOLD -> GoalProgressTrend.IMPROVING
            later <= earlier - TREND_THRESHOLD -> GoalProgressTrend.DECLINING
            else -> GoalProgressTrend.STEADY
        }
    }

    private fun consecutiveRuns(days: List<Long>): List<Int> {
        if (days.isEmpty()) return emptyList()
        val runs = mutableListOf<Int>()
        var current = 1
        for (index in 1 until days.size) {
            if (days[index] == days[index - 1] + 1) {
                current += 1
            } else {
                runs += current
                current = 1
            }
        }
        runs += current
        return runs
    }

    private fun unitsMatch(recorded: String?, expected: String?): Boolean {
        if (recorded.isNullOrBlank() || expected.isNullOrBlank()) return false
        return recorded.trim().equals(expected.trim(), ignoreCase = true)
    }

    private const val PARTIAL_CREDIT = 0.5
    private const val TREND_THRESHOLD = 0.15
    private const val MIN_TREND_SAMPLES = 4
}
