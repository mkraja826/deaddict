package com.deaddict.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus

@Entity(
    tableName = "active_programs",
    indices = [Index(value = ["programId"], unique = true)],
)
data class ActiveProgramEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val activatedAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
    val syncState: SyncState,
)

@Entity(
    tableName = "recovery_tracks",
    indices = [
        Index("ownerKey"),
        Index("programId"),
        Index("role"),
        Index("status"),
        Index("updatedAtEpochMillis"),
        Index(value = ["ownerKey", "programId"]),
    ],
)
data class RecoveryTrackEntity(
    @PrimaryKey val id: String,
    val ownerKey: String,
    val programId: String,
    val displayAlias: String?,
    val role: RecoveryTrackRole,
    val status: RecoveryTrackStatus,
    val startedAtEpochMillis: Long,
    val pausedAtEpochMillis: Long?,
    val maintenanceAtEpochMillis: Long?,
    val archivedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(ownerKey.isNotBlank())
        require(programId.isNotBlank())
        require(displayAlias == null || displayAlias.isNotBlank())
        require(displayAlias == null || displayAlias.length <= 80)
        require(revision >= 0)
        require(updatedAtEpochMillis >= createdAtEpochMillis)
        require(role != RecoveryTrackRole.PRIMARY || status.isPrimaryEligible)
    }
}

@Entity(
    tableName = "recovery_goal_versions",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["recoveryTrackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("recoveryTrackId"),
        Index("effectiveFromEpochMillis"),
        Index("effectiveUntilEpochMillis"),
        Index(value = ["recoveryTrackId", "effectiveUntilEpochMillis"]),
    ],
)
data class RecoveryGoalVersionEntity(
    @PrimaryKey val id: String,
    val recoveryTrackId: String,
    val goalType: RecoveryGoalType,
    val targetValue: Double?,
    val unitKey: String?,
    val periodType: GoalPeriodType?,
    val title: String?,
    val effectiveFromEpochMillis: Long,
    val effectiveUntilEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(recoveryTrackId.isNotBlank())
        require(targetValue == null || targetValue.isFinite())
        require(targetValue == null || targetValue >= 0)
        require(targetValue == null || !unitKey.isNullOrBlank())
        require(title == null || title.isNotBlank())
        require(title == null || title.length <= 120)
        require(
            effectiveUntilEpochMillis == null ||
                effectiveUntilEpochMillis > effectiveFromEpochMillis,
        )
        require(updatedAtEpochMillis >= createdAtEpochMillis)
        require(revision >= 0)
    }
}

@Entity(
    tableName = "tracking_events",
    indices = [
        Index("programId"),
        Index("occurredAtEpochMillis"),
        Index("syncState"),
    ],
)
data class TrackingEventEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val kind: TrackingEventKind,
    val quantity: Double?,
    val unit: String?,
    val costMinorUnits: Long?,
    val urgeIntensity: Int?,
    val triggerKey: String?,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val privateNote: String?,
    val syncState: SyncState,
) {
    init {
        require(id.isNotBlank())
        require(programId.isNotBlank())
        require(quantity == null || quantity >= 0)
        require(costMinorUnits == null || costMinorUnits >= 0)
        require(urgeIntensity == null || urgeIntensity in 1..5)
    }
}

@Entity(
    tableName = "rescue_sessions",
    indices = [Index("programId"), Index("startedAtEpochMillis"), Index("syncState")],
)
data class RescueSessionEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val initialUrge: Int,
    val finalUrge: Int?,
    val triggerKey: String?,
    val actionKeys: List<String>,
    val outcome: RescueOutcome?,
    val syncState: SyncState,
) {
    init {
        require(initialUrge in 1..5)
        require(finalUrge == null || finalUrge in 1..5)
        require(actionKeys.size <= 10)
    }
}

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["state", "nextAttemptAtEpochMillis"]),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,
    val aggregateType: SyncAggregateType,
    val aggregateId: String,
    val operation: SyncOperation,
    val payload: String,
    val createdAtEpochMillis: Long,
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMillis: Long,
    val state: OutboxState = OutboxState.PENDING,
    val lastErrorCode: String? = null,
) {
    init {
        require(idempotencyKey.isNotBlank())
        require(attemptCount >= 0)
    }
}

enum class SyncState { LOCAL_ONLY, PENDING, SYNCED }
enum class OutboxState { PENDING, IN_FLIGHT, COMPLETED, DEAD_LETTER }
enum class SyncOperation { UPSERT, DELETE }
enum class SyncAggregateType {
    ACTIVE_PROGRAM,
    RECOVERY_TRACK,
    RECOVERY_GOAL,
    TRACKING_EVENT,
    RESCUE_SESSION,
}
enum class TrackingEventKind { ACTIVITY, URGE, CRAVING, SLIP, QUANTITY, TIME, COST }
enum class RescueOutcome { REDUCED, SAME, INCREASED, NOT_COMPLETED }

private val RecoveryTrackStatus.isPrimaryEligible: Boolean
    get() = this == RecoveryTrackStatus.ACTIVE || this == RecoveryTrackStatus.MAINTENANCE
