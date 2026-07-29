package com.deaddict.database.repository

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.entity.OutboxState
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryGoalVersion
import com.deaddict.model.RecoveryGoalVersionId
import com.deaddict.model.RecoveryTrack
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.model.isPrimaryEligible
import com.deaddict.programs.ProgramId
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class RecoveryGoalDraft(
    val goalType: RecoveryGoalType,
    val targetValue: Double? = null,
    val unitKey: String? = null,
    val periodType: GoalPeriodType? = null,
    val title: String? = null,
)

class LocalRecoveryTrackRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeOpen(ownerKey: OwnerKey): Flow<List<RecoveryTrack>> =
        database.recoveryTrackDao().observeOpen(ownerKey.value)
            .map { tracks -> tracks.map(RecoveryTrackEntity::toDomain) }

    fun observeArchived(ownerKey: OwnerKey): Flow<List<RecoveryTrack>> =
        database.recoveryTrackDao().observeArchived(ownerKey.value)
            .map { tracks -> tracks.map(RecoveryTrackEntity::toDomain) }

    fun observeCurrentGoal(trackId: RecoveryTrackId): Flow<RecoveryGoalVersion?> =
        database.recoveryGoalDao().observeCurrent(trackId.value)
            .map { it?.toDomain() }

    fun observeGoalHistory(trackId: RecoveryTrackId): Flow<List<RecoveryGoalVersion>> =
        database.recoveryGoalDao().observeHistory(trackId.value)
            .map { goals -> goals.map(RecoveryGoalVersionEntity::toDomain) }

    suspend fun create(
        ownerKey: OwnerKey,
        programId: ProgramId,
        initialGoal: RecoveryGoalDraft,
        syncPolicy: SyncPolicy,
        displayAlias: String? = null,
        startedAtEpochMillis: Long? = null,
    ): RecoveryTrackId = database.withTransaction {
        createInTransaction(
            ownerKey = ownerKey,
            programId = programId,
            initialGoal = initialGoal,
            syncPolicy = syncPolicy,
            displayAlias = displayAlias,
            startedAtEpochMillis = startedAtEpochMillis,
        )
    }

    suspend fun makePrimary(ownerKey: OwnerKey, trackId: RecoveryTrackId): Boolean =
        database.withTransaction {
            val target = requireOwned(ownerKey, trackId)
            require(target.status.isPrimaryEligible) {
                "Only active or maintenance tracks can be primary"
            }
            val current = database.recoveryTrackDao().primary(ownerKey.value)
            if (current?.id == target.id) return@withTransaction false

            val now = clock.nowMillis()
            current?.let { existing ->
                val demoted = existing.copy(
                    role = RecoveryTrackRole.SUPPORTING,
                    updatedAtEpochMillis = now,
                    revision = existing.revision + 1,
                    syncState = existing.syncState.forMutation(),
                )
                check(database.recoveryTrackDao().update(demoted) == 1)
                enqueueTrackIfNeeded(demoted, now)
            }

            val promoted = target.copy(
                role = RecoveryTrackRole.PRIMARY,
                updatedAtEpochMillis = now,
                revision = target.revision + 1,
                syncState = target.syncState.forMutation(),
            )
            check(database.recoveryTrackDao().update(promoted) == 1)
            enqueueTrackIfNeeded(promoted, now)
            true
        }

    suspend fun pause(ownerKey: OwnerKey, trackId: RecoveryTrackId): RecoveryTrack =
        transition(ownerKey, trackId, RecoveryTrackStatus.PAUSED)

    suspend fun resume(ownerKey: OwnerKey, trackId: RecoveryTrackId): RecoveryTrack =
        transition(ownerKey, trackId, RecoveryTrackStatus.ACTIVE)

    suspend fun enterMaintenance(ownerKey: OwnerKey, trackId: RecoveryTrackId): RecoveryTrack =
        transition(ownerKey, trackId, RecoveryTrackStatus.MAINTENANCE)

    suspend fun archive(ownerKey: OwnerKey, trackId: RecoveryTrackId): RecoveryTrack =
        transition(ownerKey, trackId, RecoveryTrackStatus.ARCHIVED)

    suspend fun restart(
        ownerKey: OwnerKey,
        archivedTrackId: RecoveryTrackId,
        initialGoal: RecoveryGoalDraft,
        syncPolicy: SyncPolicy,
        startedAtEpochMillis: Long? = null,
    ): RecoveryTrackId = database.withTransaction {
        val archived = requireOwned(ownerKey, archivedTrackId)
        require(archived.status == RecoveryTrackStatus.ARCHIVED) {
            "Only an archived Recovery Track can be restarted"
        }
        createInTransaction(
            ownerKey = ownerKey,
            programId = ProgramId.of(archived.programId),
            initialGoal = initialGoal,
            syncPolicy = syncPolicy,
            displayAlias = archived.displayAlias,
            startedAtEpochMillis = startedAtEpochMillis,
        )
    }

    suspend fun changeGoal(
        ownerKey: OwnerKey,
        trackId: RecoveryTrackId,
        replacement: RecoveryGoalDraft,
    ): RecoveryGoalVersion = database.withTransaction {
        val track = requireOwned(ownerKey, trackId)
        require(track.status != RecoveryTrackStatus.ARCHIVED) {
            "Archived Recovery Tracks are read-only"
        }
        val current = checkNotNull(database.recoveryGoalDao().current(trackId.value)) {
            "Recovery Track must have one current goal"
        }
        val now = clock.nowMillis()
        val effectiveAt = maxOf(now, current.effectiveFromEpochMillis + 1)
        val nextState = track.syncState.forMutation()
        val replacementDomain = replacement.toDomain(
            id = RecoveryGoalVersionId.parse(ids.next()),
            trackId = trackId,
            effectiveFromEpochMillis = effectiveAt,
            createdAtEpochMillis = effectiveAt,
        )
        val replacementEntity = replacementDomain.toEntity(nextState)

        database.recoveryGoalDao().replaceCurrent(
            recoveryTrackId = trackId.value,
            closedAtEpochMillis = effectiveAt,
            replacement = replacementEntity,
            syncState = nextState,
        )

        val closed = checkNotNull(database.recoveryGoalDao().byId(current.id))
        enqueueGoalIfNeeded(closed, now)
        enqueueGoalIfNeeded(replacementEntity, now)
        replacementDomain
    }

    suspend fun reconcileOwner(
        from: OwnerKey,
        to: OwnerKey,
        syncPolicy: SyncPolicy,
    ): Int = database.withTransaction {
        if (from == to) return@withTransaction 0
        require(!from.isAuthenticated || to.isAuthenticated) {
            "Authenticated Recovery Track data cannot be reassigned to a guest profile"
        }

        val sourceTracks = database.recoveryTrackDao().allForOwner(from.value)
        if (sourceTracks.isEmpty()) return@withTransaction 0

        val targetOpenPrograms = database.recoveryTrackDao().openForOwner(to.value)
            .mapTo(mutableSetOf()) { it.programId }
        val conflicts = sourceTracks
            .filter { it.status != RecoveryTrackStatus.ARCHIVED }
            .map { it.programId }
            .filter { it in targetOpenPrograms }
        require(conflicts.isEmpty()) {
            "Destination owner already has an open Recovery Track for: ${conflicts.distinct()}"
        }

        val now = clock.nowMillis()
        var destinationHasPrimary = database.recoveryTrackDao().primary(to.value) != null
        val destinationState = syncPolicy.initialSyncState()

        for (source in sourceTracks) {
            val role = when {
                source.role != RecoveryTrackRole.PRIMARY -> source.role
                destinationHasPrimary -> RecoveryTrackRole.SUPPORTING
                else -> {
                    destinationHasPrimary = true
                    RecoveryTrackRole.PRIMARY
                }
            }
            val adopted = source.copy(
                ownerKey = to.value,
                role = role,
                updatedAtEpochMillis = now,
                revision = source.revision + 1,
                syncState = destinationState,
            )
            check(database.recoveryTrackDao().update(adopted) == 1)
            enqueueTrackIfNeeded(adopted, now)

            for (goal in database.recoveryGoalDao().allForTrack(source.id)) {
                val adoptedGoal = goal.copy(
                    updatedAtEpochMillis = now,
                    revision = goal.revision + 1,
                    syncState = destinationState,
                )
                check(database.recoveryGoalDao().update(adoptedGoal) == 1)
                enqueueGoalIfNeeded(adoptedGoal, now)
            }
        }
        sourceTracks.size
    }

    private suspend fun transition(
        ownerKey: OwnerKey,
        trackId: RecoveryTrackId,
        targetStatus: RecoveryTrackStatus,
    ): RecoveryTrack = database.withTransaction {
        val existing = requireOwned(ownerKey, trackId)
        val now = clock.nowMillis()
        val transitioned = existing.toDomain().transitionTo(
            target = targetStatus,
            at = Instant.ofEpochMilli(now),
        )
        val updated = transitioned.toEntity(existing.syncState.forMutation())
        check(database.recoveryTrackDao().update(updated) == 1)
        enqueueTrackIfNeeded(updated, now)

        if (
            existing.role == RecoveryTrackRole.PRIMARY &&
            !targetStatus.isPrimaryEligible
        ) {
            database.recoveryTrackDao()
                .primaryFallback(ownerKey.value, existing.id)
                ?.let { fallback ->
                    val promoted = fallback.copy(
                        role = RecoveryTrackRole.PRIMARY,
                        updatedAtEpochMillis = now,
                        revision = fallback.revision + 1,
                        syncState = fallback.syncState.forMutation(),
                    )
                    check(database.recoveryTrackDao().update(promoted) == 1)
                    enqueueTrackIfNeeded(promoted, now)
                }
        }
        transitioned
    }

    private suspend fun createInTransaction(
        ownerKey: OwnerKey,
        programId: ProgramId,
        initialGoal: RecoveryGoalDraft,
        syncPolicy: SyncPolicy,
        displayAlias: String?,
        startedAtEpochMillis: Long?,
    ): RecoveryTrackId {
        require(database.recoveryTrackDao().openByProgramId(ownerKey.value, programId.value) == null) {
            "Owner already has an open Recovery Track for ${programId.value}"
        }

        val now = clock.nowMillis()
        val startedAt = startedAtEpochMillis ?: now
        require(startedAt <= now) { "Recovery Track start time cannot be in the future" }
        val state = syncPolicy.initialSyncState()
        val trackId = RecoveryTrackId.parse(ids.next())
        val role = if (database.recoveryTrackDao().primary(ownerKey.value) == null) {
            RecoveryTrackRole.PRIMARY
        } else {
            RecoveryTrackRole.SUPPORTING
        }
        val track = RecoveryTrack(
            id = trackId,
            ownerKey = ownerKey,
            programId = programId,
            displayAlias = displayAlias,
            role = role,
            status = RecoveryTrackStatus.ACTIVE,
            startedAt = Instant.ofEpochMilli(startedAt),
            createdAt = Instant.ofEpochMilli(now),
            updatedAt = Instant.ofEpochMilli(now),
        ).toEntity(state)
        val goal = initialGoal.toDomain(
            id = RecoveryGoalVersionId.parse(ids.next()),
            trackId = trackId,
            effectiveFromEpochMillis = startedAt,
            createdAtEpochMillis = now,
        ).toEntity(state)

        database.recoveryTrackDao().insert(track)
        database.recoveryGoalDao().insert(goal)
        enqueueTrackIfNeeded(track, now)
        enqueueGoalIfNeeded(goal, now)
        return trackId
    }

    private suspend fun requireOwned(
        ownerKey: OwnerKey,
        trackId: RecoveryTrackId,
    ): RecoveryTrackEntity {
        val track = checkNotNull(database.recoveryTrackDao().byId(trackId.value)) {
            "Recovery Track does not exist"
        }
        require(track.ownerKey == ownerKey.value) { "Recovery Track belongs to another owner" }
        return track
    }

    private suspend fun enqueueTrackIfNeeded(track: RecoveryTrackEntity, now: Long) {
        if (track.syncState == SyncState.LOCAL_ONLY) return
        enqueueUpsert(
            aggregateType = SyncAggregateType.RECOVERY_TRACK,
            aggregateId = track.id,
            revision = track.revision,
            now = now,
        )
    }

    private suspend fun enqueueGoalIfNeeded(goal: RecoveryGoalVersionEntity, now: Long) {
        if (goal.syncState == SyncState.LOCAL_ONLY) return
        enqueueUpsert(
            aggregateType = SyncAggregateType.RECOVERY_GOAL,
            aggregateId = goal.id,
            revision = goal.revision,
            now = now,
        )
    }

    private suspend fun enqueueUpsert(
        aggregateType: SyncAggregateType,
        aggregateId: String,
        revision: Long,
        now: Long,
    ) {
        database.syncOutboxDao().enqueue(
            SyncOutboxEntity(
                id = ids.next(),
                idempotencyKey = "${aggregateType.name}:$aggregateId:UPSERT:$revision",
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                operation = SyncOperation.UPSERT,
                payload = """{"id":"$aggregateId","revision":$revision}""",
                createdAtEpochMillis = now,
                nextAttemptAtEpochMillis = now,
                state = OutboxState.PENDING,
            ),
        )
    }
}

private fun RecoveryGoalDraft.toDomain(
    id: RecoveryGoalVersionId,
    trackId: RecoveryTrackId,
    effectiveFromEpochMillis: Long,
    createdAtEpochMillis: Long,
): RecoveryGoalVersion = RecoveryGoalVersion(
    id = id,
    recoveryTrackId = trackId,
    goalType = goalType,
    targetValue = targetValue,
    unitKey = unitKey,
    periodType = periodType,
    title = title,
    effectiveFrom = Instant.ofEpochMilli(effectiveFromEpochMillis),
    effectiveUntil = null,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

private fun SyncPolicy.initialSyncState(): SyncState = when (this) {
    SyncPolicy.LOCAL_ONLY -> SyncState.LOCAL_ONLY
    SyncPolicy.CLOUD_ELIGIBLE -> SyncState.PENDING
}

private fun SyncState.forMutation(): SyncState = when (this) {
    SyncState.LOCAL_ONLY -> SyncState.LOCAL_ONLY
    SyncState.PENDING, SyncState.SYNCED -> SyncState.PENDING
}
