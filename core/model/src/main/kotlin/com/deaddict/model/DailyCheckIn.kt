package com.deaddict.model

import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@JvmInline
value class DailyCheckInId private constructor(val value: String) {
    companion object {
        fun new(): DailyCheckInId = DailyCheckInId(UUID.randomUUID().toString())
        fun parse(value: String): DailyCheckInId {
            UUID.fromString(value)
            return DailyCheckInId(value)
        }
    }
}

@JvmInline
value class TrackCheckInEntryId private constructor(val value: String) {
    companion object {
        fun new(): TrackCheckInEntryId = TrackCheckInEntryId(UUID.randomUUID().toString())
        fun parse(value: String): TrackCheckInEntryId {
            UUID.fromString(value)
            return TrackCheckInEntryId(value)
        }
    }
}

enum class TrackCheckInOutcome {
    ALIGNED,
    URGE_MANAGED,
    LIMIT_EXCEEDED,
    SLIP,
    OBSERVED,
    NOT_TRACKED,
}

data class DailyCheckIn(
    val id: DailyCheckInId,
    val ownerKey: OwnerKey,
    val localDate: LocalDate,
    val timezoneId: String,
    val mood: Int? = null,
    val stress: Int? = null,
    val energy: Int? = null,
    val sleepQuality: Int? = null,
    val triggerKeys: Set<String> = emptySet(),
    val privateNote: String? = null,
    val completedAtEpochMillis: Long,
) {
    init {
        ZoneId.of(timezoneId)
        require(mood == null || mood in 1..5) { "Mood must be between 1 and 5" }
        require(stress == null || stress in 1..5) { "Stress must be between 1 and 5" }
        require(energy == null || energy in 1..5) { "Energy must be between 1 and 5" }
        require(sleepQuality == null || sleepQuality in 1..5) { "Sleep quality must be between 1 and 5" }
        require(completedAtEpochMillis > 0) { "Completion time must be positive" }
    }
}

data class TrackCheckInEntry(
    val id: TrackCheckInEntryId,
    val dailyCheckInId: DailyCheckInId,
    val recoveryTrackId: RecoveryTrackId,
    val goalVersionId: RecoveryGoalVersionId,
    val outcome: TrackCheckInOutcome,
    val urgeIntensity: Int? = null,
    val quantity: Double? = null,
    val durationMinutes: Long? = null,
    val costMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val triggerKeys: Set<String> = emptySet(),
    val privateNote: String? = null,
) {
    init {
        require(urgeIntensity == null || urgeIntensity in 1..5) { "Urge intensity must be between 1 and 5" }
        require(quantity == null || quantity >= 0.0) { "Quantity cannot be negative" }
        require(durationMinutes == null || durationMinutes >= 0) { "Duration cannot be negative" }
        require(costMinorUnits == null || costMinorUnits >= 0) { "Cost cannot be negative" }
        require((costMinorUnits == null) == (currencyCode == null)) {
            "Cost and currency must be provided together"
        }
        require(currencyCode == null || currencyCode.matches(Regex("[A-Z]{3}"))) {
            "Currency must be an ISO 4217 code"
        }
    }
}

data class TrackCheckInAnswer(
    val slipped: Boolean = false,
    val urgeManaged: Boolean = false,
    val explicitlyTracked: Boolean = true,
    val quantity: Double? = null,
    val durationMinutes: Long? = null,
    val costMinorUnits: Long? = null,
)

object TrackCheckInOutcomeResolver {
    fun resolve(
        goal: RecoveryGoalVersion,
        answer: TrackCheckInAnswer,
    ): TrackCheckInOutcome {
        if (!answer.explicitlyTracked) return TrackCheckInOutcome.NOT_TRACKED
        if (answer.slipped) return TrackCheckInOutcome.SLIP
        if (answer.urgeManaged) return TrackCheckInOutcome.URGE_MANAGED

        return when (goal.goalType) {
            RecoveryGoalType.AWARENESS_ONLY,
            RecoveryGoalType.CUSTOM,
            -> TrackCheckInOutcome.OBSERVED

            RecoveryGoalType.QUIT,
            RecoveryGoalType.NO_USE_PERIOD,
            -> TrackCheckInOutcome.ALIGNED

            RecoveryGoalType.REDUCE_QUANTITY,
            RecoveryGoalType.DAILY_LIMIT,
            RecoveryGoalType.WEEKLY_LIMIT,
            -> resolveNumericLimit(goal.targetValue, answer.quantity)

            RecoveryGoalType.TIME_LIMIT -> resolveNumericLimit(
                goal.targetValue,
                answer.durationMinutes?.toDouble(),
            )

            RecoveryGoalType.SPENDING_LIMIT -> resolveNumericLimit(
                goal.targetValue,
                answer.costMinorUnits?.toDouble(),
            )

            RecoveryGoalType.DELAY_FIRST_USE -> {
                val target = goal.targetValue
                val actual = answer.durationMinutes?.toDouble()
                if (target == null || actual == null) TrackCheckInOutcome.OBSERVED
                else if (actual >= target) TrackCheckInOutcome.ALIGNED
                else TrackCheckInOutcome.LIMIT_EXCEEDED
            }
        }
    }

    private fun resolveNumericLimit(target: Double?, actual: Double?): TrackCheckInOutcome {
        if (target == null || actual == null) return TrackCheckInOutcome.OBSERVED
        return if (actual <= target) {
            TrackCheckInOutcome.ALIGNED
        } else {
            TrackCheckInOutcome.LIMIT_EXCEEDED
        }
    }
}
