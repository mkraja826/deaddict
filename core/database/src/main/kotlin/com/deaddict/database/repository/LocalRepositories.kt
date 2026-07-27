package com.deaddict.database.repository

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.OutboxState
import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.programs.ProgramId
import kotlinx.coroutines.flow.Flow
import java.util.UUID

fun interface EpochClock {
    fun nowMillis(): Long
}

fun interface IdGenerator {
    fun next(): String
}

enum class SyncPolicy {
    LOCAL_ONLY,
    CLOUD_ELIGIBLE,
}

data class NewTrackingEvent(
    val programId: ProgramId,
    val kind: TrackingEventKind,
    val quantity: Double? = null,
    val unit: String? = null,
    val costMinorUnits: Long? = null,
    val urgeIntensity: Int? = null,
    val triggerKey: String? = null,
    val occurredAtEpochMillis: Long,
    val privateNote: String? = null,
)

data class NewRescueSession(
    val programId: ProgramId,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val initialUrge: Int,
    val finalUrge: Int?,
    val triggerKey: String?,
    val actionKeys: List<String>,
    val outcome: RescueOutcome?,
)

class LocalProgramRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeActive(): Flow<List<ActiveProgramEntity>> = database.programDao().observeActive()

    suspend fun activate(programId: ProgramId, syncPolicy: SyncPolicy): String {
        val id = ids.next()
        val now = clock.nowMillis()
        val state = syncPolicy.toInitialSyncState()
        val entity = ActiveProgramEntity(id, programId.value, now, null, state)

        database.withTransaction {
            database.programDao().insert(entity)
            if (syncPolicy == SyncPolicy.CLOUD_ELIGIBLE) {
                database.syncOutboxDao().enqueue(
                    outbox(
                        id = ids.next(),
                        aggregateType = SyncAggregateType.ACTIVE_PROGRAM,
                        aggregateId = id,
                        operation = SyncOperation.UPSERT,
                        payload = """{"id":"$id","program_id":"${programId.value}","activated_at":$now}""",
                        now = now,
                    ),
                )
            }
        }
        return id
    }
}

class LocalTrackingRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeForProgram(programId: ProgramId): Flow<List<TrackingEventEntity>> =
        database.trackingDao().observeForProgram(programId.value)

    suspend fun record(input: NewTrackingEvent, syncPolicy: SyncPolicy): String {
        val id = ids.next()
        val now = clock.nowMillis()
        val state = syncPolicy.toInitialSyncState()
        val entity = TrackingEventEntity(
            id = id,
            programId = input.programId.value,
            kind = input.kind,
            quantity = input.quantity,
            unit = input.unit,
            costMinorUnits = input.costMinorUnits,
            urgeIntensity = input.urgeIntensity,
            triggerKey = input.triggerKey,
            occurredAtEpochMillis = input.occurredAtEpochMillis,
            createdAtEpochMillis = now,
            privateNote = input.privateNote,
            syncState = state,
        )

        database.withTransaction {
            database.trackingDao().insert(entity)
            if (syncPolicy == SyncPolicy.CLOUD_ELIGIBLE) {
                database.syncOutboxDao().enqueue(
                    outbox(
                        id = ids.next(),
                        aggregateType = SyncAggregateType.TRACKING_EVENT,
                        aggregateId = id,
                        operation = SyncOperation.UPSERT,
                        payload = trackingPayload(entity),
                        now = now,
                    ),
                )
            }
        }
        return id
    }
}

class LocalRescueRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeRecent(limit: Int = 20): Flow<List<RescueSessionEntity>> {
        require(limit in 1..100)
        return database.rescueDao().observeRecent(limit)
    }

    suspend fun record(input: NewRescueSession, syncPolicy: SyncPolicy): String {
        val id = ids.next()
        val now = clock.nowMillis()
        val entity = RescueSessionEntity(
            id = id,
            programId = input.programId.value,
            startedAtEpochMillis = input.startedAtEpochMillis,
            completedAtEpochMillis = input.completedAtEpochMillis,
            initialUrge = input.initialUrge,
            finalUrge = input.finalUrge,
            triggerKey = input.triggerKey,
            actionKeys = input.actionKeys,
            outcome = input.outcome,
            syncState = syncPolicy.toInitialSyncState(),
        )
        database.withTransaction {
            database.rescueDao().insert(entity)
            if (syncPolicy == SyncPolicy.CLOUD_ELIGIBLE) {
                database.syncOutboxDao().enqueue(
                    outbox(
                        id = ids.next(),
                        aggregateType = SyncAggregateType.RESCUE_SESSION,
                        aggregateId = id,
                        operation = SyncOperation.UPSERT,
                        payload = rescuePayload(entity),
                        now = now,
                    ),
                )
            }
        }
        return id
    }
}

private fun SyncPolicy.toInitialSyncState(): SyncState =
    if (this == SyncPolicy.LOCAL_ONLY) SyncState.LOCAL_ONLY else SyncState.PENDING

private fun outbox(
    id: String,
    aggregateType: SyncAggregateType,
    aggregateId: String,
    operation: SyncOperation,
    payload: String,
    now: Long,
) = SyncOutboxEntity(
    id = id,
    idempotencyKey = "${aggregateType.name}:$aggregateId:${operation.name}",
    aggregateType = aggregateType,
    aggregateId = aggregateId,
    operation = operation,
    payload = payload,
    createdAtEpochMillis = now,
    nextAttemptAtEpochMillis = now,
    state = OutboxState.PENDING,
)

// Private notes are deliberately absent from synchronized payloads.
private fun trackingPayload(event: TrackingEventEntity): String =
    """{"id":"${event.id}","program_id":"${event.programId}","kind":"${event.kind.name}","occurred_at":${event.occurredAtEpochMillis}}"""

private fun rescuePayload(session: RescueSessionEntity): String =
    """{"id":"${session.id}","program_id":"${session.programId}","started_at":${session.startedAtEpochMillis},"initial_urge":${session.initialUrge}}"""
