package com.deaddict.app.sync

import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncState
import com.deaddict.database.repository.toDomain
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus

internal class RecoveryCloudRestorer(
    private val database: DeAddictDatabase,
) {
    suspend fun apply(snapshot: CloudSnapshot): RestoreSummary {
        val counts = Counts()
        val trackTombstones = database.syncOutboxDao()
            .deleteTombstoneIds(SyncAggregateType.RECOVERY_TRACK.name)
            .toHashSet()
        val goalTombstones = database.syncOutboxDao()
            .deleteTombstoneIds(SyncAggregateType.RECOVERY_GOAL.name)
            .toHashSet()

        restoreTracks(snapshot.recoveryTracks, trackTombstones, counts)
        restoreGoals(snapshot.recoveryGoals, goalTombstones, counts)
        return RestoreSummary(counts.inserted, counts.updated, counts.skipped)
    }

    private suspend fun restoreTracks(
        records: List<RemoteRecoveryTrackRecord>,
        tombstones: Set<String>,
        counts: Counts,
    ) {
        val ordered = records.sortedWith(
            compareBy<RemoteRecoveryTrackRecord> { it.role == RecoveryTrackRole.PRIMARY.name }
                .thenBy { it.startedAtEpochMillis }
                .thenBy { it.id },
        )
        for (remote in ordered) {
            if (remote.id in tombstones) {
                counts.skipped += 1
                continue
            }

            val ownerKey = try {
                OwnerKey.authenticated(remote.userId).value
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            val role = try {
                RecoveryTrackRole.valueOf(remote.role)
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            val status = try {
                RecoveryTrackStatus.valueOf(remote.status)
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            val restored = try {
                RecoveryTrackEntity(
                    id = remote.id,
                    ownerKey = ownerKey,
                    programId = remote.programId,
                    displayAlias = remote.displayAlias,
                    role = role,
                    status = status,
                    startedAtEpochMillis = remote.startedAtEpochMillis,
                    pausedAtEpochMillis = remote.pausedAtEpochMillis,
                    maintenanceAtEpochMillis = remote.maintenanceAtEpochMillis,
                    archivedAtEpochMillis = remote.archivedAtEpochMillis,
                    createdAtEpochMillis = remote.createdAtEpochMillis,
                    updatedAtEpochMillis = remote.clientUpdatedAtEpochMillis,
                    revision = remote.revision,
                    syncState = SyncState.SYNCED,
                ).also { it.toDomain() }
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }

            val existing = database.recoveryTrackDao().byId(remote.id)
            if (existing != null && existing.syncState != SyncState.SYNCED) {
                counts.skipped += 1
                continue
            }

            if (status != RecoveryTrackStatus.ARCHIVED) {
                val duplicate = database.recoveryTrackDao()
                    .openByProgramId(ownerKey, remote.programId)
                if (duplicate != null && duplicate.id != remote.id) {
                    counts.skipped += 1
                    continue
                }
            }

            if (role == RecoveryTrackRole.PRIMARY) {
                val currentPrimary = database.recoveryTrackDao().primary(ownerKey)
                if (currentPrimary != null && currentPrimary.id != remote.id) {
                    if (currentPrimary.syncState != SyncState.SYNCED) {
                        counts.skipped += 1
                        continue
                    }
                    val supporting = currentPrimary.copy(
                        role = RecoveryTrackRole.SUPPORTING,
                        syncState = SyncState.SYNCED,
                    ).also { it.toDomain() }
                    database.recoveryTrackDao().upsertFromCloud(supporting)
                }
            }

            database.recoveryTrackDao().upsertFromCloud(restored)
            if (existing == null) counts.inserted += 1 else counts.updated += 1
        }
    }

    private suspend fun restoreGoals(
        records: List<RemoteRecoveryGoalRecord>,
        tombstones: Set<String>,
        counts: Counts,
    ) {
        val ordered = records.sortedWith(
            compareBy<RemoteRecoveryGoalRecord> { it.effectiveUntilEpochMillis == null }
                .thenBy { it.effectiveFromEpochMillis }
                .thenBy { it.id },
        )
        for (remote in ordered) {
            if (remote.id in tombstones) {
                counts.skipped += 1
                continue
            }

            val ownerKey = try {
                OwnerKey.authenticated(remote.userId).value
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            val parent = database.recoveryTrackDao().byId(remote.recoveryTrackId)
            if (parent == null || parent.ownerKey != ownerKey) {
                counts.skipped += 1
                continue
            }

            val goalType = try {
                RecoveryGoalType.valueOf(remote.goalType)
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            val periodType = try {
                remote.periodType?.let(GoalPeriodType::valueOf)
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }
            val restored = try {
                RecoveryGoalVersionEntity(
                    id = remote.id,
                    recoveryTrackId = remote.recoveryTrackId,
                    goalType = goalType,
                    targetValue = remote.targetValue,
                    unitKey = remote.unitKey,
                    periodType = periodType,
                    title = remote.title,
                    effectiveFromEpochMillis = remote.effectiveFromEpochMillis,
                    effectiveUntilEpochMillis = remote.effectiveUntilEpochMillis,
                    createdAtEpochMillis = remote.createdAtEpochMillis,
                    updatedAtEpochMillis = remote.clientUpdatedAtEpochMillis,
                    revision = remote.revision,
                    syncState = SyncState.SYNCED,
                ).also { it.toDomain() }
            } catch (_: IllegalArgumentException) {
                counts.skipped += 1
                continue
            }

            val existing = database.recoveryGoalDao().byId(remote.id)
            if (existing != null && existing.syncState != SyncState.SYNCED) {
                counts.skipped += 1
                continue
            }
            if (remote.effectiveUntilEpochMillis == null) {
                val current = database.recoveryGoalDao().current(remote.recoveryTrackId)
                if (current != null && current.id != remote.id) {
                    counts.skipped += 1
                    continue
                }
            }

            database.recoveryGoalDao().upsertFromCloud(restored)
            if (existing == null) counts.inserted += 1 else counts.updated += 1
        }
    }

    private data class Counts(
        var inserted: Int = 0,
        var updated: Int = 0,
        var skipped: Int = 0,
    )
}
