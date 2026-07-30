package com.deaddict.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deaddict.model.DailyCheckIn
import com.deaddict.model.TrackCheckInOutcome

@Entity(
    tableName = "daily_check_ins",
    indices = [
        Index("ownerKey"),
        Index("completedAtEpochMillis"),
        Index("syncState"),
        Index(value = ["ownerKey", "localDate"], unique = true),
    ],
)
data class DailyCheckInEntity(
    @PrimaryKey val id: String,
    val ownerKey: String,
    val localDate: String,
    val timezoneId: String,
    val mood: Int?,
    val stress: Int?,
    val energy: Int?,
    val sleep: Int?,
    val triggerKeys: List<String>,
    val privateNote: String?,
    val completedAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(ownerKey.isNotBlank())
        require(localDate.isNotBlank())
        require(timezoneId.isNotBlank())
        listOfNotNull(mood, stress, energy, sleep).forEach { require(it in 1..5) }
        require(triggerKeys.size <= DailyCheckIn.MAX_TRIGGER_COUNT)
        triggerKeys.forEach { require(it.isNotBlank()) }
        require(privateNote == null || privateNote.length <= DailyCheckIn.MAX_PRIVATE_NOTE_LENGTH)
        require(completedAtEpochMillis >= createdAtEpochMillis)
        require(updatedAtEpochMillis >= createdAtEpochMillis)
    }
}

@Entity(
    tableName = "track_check_in_entries",
    foreignKeys = [
        ForeignKey(
            entity = DailyCheckInEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyCheckInId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecoveryTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["recoveryTrackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("ownerKey"),
        Index("dailyCheckInId"),
        Index("recoveryTrackId"),
        Index("recoveryGoalVersionId"),
        Index("outcome"),
        Index("syncState"),
        Index(value = ["dailyCheckInId", "recoveryTrackId"], unique = true),
        Index(value = ["recoveryTrackId", "createdAtEpochMillis"]),
    ],
)
data class TrackCheckInEntryEntity(
    @PrimaryKey val id: String,
    val dailyCheckInId: String,
    val ownerKey: String,
    val recoveryTrackId: String,
    val recoveryGoalVersionId: String?,
    val outcome: TrackCheckInOutcome,
    val urgeIntensity: Int?,
    val quantity: Double?,
    val quantityUnit: String?,
    val durationMinutes: Long?,
    val costMinorUnits: Long?,
    val currencyCode: String?,
    val triggerKeys: List<String>,
    val privateNote: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(dailyCheckInId.isNotBlank())
        require(ownerKey.isNotBlank())
        require(recoveryTrackId.isNotBlank())
        require(recoveryGoalVersionId == null || recoveryGoalVersionId.isNotBlank())
        require(urgeIntensity == null || urgeIntensity in 1..5)
        require(quantity == null || quantity.isFinite())
        require(quantity == null || quantity >= 0)
        require((quantity == null) == (quantityUnit == null))
        require(quantityUnit == null || quantityUnit.isNotBlank())
        require(durationMinutes == null || durationMinutes >= 0)
        require(costMinorUnits == null || costMinorUnits >= 0)
        require((costMinorUnits == null) == (currencyCode == null))
        require(currencyCode == null || Regex("[A-Z]{3}").matches(currencyCode))
        require(triggerKeys.size <= DailyCheckIn.MAX_TRIGGER_COUNT)
        triggerKeys.forEach { require(it.isNotBlank()) }
        require(privateNote == null || privateNote.length <= DailyCheckIn.MAX_PRIVATE_NOTE_LENGTH)
        require(updatedAtEpochMillis >= createdAtEpochMillis)
    }
}

@Entity(
    tableName = "daily_check_in_drafts",
    indices = [Index("localDate")],
)
data class DailyCheckInDraftEntity(
    @PrimaryKey val ownerKey: String,
    val localDate: String,
    val timezoneId: String,
    val mood: Int?,
    val stress: Int?,
    val energy: Int?,
    val sleep: Int?,
    val triggerKeys: List<String>,
    val privateNote: String?,
    val trackEntriesPayload: String,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(ownerKey.isNotBlank())
        require(localDate.isNotBlank())
        require(timezoneId.isNotBlank())
        listOfNotNull(mood, stress, energy, sleep).forEach { require(it in 1..5) }
        require(triggerKeys.size <= DailyCheckIn.MAX_TRIGGER_COUNT)
        triggerKeys.forEach { require(it.isNotBlank()) }
        require(privateNote == null || privateNote.length <= DailyCheckIn.MAX_PRIVATE_NOTE_LENGTH)
        require(trackEntriesPayload.length <= MAX_TRACK_PAYLOAD_LENGTH)
        require(updatedAtEpochMillis > 0)
    }

    companion object {
        const val MAX_TRACK_PAYLOAD_LENGTH = 100_000
    }
}
