package com.deaddict.model

import java.time.Instant
import java.util.UUID

@JvmInline
value class RecoveryGoalVersionId private constructor(val value: String) {
    companion object {
        fun random(): RecoveryGoalVersionId = RecoveryGoalVersionId(UUID.randomUUID().toString())

        fun parse(value: String): RecoveryGoalVersionId {
            UUID.fromString(value)
            return RecoveryGoalVersionId(value)
        }
    }
}

enum class RecoveryGoalType {
    QUIT_COMPLETELY,
    REDUCE_QUANTITY,
    DAILY_LIMIT,
    WEEKLY_LIMIT,
    TIME_LIMIT,
    SPENDING_LIMIT,
    DELAY_FIRST_USE,
    NO_USE_PERIOD,
    AWARENESS_ONLY,
    CUSTOM,
}

enum class GoalPeriodType {
    DAY,
    WEEK,
    MONTH,
    SESSION,
}

data class RecoveryGoalVersion(
    val id: RecoveryGoalVersionId,
    val recoveryTrackId: RecoveryTrackId,
    val goalType: RecoveryGoalType,
    val targetValue: Double?,
    val unitKey: String?,
    val periodType: GoalPeriodType?,
    val title: String?,
    val effectiveFrom: Instant,
    val effectiveUntil: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val revision: Long = 0,
) {
    init {
        require(targetValue == null || targetValue.isFinite()) {
            "Target value must be finite"
        }
        require(targetValue == null || targetValue >= 0) {
            "Target value must not be negative"
        }
        require(targetValue == null || !unitKey.isNullOrBlank()) {
            "A target value requires a unit key"
        }
        require(title == null || title.isNotBlank()) {
            "Goal title must be null or non-blank"
        }
        require(title == null || title.length <= MAX_TITLE_LENGTH) {
            "Goal title must be at most $MAX_TITLE_LENGTH characters"
        }
        require(effectiveUntil == null || effectiveUntil.isAfter(effectiveFrom)) {
            "effectiveUntil must be later than effectiveFrom"
        }
        require(!updatedAt.isBefore(createdAt)) {
            "updatedAt must not be earlier than createdAt"
        }
        require(revision >= 0) { "Revision must not be negative" }
    }

    val isCurrent: Boolean get() = effectiveUntil == null

    fun close(
        at: Instant,
        updatedAt: Instant = at,
    ): RecoveryGoalVersion {
        require(isCurrent) { "Only the current goal version can be closed" }
        require(at.isAfter(effectiveFrom)) { "Goal must end after it became effective" }
        require(!updatedAt.isBefore(this.updatedAt)) {
            "Update time must not move backwards"
        }
        return copy(
            effectiveUntil = at,
            updatedAt = updatedAt,
            revision = revision + 1,
        )
    }

    companion object {
        const val MAX_TITLE_LENGTH = 120
    }
}
