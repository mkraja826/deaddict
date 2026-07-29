package com.deaddict.app.sync

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.model.OwnerKey

interface RestoreStore {
    suspend fun apply(snapshot: CloudSnapshot): RestoreSummary
}

data class RestoreSummary(
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
)

class RoomRestoreStore(
    private val database: DeAddictDatabase,
) : RestoreStore {
    override suspend fun apply(snapshot: CloudSnapshot): RestoreSummary =
        database.withTransaction {
            val recovery = RecoveryCloudRestorer(database).apply(snapshot)
            var inserted = recovery.inserted
            var updated = recovery.updated
            var skipped = recovery.skipped
            val programTombstones = database.syncOutboxDao()
                .deleteTombstoneIds(SyncAggregateType.ACTIVE_PROGRAM.name)
                .toHashSet()
            val trackingTombstones = database.syncOutboxDao()
                .deleteTombstoneIds(SyncAggregateType.TRACKING_EVENT.name)
                .toHashSet()
            val rescueTombstones = database.syncOutboxDao()
                .deleteTombstoneIds(SyncAggregateType.RESCUE_SESSION.name)
                .toHashSet()

            for (remote in snapshot.programs) {
                if (remote.id in programTombstones) {
                    skipped += 1
                    continue
                }
                val byId = database.programDao().byId(remote.id)
                val byProgram = database.programDao().byProgramId(remote.programId)
                val localConflict = byId ?: byProgram
                if (localConflict != null && localConflict.syncState != SyncState.SYNCED) {
                    skipped += 1
                    continue
                }
                database.programDao().upsertFromCloud(
                    ActiveProgramEntity(
                        id = remote.id,
                        programId = remote.programId,
                        activatedAtEpochMillis = remote.activatedAtEpochMillis,
                        archivedAtEpochMillis = remote.archivedAtEpochMillis,
                        syncState = SyncState.SYNCED,
                    ),
                )
                if (localConflict == null) inserted += 1 else updated += 1
            }

            for (remote in snapshot.trackingEvents) {
                if (remote.id in trackingTombstones) {
                    skipped += 1
                    continue
                }
                val ownerKey = try {
                    OwnerKey.authenticated(remote.userId).value
                } catch (_: IllegalArgumentException) {
                    skipped += 1
                    continue
                }
                if (!validTrackOwnership(remote.recoveryTrackId, ownerKey, remote.programId)) {
                    skipped += 1
                    continue
                }
                val existing = database.trackingDao().byId(remote.id)
                if (existing != null && existing.syncState != SyncState.SYNCED) {
                    skipped += 1
                    continue
                }
                val kind = try {
                    TrackingEventKind.valueOf(remote.kind)
                } catch (_: IllegalArgumentException) {
                    skipped += 1
                    continue
                }
                val restored = try {
                    TrackingEventEntity(
                        id = remote.id,
                        ownerKey = ownerKey,
                        recoveryTrackId = remote.recoveryTrackId,
                        programId = remote.programId,
                        kind = kind,
                        quantity = remote.quantity,
                        unit = remote.unit,
                        costMinorUnits = remote.costMinorUnits,
                        urgeIntensity = remote.urgeIntensity,
                        triggerKey = remote.triggerKey,
                        occurredAtEpochMillis = remote.occurredAtEpochMillis,
                        createdAtEpochMillis = remote.clientUpdatedAtEpochMillis,
                        privateNote = existing?.privateNote,
                        syncState = SyncState.SYNCED,
                    )
                } catch (_: IllegalArgumentException) {
                    skipped += 1
                    continue
                }
                database.trackingDao().upsertFromCloud(restored)
                if (existing == null) inserted += 1 else updated += 1
            }

            for (remote in snapshot.rescueSessions) {
                if (remote.id in rescueTombstones) {
                    skipped += 1
                    continue
                }
                val ownerKey = try {
                    OwnerKey.authenticated(remote.userId).value
                } catch (_: IllegalArgumentException) {
                    skipped += 1
                    continue
                }
                if (!validTrackOwnership(remote.recoveryTrackId, ownerKey, remote.programId)) {
                    skipped += 1
                    continue
                }
                val existing = database.rescueDao().byId(remote.id)
                if (existing != null && existing.syncState != SyncState.SYNCED) {
                    skipped += 1
                    continue
                }
                val outcome = try {
                    remote.outcome?.let(RescueOutcome::valueOf)
                } catch (_: IllegalArgumentException) {
                    skipped += 1
                    continue
                }
                val restored = try {
                    RescueSessionEntity(
                        id = remote.id,
                        ownerKey = ownerKey,
                        recoveryTrackId = remote.recoveryTrackId,
                        programId = remote.programId,
                        startedAtEpochMillis = remote.startedAtEpochMillis,
                        completedAtEpochMillis = remote.completedAtEpochMillis,
                        initialUrge = remote.initialUrge,
                        finalUrge = remote.finalUrge,
                        triggerKey = remote.triggerKey,
                        actionKeys = remote.actionKeys,
                        outcome = outcome,
                        syncState = SyncState.SYNCED,
                    )
                } catch (_: IllegalArgumentException) {
                    skipped += 1
                    continue
                }
                database.rescueDao().upsertFromCloud(restored)
                if (existing == null) inserted += 1 else updated += 1
            }

            RestoreSummary(inserted, updated, skipped)
        }

    private suspend fun validTrackOwnership(
        recoveryTrackId: String?,
        ownerKey: String,
        programId: String,
    ): Boolean {
        if (recoveryTrackId == null) return true
        val track = database.recoveryTrackDao().byId(recoveryTrackId) ?: return false
        return track.ownerKey == ownerKey && track.programId == programId
    }
}

class CloudRestoreProcessor(
    private val store: RestoreStore,
    private val remote: RemoteSyncGateway,
) {
    suspend fun restore(): RestoreSummary? {
        if (!remote.available) return null
        if (remote.currentUserId() == null) return null
        return store.apply(remote.downloadSnapshot())
    }
}
