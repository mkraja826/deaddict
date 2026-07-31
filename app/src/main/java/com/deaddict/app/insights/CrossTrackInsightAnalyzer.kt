package com.deaddict.app.insights

import com.deaddict.database.dao.CrossTrackOutcomeRow
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.TrackCheckInOutcome
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class CrossTrackPattern {
    MOVE_TOGETHER,
    POSSIBLE_SHIFT_TOWARD_OTHER,
    POSSIBLE_SHIFT_TOWARD_SELECTED,
    MIXED,
    NOT_ENOUGH_DATA,
}

data class CrossTrackPairInsight(
    val otherTrackId: String,
    val pairedDays: Int,
    val comparableDays: Int,
    val bothMetDays: Int,
    val bothDifficultDays: Int,
    val selectedMetOtherDifficultDays: Int,
    val selectedDifficultOtherMetDays: Int,
    val pattern: CrossTrackPattern,
    val dominantPatternPercent: Int?,
)

data class ReplacementActionInsight(
    val actionKey: String,
    val attempts: Int,
    val reducedUrgeCount: Int,
    val reducedUrgePercent: Int,
    val averageUrgeDrop: Double,
)

data class CrossTrackInsightSummary(
    val pairings: List<CrossTrackPairInsight>,
    val replacementActions: List<ReplacementActionInsight>,
    val sharedDifficultDays: Int,
    val possibleShiftDays: Int,
    val averageStressOnSharedDifficultDays: Double?,
    val averageSleepOnSharedDifficultDays: Double?,
    val explanation: String,
)

object CrossTrackInsightAnalyzer {
    fun analyze(
        rows: List<CrossTrackOutcomeRow>,
        rescues: List<RescueSessionEntity>,
    ): CrossTrackInsightSummary? {
        val pairings = rows
            .groupBy(CrossTrackOutcomeRow::otherTrackId)
            .map { (otherTrackId, pairRows) -> analyzePair(otherTrackId, pairRows) }
            .sortedWith(
                compareByDescending<CrossTrackPairInsight> { it.comparableDays }
                    .thenByDescending { it.pairedDays }
                    .thenBy { it.otherTrackId },
            )

        val rowsByDay = rows.groupBy(CrossTrackOutcomeRow::localDateEpochDay)
        val sharedDifficultRows = rowsByDay.values.mapNotNull { dayRows ->
            dayRows.firstOrNull()?.takeIf { representative ->
                representative.selectedOutcome.isDifficult() &&
                    dayRows.any { it.otherOutcome.isDifficult() }
            }
        }
        val possibleShiftDays = rowsByDay.values.count { dayRows ->
            dayRows.firstOrNull()?.selectedOutcome == TrackCheckInOutcome.GOAL_MET &&
                dayRows.any { it.otherOutcome.isDifficult() }
        }
        val replacementActions = analyzeReplacementActions(rescues)

        if (pairings.isEmpty() && replacementActions.isEmpty()) return null

        return CrossTrackInsightSummary(
            pairings = pairings,
            replacementActions = replacementActions,
            sharedDifficultDays = sharedDifficultRows.size,
            possibleShiftDays = possibleShiftDays,
            averageStressOnSharedDifficultDays = sharedDifficultRows
                .mapNotNull(CrossTrackOutcomeRow::stress)
                .average()
                .takeUnless(Double::isNaN),
            averageSleepOnSharedDifficultDays = sharedDifficultRows
                .mapNotNull(CrossTrackOutcomeRow::sleepQuality)
                .average()
                .takeUnless(Double::isNaN),
            explanation = "These are repeated same-day associations, not proof that one Recovery Track caused another. " +
                "Replacement-action results only describe what happened after recorded Rescue attempts.",
        )
    }

    private fun analyzePair(
        otherTrackId: String,
        rows: List<CrossTrackOutcomeRow>,
    ): CrossTrackPairInsight {
        val uniqueDays = rows.distinctBy(CrossTrackOutcomeRow::localDateEpochDay)
        val comparable = uniqueDays.mapNotNull { row ->
            val selected = row.selectedOutcome.direction() ?: return@mapNotNull null
            val other = row.otherOutcome.direction() ?: return@mapNotNull null
            selected to other
        }
        val bothMet = comparable.count { (selected, other) ->
            selected == OutcomeDirection.MET && other == OutcomeDirection.MET
        }
        val bothDifficult = comparable.count { (selected, other) ->
            selected == OutcomeDirection.DIFFICULT && other == OutcomeDirection.DIFFICULT
        }
        val towardOther = comparable.count { (selected, other) ->
            selected == OutcomeDirection.MET && other == OutcomeDirection.DIFFICULT
        }
        val towardSelected = comparable.count { (selected, other) ->
            selected == OutcomeDirection.DIFFICULT && other == OutcomeDirection.MET
        }
        val sameDirection = bothMet + bothDifficult
        val candidates = listOf(
            CrossTrackPattern.MOVE_TOGETHER to sameDirection,
            CrossTrackPattern.POSSIBLE_SHIFT_TOWARD_OTHER to towardOther,
            CrossTrackPattern.POSSIBLE_SHIFT_TOWARD_SELECTED to towardSelected,
        )
        val maxCount = candidates.maxOfOrNull { it.second } ?: 0
        val minimumDominantCount = maxOf(
            MIN_DOMINANT_DAYS,
            ceil(comparable.size * MIN_DOMINANT_SHARE).toInt(),
        )
        val dominant = candidates.filter { it.second == maxCount && it.second >= minimumDominantCount }
        val pattern = when {
            comparable.size < MIN_COMPARABLE_DAYS -> CrossTrackPattern.NOT_ENOUGH_DATA
            dominant.size == 1 -> dominant.single().first
            else -> CrossTrackPattern.MIXED
        }
        val dominantPercent = maxCount
            .takeIf { comparable.isNotEmpty() }
            ?.let { ((it.toDouble() / comparable.size) * 100).roundToInt() }

        return CrossTrackPairInsight(
            otherTrackId = otherTrackId,
            pairedDays = uniqueDays.size,
            comparableDays = comparable.size,
            bothMetDays = bothMet,
            bothDifficultDays = bothDifficult,
            selectedMetOtherDifficultDays = towardOther,
            selectedDifficultOtherMetDays = towardSelected,
            pattern = pattern,
            dominantPatternPercent = dominantPercent,
        )
    }

    private fun analyzeReplacementActions(
        rescues: List<RescueSessionEntity>,
    ): List<ReplacementActionInsight> {
        data class ActionAttempt(val actionKey: String, val urgeDrop: Int)

        val attempts = rescues.flatMap { rescue ->
            val finalUrge = rescue.finalUrge ?: return@flatMap emptyList()
            rescue.actionKeys
                .map { it.trim().lowercase() }
                .filter(String::isNotBlank)
                .distinct()
                .map { action -> ActionAttempt(action, rescue.initialUrge - finalUrge) }
        }

        return attempts
            .groupBy(ActionAttempt::actionKey)
            .map { (actionKey, actionAttempts) ->
                val reduced = actionAttempts.count { it.urgeDrop > 0 }
                ReplacementActionInsight(
                    actionKey = actionKey,
                    attempts = actionAttempts.size,
                    reducedUrgeCount = reduced,
                    reducedUrgePercent = ((reduced.toDouble() / actionAttempts.size) * 100).roundToInt(),
                    averageUrgeDrop = actionAttempts.map(ActionAttempt::urgeDrop).average(),
                )
            }
            .sortedWith(
                compareByDescending<ReplacementActionInsight> { it.attempts }
                    .thenByDescending { it.reducedUrgePercent }
                    .thenByDescending { it.averageUrgeDrop }
                    .thenBy { it.actionKey },
            )
            .take(MAX_ACTION_INSIGHTS)
    }

    private enum class OutcomeDirection { MET, DIFFICULT }

    private fun TrackCheckInOutcome.direction(): OutcomeDirection? = when (this) {
        TrackCheckInOutcome.GOAL_MET -> OutcomeDirection.MET
        TrackCheckInOutcome.GOAL_NOT_MET,
        TrackCheckInOutcome.SLIP,
        -> OutcomeDirection.DIFFICULT
        TrackCheckInOutcome.GOAL_PARTLY_MET,
        TrackCheckInOutcome.AWARENESS_LOGGED,
        -> null
    }

    private fun TrackCheckInOutcome.isDifficult(): Boolean =
        this == TrackCheckInOutcome.GOAL_NOT_MET || this == TrackCheckInOutcome.SLIP

    private const val MIN_COMPARABLE_DAYS = 4
    private const val MIN_DOMINANT_DAYS = 3
    private const val MIN_DOMINANT_SHARE = 0.5
    private const val MAX_ACTION_INSIGHTS = 5
}
