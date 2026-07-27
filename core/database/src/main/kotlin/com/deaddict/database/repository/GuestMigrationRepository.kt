package com.deaddict.database.repository

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.TrackingEventEntity
import java.util.UUID

class GuestUploadConsent private constructor() {
    companion object {
        fun explicitlyAccepted(): GuestUploadConsent = GuestUploadConsent()
    }
}

data class GuestMigrationResult(
    val programsQueued: Int,
    val trackingEventsQueued: Int,
    val rescueSessionsQueued: Int,
)

class GuestMigrationRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    suspend fun queueExistingGuestData(consent: GuestUploadConsent): GuestMigrationResult {
        @Suppress("UNUSED_VARIABLE")
        val requiredConsent = consent
        return database.withTransaction {
            val programs = database.programDao().localOnly()
            val tracking = database.trackingDao().localOnly()
            val rescues = database.rescueDao().localOnly()
            for (program in programs) queueProgram(program)
            for (event in tracking) queueTracking(event)
            for (session in rescues) queueRescue(session)
            GuestMigrationResult(programs.size, tracking.size, rescues.size)
        }
    }

    private suspend fun queueProgram(value: ActiveProgramEntity) {
        if (database.programDao().markPending(value.id) == 1) {
            enqueue(
                SyncAggregateType.ACTIVE_PROGRAM,
                value.id,
                """{"id":"${value.id}","program_id":"${value.programId}","activated_at":${value.activatedAtEpochMillis}}""",
            )
        }
    }

    private suspend fun queueTracking(value: TrackingEventEntity) {
        if (database.trackingDao().markPending(value.id) == 1) {
            enqueue(
                SyncAggregateType.TRACKING_EVENT,
                value.id,
                """{"id":"${value.id}","program_id":"${value.programId}","kind":"${value.kind.name}","occurred_at":${value.occurredAtEpochMillis}}""",
            )
        }
    }

    private suspend fun queueRescue(value: RescueSessionEntity) {
        if (database.rescueDao().markPending(value.id) == 1) {
            enqueue(
                SyncAggregateType.RESCUE_SESSION,
                value.id,
                """{"id":"${value.id}","program_id":"${value.programId}","started_at":${value.startedAtEpochMillis},"initial_urge":${value.initialUrge}}""",
            )
        }
    }

    private suspend fun enqueue(type: SyncAggregateType, aggregateId: String, payload: String) {
        val now = clock.nowMillis()
        database.syncOutboxDao().enqueue(
            com.deaddict.database.entity.SyncOutboxEntity(
                id = ids.next(),
                idempotencyKey = "${type.name}:$aggregateId:${SyncOperation.UPSERT.name}",
                aggregateType = type,
                aggregateId = aggregateId,
                operation = SyncOperation.UPSERT,
                payload = payload,
                createdAtEpochMillis = now,
                nextAttemptAtEpochMillis = now,
            ),
        )
    }
}
