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
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

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

data class RecoveryOwnerSelection(
    val ownerKey: OwnerKey,
    val recoveryTrackId: RecoveryTrackId,
)

fun interface RecoveryOwnerContext {
    suspend fun current(): RecoveryOwnerSelection?
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
    val ownerKey: OwnerKey? = null,
    val recoveryTrackId: RecoveryTrackId? = null,
) {
    init {
        require((ownerKey == null) == (recoveryTrackId == null)) {
            "Owner and Recovery Track identity must be supplied together"
        }
    }
}

data class NewRescueSession(
    val programId: ProgramId,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
    val initialUrge: Int,
    val finalUrge: Int?,
    val triggerKey: String?,
    val actionKeys: List<String>,
    val outcome: RescueOutcome?,
    val ownerKey: OwnerKey? = null,
    val recoveryTrackId: RecoveryTrackId? = null,
) {
    init {
        require((ownerKey == null) == (recoveryTrackId == null)) {
            "Owner and Recovery Track identity must be supplied together"
        }
    }
}

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

    suspend fun delete(id: String): Boolean = database.withTransaction {
        val entity = database.programDao().byId(id) ?: return@withTransaction false
        if (entity.syncState != SyncState.LOCAL_ONLY) {
            database.enqueueDelete(SyncAggregateType.ACTIVE_PROGRAM, id, clock.nowMillis(), ids)
        }
        check(database.programDao().deleteById(id) == 1)
        true
    }
}

class LocalTrackingRepository(
    private val database: DeAddictDatabase,
    private val ownerContext: RecoveryOwnerContext? = null,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeForTrack(
        recoveryTrackId: RecoveryTrackId,
        limit: Int = DEFAULT_TRACKING_OBSERVATION_LIMIT,
    ): Flow<List<TrackingEventEntity>> {
        require(limit in 1..MAX_TRACKING_OBSERVATION_LIMIT)
        return database.trackingDao().observeForTrack(recoveryTrackId.value, limit)
    }

    @Deprecated("Use observeForTrack so repeated journeys remain independent")
    fun observeForProgram(
        programId: ProgramId,
        limit: Int = DEFAULT_TRACKING_OBSERVATION_LIMIT,
    ): Flow<List<TrackingEventEntity>> {
        require(limit in 1..MAX_TRACKING_OBSERVATION_LIMIT)
        return database.trackingDao().observeForProgram(programId.value, limit)
    }

    suspend fun record(input: NewTrackingEvent, syncPolicy: SyncPolicy): String {
        val id = ids.next()
        val now = clock.nowMillis()
        val state = syncPolicy.toInitialSyncState()
        val selected = input.explicitSelection() ?: ownerContext?.current()

        database.withTransaction {
            val ownership = database.resolveRecoveryTrackOwnership(
                selection = selected,
                programId = input.programId,
            )
            val entity = TrackingEventEntity(
                id = id,
                ownerKey = ownership.ownerKey,
                recoveryTrackId = ownership.recoveryTrackId,
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
            database.trackingDao().insert(entity)
            if (syncPolicy == SyncPolicy.CLOUD_ELIGIBLE) {
                require(ownership.recoveryTrackId != null) {
                    "Cloud-eligible tracking events require a Recovery Track"
                }
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

    suspend fun delete(id: String): Boolean = database.withTransaction {
        val entity = database.trackingDao().byId(id) ?: return@withTransaction false
        if (entity.syncState != SyncState.LOCAL_ONLY) {
            database.enqueueDelete(SyncAggregateType.TRACKING_EVENT, id, clock.nowMillis(), ids)
        }
        check(database.trackingDao().deleteById(id) == 1)
        true
    }

    private companion object {
        const val DEFAULT_TRACKING_OBSERVATION_LIMIT = 500
        const val MAX_TRACKING_OBSERVATION_LIMIT = 2_000
    }
}

class LocalRescueRepository(
    private val database: DeAddictDatabase,
    private val ownerContext: RecoveryOwnerContext? = null,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeRecent(limit: Int = 20): Flow<List<RescueSessionEntity>> {
        require(limit in 1..100)
        return database.rescueDao().observeRecent(limit)
    }

    fun observeForTrack(
        recoveryTrackId: RecoveryTrackId,
        limit: Int = 20,
    ): Flow<List<RescueSessionEntity>> {
        require(limit in 1..100)
        return database.rescueDao().observeForTrack(recoveryTrackId.value, limit)
    }

    suspend fun record(input: NewRescueSession, syncPolicy: SyncPolicy): String {
        val id = ids.next()
        val now = clock.nowMillis()
        val selected = input.explicitSelection() ?: ownerContext?.current()
        database.withTransaction {
            val ownership = database.resolveRecoveryTrackOwnership(
                selection = selected,
                programId = input.programId,
            )
            val entity = RescueSessionEntity(
                id = id,
                ownerKey = ownership.ownerKey,
                recoveryTrackId = ownership.recoveryTrackId,
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
            database.rescueDao().insert(entity)
            if (syncPolicy == SyncPolicy.CLOUD_ELIGIBLE) {
                require(ownership.recoveryTrackId != null) {
                    "Cloud-eligible Rescue sessions require a Recovery Track"
                }
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

    suspend fun delete(id: String): Boolean = database.withTransaction {
        val entity = database.rescueDao().byId(id) ?: return@withTransaction false
        if (entity.syncState != SyncState.LOCAL_ONLY) {
            database.enqueueDelete(SyncAggregateType.RESCUE_SESSION, id, clock.nowMillis(), ids)
        }
        check(database.rescueDao().deleteById(id) == 1)
        true
    }
}

private fun NewTrackingEvent.explicitSelection(): RecoveryOwnerSelection? =
    ownerKey?.let { owner -> RecoveryOwnerSelection(owner, checkNotNull(recoveryTrackId)) }

private fun NewRescueSession.explicitSelection(): RecoveryOwnerSelection? =
    ownerKey?.let { owner -> RecoveryOwnerSelection(owner, checkNotNull(recoveryTrackId)) }

private data class RecoveryTrackOwnership(
    val ownerKey: String,
    val recoveryTrackId: String?,
)

private suspend fun DeAddictDatabase.resolveRecoveryTrackOwnership(
    selection: RecoveryOwnerSelection?,
    programId: ProgramId,
): RecoveryTrackOwnership {
    if (selection == null) return RecoveryTrackOwnership(LEGACY_OWNER_KEY, null)

    val track = checkNotNull(recoveryTrackDao().byId(selection.recoveryTrackId.value)) {
        "Recovery Track does not exist"
    }
    require(track.ownerKey == selection.ownerKey.value) {
        "Recovery Track belongs to another owner"
    }
    require(track.programId == programId.value) {
        "Recovery Track program does not match the event"
    }
    require(track.status in setOf(RecoveryTrackStatus.ACTIVE, RecoveryTrackStatus.MAINTENANCE)) {
        "Only active or maintenance Recovery Tracks can receive new records"
    }
    return RecoveryTrackOwnership(selection.ownerKey.value, track.id)
}

private suspend fun DeAddictDatabase.enqueueDelete(
    aggregateType: SyncAggregateType,
    aggregateId: String,
    now: Long,
    ids: IdGenerator,
) {
    syncOutboxDao().supersedePendingUpsert(aggregateType.name, aggregateId)
    val inserted = syncOutboxDao().enqueue(
        outbox(
            id = ids.next(),
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            operation = SyncOperation.DELETE,
            payload = """{"id":"$aggregateId"}""",
            now = now,
        ),
    )
    check(inserted != -1L) { "Delete operation is already queued" }
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
    """{"id":"${event.id}","recovery_track_id":${event.recoveryTrackId.jsonStringOrNull()},"program_id":"${event.programId}","kind":"${event.kind.name}","occurred_at":${event.occurredAtEpochMillis}}"""

private fun rescuePayload(session: RescueSessionEntity): String =
    """{"id":"${session.id}","recovery_track_id":${session.recoveryTrackId.jsonStringOrNull()},"program_id":"${session.programId}","started_at":${session.startedAtEpochMillis},"initial_urge":${session.initialUrge}}"""

private fun String?.jsonStringOrNull(): String = this?.let { "\"$it\"" } ?: "null"

private const val LEGACY_OWNER_KEY = "legacy-local"
