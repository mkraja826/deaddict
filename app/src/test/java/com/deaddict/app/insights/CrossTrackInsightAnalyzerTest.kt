package com.deaddict.app.insights

import com.deaddict.database.dao.CrossTrackOutcomeRow
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class CrossTrackInsightAnalyzerTest {
    @Test
    fun repeatedSelectedMetAndOtherDifficultIsLabeledAsPossibleShift() {
        val rows = listOf(
            row(1, TrackCheckInOutcome.GOAL_MET, TrackCheckInOutcome.SLIP),
            row(2, TrackCheckInOutcome.GOAL_MET, TrackCheckInOutcome.GOAL_NOT_MET),
            row(3, TrackCheckInOutcome.GOAL_MET, TrackCheckInOutcome.SLIP),
            row(4, TrackCheckInOutcome.GOAL_MET, TrackCheckInOutcome.GOAL_MET),
            row(5, TrackCheckInOutcome.GOAL_NOT_MET, TrackCheckInOutcome.GOAL_NOT_MET),
        )

        val summary = checkNotNull(CrossTrackInsightAnalyzer.analyze(rows, emptyList()))
        val pair = summary.pairings.single()

        assertEquals(CrossTrackPattern.POSSIBLE_SHIFT_TOWARD_OTHER, pair.pattern)
        assertEquals(5, pair.comparableDays)
        assertEquals(3, pair.selectedMetOtherDifficultDays)
        assertEquals(60, pair.dominantPatternPercent)
        assertEquals(1, summary.sharedDifficultDays)
        assertEquals(3, summary.possibleShiftDays)
    }

    @Test
    fun partialAndAwarenessOutcomesDoNotCreateDirectionalClaims() {
        val rows = listOf(
            row(1, TrackCheckInOutcome.GOAL_PARTLY_MET, TrackCheckInOutcome.GOAL_NOT_MET),
            row(2, TrackCheckInOutcome.AWARENESS_LOGGED, TrackCheckInOutcome.GOAL_MET),
            row(3, TrackCheckInOutcome.GOAL_MET, TrackCheckInOutcome.GOAL_MET),
            row(4, TrackCheckInOutcome.SLIP, TrackCheckInOutcome.GOAL_NOT_MET),
        )

        val pair = checkNotNull(CrossTrackInsightAnalyzer.analyze(rows, emptyList()))
            .pairings
            .single()

        assertEquals(2, pair.comparableDays)
        assertEquals(CrossTrackPattern.NOT_ENOUGH_DATA, pair.pattern)
    }

    @Test
    fun rescueActionsAggregateObservedUrgeReduction() {
        val rescues = listOf(
            rescue(
                id = "00000000-0000-0000-0000-000000000501",
                initial = 5,
                final = 3,
                actions = listOf("walk", "walk"),
            ),
            rescue(
                id = "00000000-0000-0000-0000-000000000502",
                initial = 4,
                final = 4,
                actions = listOf("walk"),
            ),
            rescue(
                id = "00000000-0000-0000-0000-000000000503",
                initial = 4,
                final = 2,
                actions = listOf("slow_breathing"),
            ),
        )

        val actions = checkNotNull(CrossTrackInsightAnalyzer.analyze(emptyList(), rescues))
            .replacementActions

        assertEquals("walk", actions.first().actionKey)
        assertEquals(2, actions.first().attempts)
        assertEquals(1, actions.first().reducedUrgeCount)
        assertEquals(50, actions.first().reducedUrgePercent)
        assertEquals(1.0, actions.first().averageUrgeDrop, 0.001)
        assertEquals("slow_breathing", actions[1].actionKey)
        assertEquals(100, actions[1].reducedUrgePercent)
    }

    private fun row(
        day: Long,
        selected: TrackCheckInOutcome,
        other: TrackCheckInOutcome,
    ) = CrossTrackOutcomeRow(
        localDateEpochDay = day,
        mood = 3,
        stress = if (day == 5L) 5 else 3,
        energy = 3,
        sleepQuality = if (day == 5L) 2 else 4,
        selectedOutcome = selected,
        otherTrackId = OTHER_TRACK_ID,
        otherOutcome = other,
    )

    private fun rescue(
        id: String,
        initial: Int,
        final: Int,
        actions: List<String>,
    ) = RescueSessionEntity(
        id = id,
        ownerKey = "guest:test-profile",
        recoveryTrackId = SELECTED_TRACK_ID,
        programId = "gaming",
        startedAtEpochMillis = 1_000L,
        completedAtEpochMillis = 2_000L,
        initialUrge = initial,
        finalUrge = final,
        triggerKey = "stress",
        actionKeys = actions,
        outcome = null,
        syncState = SyncState.LOCAL_ONLY,
    )

    private companion object {
        const val SELECTED_TRACK_ID = "00000000-0000-0000-0000-000000000510"
        const val OTHER_TRACK_ID = "00000000-0000-0000-0000-000000000511"
    }
}
