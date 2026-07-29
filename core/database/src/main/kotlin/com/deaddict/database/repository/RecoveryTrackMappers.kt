package com.deaddict.database.repository

import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalVersion
import com.deaddict.model.RecoveryGoalVersionId
import com.deaddict.model.RecoveryTrack
import com.deaddict.model.RecoveryTrackId
import com.deaddict.programs.ProgramId
import java.time.Instant

fun RecoveryTrackEntity.toDomain(): RecoveryTrack = RecoveryTrack(
    id = RecoveryTrackId.parse(id),
    ownerKey = OwnerKey.parse(ownerKey),
    programId = ProgramId.of(programId),
    displayAlias = displayAlias,
    role = role,
    status = status,
    startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
    pausedAt = pausedAtEpochMillis?.let(Instant::ofEpochMilli),
    maintenanceAt = maintenanceAtEpochMillis?.let(Instant::ofEpochMilli),
    archivedAt = archivedAtEpochMillis?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    revision = revision,
)

fun RecoveryTrack.toEntity(syncState: SyncState): RecoveryTrackEntity = RecoveryTrackEntity(
    id = id.value,
    ownerKey = ownerKey.value,
    programId = programId.value,
    displayAlias = displayAlias,
    role = role,
    status = status,
    startedAtEpochMillis = startedAt.toEpochMilli(),
    pausedAtEpochMillis = pausedAt?.toEpochMilli(),
    maintenanceAtEpochMillis = maintenanceAt?.toEpochMilli(),
    archivedAtEpochMillis = archivedAt?.toEpochMilli(),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    revision = revision,
    syncState = syncState,
)

fun RecoveryGoalVersionEntity.toDomain(): RecoveryGoalVersion = RecoveryGoalVersion(
    id = RecoveryGoalVersionId.parse(id),
    recoveryTrackId = RecoveryTrackId.parse(recoveryTrackId),
    goalType = goalType,
    targetValue = targetValue,
    unitKey = unitKey,
    periodType = periodType,
    title = title,
    effectiveFrom = Instant.ofEpochMilli(effectiveFromEpochMillis),
    effectiveUntil = effectiveUntilEpochMillis?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    revision = revision,
)

fun RecoveryGoalVersion.toEntity(syncState: SyncState): RecoveryGoalVersionEntity =
    RecoveryGoalVersionEntity(
        id = id.value,
        recoveryTrackId = recoveryTrackId.value,
        goalType = goalType,
        targetValue = targetValue,
        unitKey = unitKey,
        periodType = periodType,
        title = title,
        effectiveFromEpochMillis = effectiveFrom.toEpochMilli(),
        effectiveUntilEpochMillis = effectiveUntil?.toEpochMilli(),
        createdAtEpochMillis = createdAt.toEpochMilli(),
        updatedAtEpochMillis = updatedAt.toEpochMilli(),
        revision = revision,
        syncState = syncState,
    )
