package com.deaddict.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_check_ins",
    indices = [
        Index("ownerKey"),
        Index("localDateEpochDay"),
        Index("updatedAtEpochMillis"),
        Index(value = ["ownerKey", "localDateEpochDay"], unique = true),
    ],
)
data class DailyCheckInEntity(
    @PrimaryKey val id: String,
    val ownerKey: String,
    val localDateEpochDay: Long,
    val mood: Int?,
    val stress: Int?,
    val energy: Int?,
    val sleepQuality: Int?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(ownerKey.isNotBlank())
        require(localDateEpochDay >= 0)
        require(mood == null || mood in CONTEXT_RANGE)
        require(stress == null || stress in CONTEXT_RANGE)
        require(energy == null || energy in CONTEXT_RANGE)
        require(sleepQuality == null || sleepQuality in CONTEXT_RANGE)
        require(updatedAtEpochMillis >= createdAtEpochMillis)
        require(revision >= 0)
    }

    private companion object {
        val CONTEXT_RANGE = 1..5
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
        ForeignKey(
            entity = RecoveryGoalVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalVersionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("dailyCheckInId"),
        Index("recoveryTrackId"),
        Index("goalVersionId"),
        Index("updatedAtEpochMillis"),
        Index(value = ["dailyCheckInId", "recoveryTrackId"], unique = true),
    ],
)
data class TrackCheckInEntryEntity(
    @PrimaryKey val id: String,
    val dailyCheckInId: String,
    val recoveryTrackId: String,
    val goalVersionId: String?,
    val outcome: TrackCheckInOutcome,
    val measuredValue: Double?,
    val unitKey: String?,
    val peakUrge: Int?,
    val privateNote: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(dailyCheckInId.isNotBlank())
        require(recoveryTrackId.isNotBlank())
        require(goalVersionId == null || goalVersionId.isNotBlank())
        require(measuredValue == null || measuredValue.isFinite())
        require(measuredValue == null || measuredValue >= 0)
        require((measuredValue == null) == unitKey.isNullOrBlank()) {
            "Measured values and units must be supplied together"
        }
        require(peakUrge == null || peakUrge in 1..5)
        require(privateNote == null || privateNote.length <= 2_000)
        require(updatedAtEpochMillis >= createdAtEpochMillis)
        require(revision >= 0)
    }
}

enum class TrackCheckInOutcome {
    GOAL_MET,
    GOAL_PARTLY_MET,
    GOAL_NOT_MET,
    SLIP,
    AWARENESS_LOGGED,
}
