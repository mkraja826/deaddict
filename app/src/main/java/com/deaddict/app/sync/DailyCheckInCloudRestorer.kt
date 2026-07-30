package com.deaddict.app.sync

import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInEntryEntity
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.OwnerKey

internal class DailyCheckInCloudRestorer(
    private val database: DeAddictDatabase,
) {
    suspend fun apply(snapshot: CloudSnapshot): RestoreSummary {
        val counts = Counts()
        val dao = database.dailyCheckInDao()
        val checkInTombstones = database.syncOutboxDao()
            .deleteTombstoneIds(SyncAggregateType.DAILY_CHECK_IN.name)
            .toHashSet()
        val entryTombstones = database.syncOutboxDao()
            .deleteTombstoneIds(SyncAggregateType.TRACK_CHECK_IN_ENTRY.name)
            .toHashSet()
        val localParentIds = mutableMapOf<ParentKey, String?>()

        for (remote in snapshot.dailyCheckIns.sortedWith(
            compareBy<RemoteDailyCheckInRecord> { it.localDateEpochDay }
                .thenBy { it.clientUpdatedAtEpochMillis }
                .thenBy { it.id },
        )) {
            val parentKey = ParentKey(remote.userId, remote.localDateEpochDay)
            if (remote.id in checkInTombstones) {
                localParentIds[parentKey] = null
                counts.skipped += 1
                continue
            }
            val ownerKey = authenticatedOwner(remote.userId) ?: run {
                localParentIds[parentKey] = null
                counts.skipped += 1
                continue
            }
            val byId = dao.byId(remote.id)
            if (
                byId != null &&
                (byId.ownerKey != ownerKey || byId.localDateEpochDay != remote.localDateEpochDay)
            ) {
                localParentIds[parentKey] = null
                counts.skipped += 1
                continue
            }
            val byDate = dao.byDate(ownerKey, remote.localDateEpochDay)
            val localConflict = byId ?: byDate
            if (localConflict != null && localConflict.syncState != SyncState.SYNCED) {
                localParentIds[parentKey] = null
                counts.skipped += 1
                continue
            }
            val restored = try {
                DailyCheckInEntity(
                    id = localConflict?.id ?: remote.id,
                    ownerKey = ownerKey,
                    localDateEpochDay = remote.localDateEpochDay,
                    mood = remote.mood,
                    stress = remote.stress,
                    energy = remote.energy,
                    sleepQuality = remote.sleepQuality,
                    createdAtEpochMillis = remote.createdAtEpochMillis,
                    updatedAtEpochMillis = remote.clientUpdatedAtEpochMillis,
                    revision = remote.revision,
                    syncState = SyncState.SYNCED,
                )
            } catch (_: IllegalArgumentException) {
                localParentIds[parentKey] = null
                counts.skipped += 1
                continue
            }
            dao.upsertCheckIn(restored)
            localParentIds[parentKey] = restored.id
            if (localConflict == null) counts.inserted += 1 else counts.updated += 1
        }

        for (remote in snapshot.trackCheckInEntries.sortedWith(
            compareBy<RemoteTrackCheckInEntryRecord> { it.localDateEpochDay }
                .thenBy { it.recoveryTrackId }
                .thenBy { it.clientUpdatedAtEpochMillis }
                .thenBy { it.id },
        )) {
            if (remote.id in entryTombstones) {
                counts.skipped += 1
                continue
            }
            val localParentId = localParentIds[ParentKey(remote.userId, remote.localDateEpochDay)]
            if (localParentId == null) {
                counts.skipped += 1
                continue
            }
            val parent = dao.byId(localParentId)
            val ownerKey = authenticatedOwner(remote.userId)
            if (parent == null || ownerKey == null || parent.ownerKey != ownerKey) {
                counts.skipped += 1
                continue
            }
            val track = database.recoveryTrackDao().byId(remote.recoveryTrackId)
            if (track == null || track.ownerKey != ownerKey) {
                counts.skipped += 1
                continue
            }
            if (remote.goalVersionId != null) {
                val goal = database.recoveryGoalDao().byId(remote.goalVersionId)
                if (goal == null || goal.recoveryTrackId != remote.recoveryTrackId) {
                    counts.skipped += 1
                    continue
                }
            }
            val outcome = try {
                TrackCheckInOutcome.valueOf(remote.outcome)
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            val byId = dao.entryById(remote.id)
            if (
                byId != null &&
                (byId.dailyCheckInId != localParentId || byId.recoveryTrackId != remote.recoveryTrackId)
            ) {
                counts.skipped += 1
                continue
            }
            val byTrack = dao.entryForTrack(localParentId, remote.recoveryTrackId)
            val localConflict = byId ?: byTrack
            if (localConflict != null && localConflict.syncState != SyncState.SYNCED) {
                counts.skipped += 1
                continue
            }
            val restored = try {
                TrackCheckInEntryEntity(
                    id = localConflict?.id ?: remote.id,
                    dailyCheckInId = localParentId,
                    recoveryTrackId = remote.recoveryTrackId,
                    goalVersionId = remote.goalVersionId,
                    outcome = outcome,
                    measuredValue = remote.measuredValue,
                    unitKey = remote.unitKey,
                    peakUrge = remote.peakUrge,
                    privateNote = localConflict?.privateNote,
                    createdAtEpochMillis = remote.createdAtEpochMillis,
                    updatedAtEpochMillis = remote.clientUpdatedAtEpochMillis,
                    revision = remote.revision,
                    syncState = SyncState.SYNCED,
                )
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            dao.upsertEntry(restored)
            if (localConflict == null) counts.inserted += 1 else counts.updated += 1
        }

        return RestoreSummary(counts.inserted, counts.updated, counts.skipped)
    }

    private fun authenticatedOwner(userId: String): String? =
        runCatching { OwnerKey.authenticated(userId).value }.getOrNull()

    private data class ParentKey(
        val userId: String,
        val localDateEpochDay: Long,
    )

    private data class Counts(
        var inserted: Int = 0,
        var updated: Int = 0,
        var skipped: Int = 0,
    )
}
