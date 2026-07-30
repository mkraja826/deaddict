package com.deaddict.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.deaddict.database.DailyCheckInConverters
import com.deaddict.model.RecoveryTrackStatus

@Entity(
    tableName = "daily_check_ins",
    indices = [
        Index("ownerKey"),
        Index("localDate"),
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
    val sleepQuality: Int?,
    val sharedTriggerKeys: List<String>,
    val privateNote: String?,
    val completedAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(ownerKey.isNotBlank())
        require(localDate.matches(ISO_LOCAL_DATE)) { "localDate must use ISO-8601 yyyy-MM-dd" }
        require(timezoneId.isNotBlank())
        require(mood == null || mood in 1..5)
        require(stress == null || stress in 1..5)
        require(energy == null || energy in 1..5)
        require(sleepQuality == null || sleepQuality in 1..5)
        require(sharedTriggerKeys.size <= MAX_TRIGGER_COUNT)
        require(sharedTriggerKeys.none(String::isBlank))
        require(privateNote == null || privateNote.length <= MAX_PRIVATE_NOTE_LENGTH)
        require(completedAtEpochMillis > 0)
        require(updatedAtEpochMillis >= createdAtEpochMillis)
        require(revision >= 0)
    }

    private companion object {
        val ISO_LOCAL_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        const val MAX_TRIGGER_COUNT = 20
        const val MAX_PRIVATE_NOTE_LENGTH = 4_000
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
        Index("ownerKey"),
        Index("recoveryTrackId"),
        Index("programId"),
        Index("goalVersionId"),
        Index("outcome"),
        Index("syncState"),
        Index(value = ["dailyCheckInId", "recoveryTrackId"], unique = true),
    ],
)
@TypeConverters(DailyCheckInConverters::class)
data class TrackCheckInEntryEntity(
    @PrimaryKey val id: String,
    val dailyCheckInId: String,
    val ownerKey: String,
    val recoveryTrackId: String,
    val programId: String,
    val goalVersionId: String?,
    val outcome: TrackCheckInOutcome,
    val quantity: Double?,
    val unit: String?,
    val durationMinutes: Long?,
    val costMinorUnits: Long?,
    val urgeIntensity: Int?,
    val triggerKeys: List<String>,
    val privateNote: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(dailyCheckInId.isNotBlank())
        require(ownerKey.isNotBlank())
        require(recoveryTrackId.isNotBlank())
        require(programId.isNotBlank())
        require(goalVersionId == null || goalVersionId.isNotBlank())
        require(quantity == null || quantity.isFinite() && quantity >= 0)
        require(unit == null || unit.isNotBlank())
        require(quantity == null || unit != null)
        require(durationMinutes == null || durationMinutes >= 0)
        require(costMinorUnits == null || costMinorUnits >= 0)
        require(urgeIntensity == null || urgeIntensity in 1..5)
        require(triggerKeys.size <= MAX_TRIGGER_COUNT)
        require(triggerKeys.none(String::isBlank))
        require(privateNote == null || privateNote.length <= MAX_PRIVATE_NOTE_LENGTH)
        require(updatedAtEpochMillis >= createdAtEpochMillis)
        require(revision >= 0)
    }

    private companion object {
        const val MAX_TRIGGER_COUNT = 20
        const val MAX_PRIVATE_NOTE_LENGTH = 4_000
    }
}

@Entity(
    tableName = "daily_check_in_drafts",
    primaryKeys = ["ownerKey", "localDate"],
    indices = [Index("updatedAtEpochMillis")],
)
data class DailyCheckInDraftEntity(
    val ownerKey: String,
    val localDate: String,
    val timezoneId: String,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(ownerKey.isNotBlank())
        require(localDate.matches(ISO_LOCAL_DATE)) { "localDate must use ISO-8601 yyyy-MM-dd" }
        require(timezoneId.isNotBlank())
        require(payloadJson.length <= MAX_DRAFT_LENGTH)
        require(updatedAtEpochMillis > 0)
    }

    private companion object {
        val ISO_LOCAL_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        const val MAX_DRAFT_LENGTH = 64_000
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

internal val RecoveryTrackStatus.acceptsCheckIns: Boolean
    get() = this == RecoveryTrackStatus.ACTIVE || this == RecoveryTrackStatus.MAINTENANCE
