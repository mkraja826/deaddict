package com.deaddict.app.sync

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.repository.SyncQueue
import com.deaddict.model.OwnerKey
import kotlinx.coroutines.CancellationException

interface SyncStore {
    suspend fun resetInterruptedClaims()

    suspend fun nextBatch(limit: Int): List<SyncOutboxEntity>

    suspend fun claim(id: String): Boolean

    suspend fun program(id: String): ActiveProgramEntity?

    suspend fun recoveryTrack(id: String): RecoveryTrackEntity?

    suspend fun recoveryGoal(id: String): RecoveryGoalVersionEntity?

    suspend fun trackingEvent(id: String): TrackingEventEntity?

    suspend fun rescueSession(id: String): RescueSessionEntity?

    suspend fun complete(item: SyncOutboxEntity)

    suspend fun fail(item: SyncOutboxEntity, errorCode: String, permanent: Boolean)
}

class RoomSyncStore(
    private val database: DeAddictDatabase,
) : SyncStore {
    private val queue = SyncQueue(database.syncOutboxDao())

    override suspend fun resetInterruptedClaims() {
        queue.resetInterruptedClaims()
    }

    override suspend fun nextBatch(limit: Int): List<SyncOutboxEntity> =
        queue.nextBatch(limit)

    override suspend fun claim(id: String): Boolean = queue.claim(id)

    override suspend fun program(id: String): ActiveProgramEntity? =
        database.programDao().byId(id)

    override suspend fun recoveryTrack(id: String): RecoveryTrackEntity? =
        database.recoveryTrackDao().byId(id)

    override suspend fun recoveryGoal(id: String): RecoveryGoalVersionEntity? =
        database.recoveryGoalDao().byId(id)

    override suspend fun trackingEvent(id: String): TrackingEventEntity? =
        database.trackingDao().byId(id)

    override suspend fun rescueSession(id: String): RescueSessionEntity? =
        database.rescueDao().byId(id)

    override suspend fun complete(item: SyncOutboxEntity) {
        database.withTransaction {
            if (item.operation == SyncOperation.UPSERT) {
                val marked = when (item.aggregateType) {
                    SyncAggregateType.ACTIVE_PROGRAM -> database.programDao().markSynced(item.aggregateId)
                    SyncAggregateType.RECOVERY_TRACK -> database.recoveryTrackDao().markSynced(item.aggregateId)
                    SyncAggregateType.RECOVERY_GOAL -> database.recoveryGoalDao().markSynced(item.aggregateId)
                    SyncAggregateType.TRACKING_EVENT -> database.trackingDao().markSynced(item.aggregateId)
                    SyncAggregateType.RESCUE_SESSION -> database.rescueDao().markSynced(item.aggregateId)
                }
                check(marked == 1) { "Synchronized local row is missing" }
            }
            check(queue.complete(item.id)) { "Outbox completion failed" }
        }
    }

    override suspend fun fail(item: SyncOutboxEntity, errorCode: String, permanent: Boolean) {
        check(
            queue.fail(
                id = item.id,
                currentAttemptCount = item.attemptCount,
                errorCode = errorCode,
                permanent = permanent,
            ),
        ) { "Outbox failure transition failed" }
    }
}

enum class SyncRunResult {
    IDLE,
    SUCCESS,
    RETRY,
    UNAVAILABLE,
    SIGNED_OUT,
}

class SyncProcessor(
    private val store: SyncStore,
    private val remote: RemoteSyncGateway,
) {
    suspend fun runBatch(limit: Int = 25): SyncRunResult {
        require(limit in 1..100)
        if (!remote.available) return SyncRunResult.UNAVAILABLE
        val userId = remote.currentUserId() ?: return SyncRunResult.SIGNED_OUT

        store.resetInterruptedClaims()
        val items = store.nextBatch(limit)
        if (items.isEmpty()) return SyncRunResult.IDLE

        var completed = false
        var retryNeeded = false
        for (item in items) {
            if (!store.claim(item.id)) continue
            try {
                applyOperation(userId, item)
                store.complete(item)
                completed = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: PermanentSyncFailure) {
                store.fail(item, failure.errorCode, permanent = true)
            } catch (invalid: IllegalArgumentException) {
                store.fail(item, "INVALID_LOCAL_DATA", permanent = true)
            } catch (_: Throwable) {
                store.fail(item, "REMOTE_WRITE_FAILED", permanent = false)
                retryNeeded = true
            }
        }

        return when {
            retryNeeded -> SyncRunResult.RETRY
            completed -> SyncRunResult.SUCCESS
            else -> SyncRunResult.IDLE
        }
    }

    private suspend fun applyOperation(userId: String, item: SyncOutboxEntity) {
        when (item.operation) {
            SyncOperation.DELETE -> remote.deleteRecord(
                userId = userId,
                aggregateType = item.aggregateType,
                aggregateId = item.aggregateId,
            )

            SyncOperation.UPSERT -> when (item.aggregateType) {
                SyncAggregateType.ACTIVE_PROGRAM -> remote.upsertProgram(
                    userId,
                    store.program(item.aggregateId)
                        ?: throw PermanentSyncFailure("LOCAL_ROW_MISSING"),
                )

                SyncAggregateType.RECOVERY_TRACK -> remote.upsertRecoveryTrack(
                    userId,
                    store.recoveryTrack(item.aggregateId)
                        ?: throw PermanentSyncFailure("LOCAL_ROW_MISSING"),
                )

                SyncAggregateType.RECOVERY_GOAL -> remote.upsertRecoveryGoal(
                    userId,
                    store.recoveryGoal(item.aggregateId)
                        ?: throw PermanentSyncFailure("LOCAL_ROW_MISSING"),
                )

                SyncAggregateType.TRACKING_EVENT -> {
                    val event = store.trackingEvent(item.aggregateId)
                        ?: throw PermanentSyncFailure("LOCAL_ROW_MISSING")
                    validateEventOwnership(userId, event.ownerKey, event.recoveryTrackId)
                    remote.upsertTrackingEvent(userId, event)
                }

                SyncAggregateType.RESCUE_SESSION -> {
                    val session = store.rescueSession(item.aggregateId)
                        ?: throw PermanentSyncFailure("LOCAL_ROW_MISSING")
                    validateEventOwnership(userId, session.ownerKey, session.recoveryTrackId)
                    remote.upsertRescueSession(userId, session)
                }
            }
        }
    }

    private fun validateEventOwnership(
        userId: String,
        ownerKey: String,
        recoveryTrackId: String?,
    ) {
        if (recoveryTrackId == null) {
            throw PermanentSyncFailure("RECOVERY_TRACK_MISSING")
        }
        val expectedOwner = OwnerKey.authenticated(userId).value
        if (ownerKey != expectedOwner) {
            throw PermanentSyncFailure("ACCOUNT_SCOPE_MISMATCH")
        }
    }
}

private class PermanentSyncFailure(
    val errorCode: String,
) : RuntimeException(errorCode)
