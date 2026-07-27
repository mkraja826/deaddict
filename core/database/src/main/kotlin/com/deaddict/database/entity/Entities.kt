package com.deaddict.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
enum class SyncAggregateType { ACTIVE_PROGRAM, TRACKING_EVENT, RESCUE_SESSION }
enum class TrackingEventKind { ACTIVITY, URGE, CRAVING, SLIP, QUANTITY, TIME, COST }
enum class RescueOutcome { REDUCED, SAME, INCREASED, NOT_COMPLETED }
