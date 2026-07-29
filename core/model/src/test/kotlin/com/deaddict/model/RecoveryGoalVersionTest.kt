package com.deaddict.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryGoalVersionTest {
    private val createdAt = Instant.parse("2026-07-29T12:00:00Z")

    @Test
    fun `current awareness goal can omit numeric target`() {
        val goal = goal(
            goalType = RecoveryGoalType.AWARENESS_ONLY,
            targetValue = null,
            unitKey = null,
        )

        assertTrue(goal.isCurrent)
    }

    @Test
    fun `numeric target requires a unit`() {
        assertFails {
            goal(
                goalType = RecoveryGoalType.DAILY_LIMIT,
                targetValue = 5.0,
                unitKey = null,
            )
        }
    }

    @Test
    fun `closing current goal preserves history and increments revision`() {
        val end = createdAt.plusSeconds(3_600)

        val closed = goal().close(end)

        assertFalse(closed.isCurrent)
        assertEquals(end, closed.effectiveUntil)
        assertEquals(1L, closed.revision)
    }

    @Test
    fun `closed goal cannot be closed twice`() {
        val closed = goal().close(createdAt.plusSeconds(3_600))

        assertFails { closed.close(createdAt.plusSeconds(7_200)) }
    }

    @Test
    fun `goal version id rejects non uuid input`() {
        assertFails { RecoveryGoalVersionId.parse("invalid") }
    }

    private fun goal(
        goalType: RecoveryGoalType = RecoveryGoalType.DAILY_LIMIT,
        targetValue: Double? = 5.0,
        unitKey: String? = "count",
    ): RecoveryGoalVersion = RecoveryGoalVersion(
        id = RecoveryGoalVersionId.parse("84f15c61-39b7-47ca-8b45-50fe1cd76858"),
        recoveryTrackId = RecoveryTrackId.parse("7ebdbd0b-4676-45f1-82cd-e632b3ec6092"),
        goalType = goalType,
        targetValue = targetValue,
        unitKey = unitKey,
        periodType = GoalPeriodType.DAY,
        title = null,
        effectiveFrom = createdAt,
        effectiveUntil = null,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun assertFails(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }
}
