package com.deaddict.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@JvmInline
value class DailyCheckInId private constructor(val value: String) {
    companion object {
        fun random(): DailyCheckInId = DailyCheckInId(UUID.randomUUID().toString())

        fun parse(value: String): DailyCheckInId {
            UUID.fromString(value)
            return DailyCheckInId(value)
        }
    }
}

@JvmInline
value class TrackCheckInEntryId private constructor(val value: String) {
    companion object {
        fun random(): TrackCheckInEntryId = TrackCheckInEntryId(UUID.randomUUID().toString())

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
    val mood: Int?,
    val stress: Int?,
    val energy: Int?,
    val sleep: Int?,
    val triggerKeys: Set<String>,
    val privateNote: String?,
    val completedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        ZoneId.of(timezoneId)
        validateRating("Mood", mood)
        validateRating("Stress", stress)
        validateRating("Energy", energy)
        validateRating("Sleep", sleep)
        require(triggerKeys.size <= MAX_TRIGGER_COUNT) {
            "A daily check-in can contain at most $MAX_TRIGGER_COUNT triggers"
        }
        triggerKeys.forEach { require(it.isNotBlank()) { "Trigger keys must not be blank" } }
        require(privateNote == null || privateNote.length <= MAX_PRIVATE_NOTE_LENGTH) {
            "Private note must be at most $MAX_PRIVATE_NOTE_LENGTH characters"
        }
        require(!completedAt.isBefore(createdAt)) {
            "completedAt must not be earlier than createdAt"
        }
        require(!updatedAt.isBefore(createdAt)) {
            "updatedAt must not be earlier than createdAt"
        }
    }

    companion object {
        const val MAX_TRIGGER_COUNT = 20
        const val MAX_PRIVATE_NOTE_LENGTH = 2_000

        private fun validateRating(label: String, value: Int?) {
            require(value == null || value in 1..5) { "$label must be between 1 and 5" }
        }
    }
}

data class TrackCheckInEntry(
    val id: TrackCheckInEntryId,
    val dailyCheckInId: DailyCheckInId,
    val ownerKey: OwnerKey,
    val recoveryTrackId: RecoveryTrackId,
    val recoveryGoalVersionId: RecoveryGoalVersionId?,
    val outcome: TrackCheckInOutcome,
    val urgeIntensity: Int?,
    val quantity: Double?,
    val quantityUnit: String?,
    val durationMinutes: Long?,
    val costMinorUnits: Long?,
    val currencyCode: String?,
    val triggerKeys: Set<String>,
    val privateNote: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(urgeIntensity == null || urgeIntensity in 1..5) {
            "Urge intensity must be between 1 and 5"
        }
        require(quantity == null || quantity.isFinite()) { "Quantity must be finite" }
        require(quantity == null || quantity >= 0) { "Quantity must not be negative" }
        require((quantity == null) == (quantityUnit == null)) {
            "Quantity and quantity unit must be supplied together"
        }
        require(quantityUnit == null || quantityUnit.isNotBlank()) {
            "Quantity unit must not be blank"
        }
        require(durationMinutes == null || durationMinutes >= 0) {
            "Duration must not be negative"
        }
        require(costMinorUnits == null || costMinorUnits >= 0) {
            "Cost must not be negative"
        }
        require((costMinorUnits == null) == (currencyCode == null)) {
            "Cost and currency code must be supplied together"
        }
        require(currencyCode == null || CURRENCY_CODE.matches(currencyCode)) {
            "Currency code must be a three-letter ISO-style code"
        }
        require(triggerKeys.size <= DailyCheckIn.MAX_TRIGGER_COUNT)
        triggerKeys.forEach { require(it.isNotBlank()) { "Trigger keys must not be blank" } }
        require(privateNote == null || privateNote.length <= DailyCheckIn.MAX_PRIVATE_NOTE_LENGTH)
        require(!updatedAt.isBefore(createdAt)) {
            "updatedAt must not be earlier than createdAt"
        }
    }

    val hasMeasurement: Boolean
        get() = quantity != null || durationMinutes != null || costMinorUnits != null

    companion object {
        private val CURRENCY_CODE = Regex("[A-Z]{3}")
    }
}
