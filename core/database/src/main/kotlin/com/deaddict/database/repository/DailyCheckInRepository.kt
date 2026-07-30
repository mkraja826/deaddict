package com.deaddict.database.repository

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.dao.DailyCheckInWithEntries
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.OutboxState
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInEntryEntity
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class DailyCheckInDraft(
    val ownerKey: OwnerKey,
    val localDateEpochDay: Long,
    val mood: Int? = null,
    val stress: Int? = null,
    val energy: Int? = null,
    val sleepQuality: Int? = null,
    val entries: List<TrackCheckInDraft>,
) {
    init {
        require(localDateEpochDay >= 0)
        require(mood == null || mood in CONTEXT_RANGE)
        require(stress == null || stress in CONTEXT_RANGE)
        require(energy == null || energy in CONTEXT_RANGE)
        require(sleepQuality == null || sleepQuality in CONTEXT_RANGE)
        require(entries.isNotEmpty()) { "A daily check-in needs at least one Recovery Track entry" }
        require(entries.map { it.recoveryTrackId }.distinct().size == entries.size) {
            "A Recovery Track can appear only once in a daily check-in"
        }
    }

    private companion object {
        val CONTEXT_RANGE = 1..5
    }
}

data class TrackCheckInDraft(
    val recoveryTrackId: RecoveryTrackId,
    val outcome: TrackCheckInOutcome,
    val measuredValue: Double? = null,
    val unitKey: String? = null,
    val peakUrge: Int? = null,
    val privateNote: String? = null,
) {
    init {
        require(measuredValue == null || measuredValue.isFinite())
        require(measuredValue == null || measuredValue >= 0)
        require((measuredValue == null) == unitKey.isNullOrBlank()) {
            "Measured values and units must be supplied together"
        }
        require(peakUrge == null || peakUrge in 1..5)
        require(privateNote == null || privateNote.length <= MAX_PRIVATE_NOTE_LENGTH)
    }

    private companion object {
        const val MAX_PRIVATE_NOTE_LENGTH = 2_000
    }
}

class LocalDailyCheckInRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeForDate(
        ownerKey: OwnerKey,
        localDateEpochDay: Long,
    ): Flow<DailyCheckInWithEntries?> {
        require(localDateEpochDay >= 0)
        return database.dailyCheckInDao().observeForDate(ownerKey.value, localDateEpochDay)
    }

    fun observeRecent(
        ownerKey: OwnerKey,
        limit: Int = DEFAULT_RECENT_LIMIT,
    ): Flow<List<DailyCheckInWithEntries>> {
        require(limit in 1..MAX_RECENT_LIMIT)
        return database.dailyCheckInDao().observeRecent(ownerKey.value, limit)
    }

    suspend fun save(draft: DailyCheckInDraft): String = database.withTransaction {
        val now = clock.nowMillis()
        val dao = database.dailyCheckInDao()
        val existing = dao.byDate(draft.ownerKey.value, draft.localDateEpochDay)
        val existingEntries = existing?.let { dao.entries(it.id) }.orEmpty()
            .associateBy(TrackCheckInEntryEntity::recoveryTrackId)
        val nextState = draft.ownerKey.initialSyncState()

        val resolvedEntries = draft.entries.map { entry ->
            val track = checkNotNull(database.recoveryTrackDao().byId(entry.recoveryTrackId.value)) {
                "Recovery Track does not exist"
            }
            require(track.ownerKey == draft.ownerKey.value) {
                "Recovery Track belongs to another owner"
            }
            require(track.status in CHECK_IN_ELIGIBLE_STATUSES) {
                "Only active or maintenance Recovery Tracks can be checked in"
            }
            val goal = checkNotNull(database.recoveryGoalDao().current(track.id)) {
                "Recovery Track has no current goal"
            }
            Triple(entry, track, goal)
        }

        val checkInId = existing?.id ?: ids.next()
        val checkIn = DailyCheckInEntity(
            id = checkInId,
            ownerKey = draft.ownerKey.value,
            localDateEpochDay = draft.localDateEpochDay,
            mood = draft.mood,
            stress = draft.stress,
            energy = draft.energy,
            sleepQuality = draft.sleepQuality,
            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
            revision = existing?.revision?.plus(1) ?: 0,
            syncState = nextState,
        )
        dao.upsertCheckIn(checkIn)

        val entries = resolvedEntries.map { (draftEntry, track, goal) ->
            val previous = existingEntries[track.id]
            TrackCheckInEntryEntity(
                id = previous?.id ?: ids.next(),
                dailyCheckInId = checkInId,
                recoveryTrackId = track.id,
                goalVersionId = previous?.goalVersionId ?: goal.id,
                outcome = draftEntry.outcome,
                measuredValue = draftEntry.measuredValue,
                unitKey = draftEntry.unitKey?.trim()?.takeIf(String::isNotEmpty),
                peakUrge = draftEntry.peakUrge,
                privateNote = draftEntry.privateNote?.trim()?.takeIf(String::isNotEmpty),
                createdAtEpochMillis = previous?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now,
                revision = previous?.revision?.plus(1) ?: 0,
                syncState = nextState,
            )
        }
        // Entries omitted from an edit can belong to a track paused or archived later that day.
        // Preserve them so lifecycle changes never erase an already-recorded Recovery Track outcome.
        dao.upsertEntries(entries)
        enqueueCheckInIfNeeded(checkIn, now)
        entries.forEach { enqueueEntryIfNeeded(it, now) }
        checkInId
    }

    suspend fun reconcileOwner(from: OwnerKey, to: OwnerKey): Int = database.withTransaction {
        if (from == to) return@withTransaction 0
        require(!from.isAuthenticated || to.isAuthenticated) {
            "Authenticated daily check-ins cannot be reassigned to a guest profile"
        }

        val dao = database.dailyCheckInDao()
        val sourceCheckIns = dao.allForOwner(from.value)
        if (sourceCheckIns.isEmpty()) return@withTransaction 0
        val now = clock.nowMillis()
        val destinationState = to.initialSyncState()

        for (source in sourceCheckIns) {
            val sourceEntries = dao.entries(source.id)
            val target = dao.byDate(to.value, source.localDateEpochDay)
            if (target == null) {
                val adopted = source.copy(
                    ownerKey = to.value,
                    updatedAtEpochMillis = now,
                    revision = source.revision + 1,
                    syncState = destinationState,
                )
                check(dao.updateCheckIn(adopted) == 1)
                enqueueCheckInIfNeeded(adopted, now)
                for (entry in sourceEntries) {
                    val adoptedEntry = entry.copy(
                        updatedAtEpochMillis = now,
                        revision = entry.revision + 1,
                        syncState = destinationState,
                    )
                    check(dao.updateEntry(adoptedEntry) == 1)
                    enqueueEntryIfNeeded(adoptedEntry, now)
                }
                continue
            }

            val mergedTarget = target.copy(
                mood = target.mood ?: source.mood,
                stress = target.stress ?: source.stress,
                energy = target.energy ?: source.energy,
                sleepQuality = target.sleepQuality ?: source.sleepQuality,
                updatedAtEpochMillis = now,
                revision = target.revision + 1,
                syncState = destinationState,
            )
            check(dao.updateCheckIn(mergedTarget) == 1)
            enqueueCheckInIfNeeded(mergedTarget, now)

            val targetTrackIds = dao.entries(target.id)
                .mapTo(mutableSetOf(), TrackCheckInEntryEntity::recoveryTrackId)
            for (entry in sourceEntries) {
                if (entry.recoveryTrackId in targetTrackIds) {
                    check(dao.deleteEntryById(entry.id) == 1)
                    continue
                }
                val adoptedEntry = entry.copy(
                    dailyCheckInId = target.id,
                    updatedAtEpochMillis = now,
                    revision = entry.revision + 1,
                    syncState = destinationState,
                )
                check(dao.updateEntry(adoptedEntry) == 1)
                enqueueEntryIfNeeded(adoptedEntry, now)
                targetTrackIds += adoptedEntry.recoveryTrackId
            }
            check(dao.deleteById(source.id) == 1)
        }
        sourceCheckIns.size
    }

    suspend fun delete(ownerKey: OwnerKey, id: String): Boolean = database.withTransaction {
        val checkIn = database.dailyCheckInDao().byId(id) ?: return@withTransaction false
        require(checkIn.ownerKey == ownerKey.value) { "Daily check-in belongs to another owner" }
        val now = clock.nowMillis()
        if (checkIn.syncState != SyncState.LOCAL_ONLY) {
            enqueueDelete(SyncAggregateType.DAILY_CHECK_IN, checkIn.id, checkIn.revision, now)
        }
        database.dailyCheckInDao().deleteById(id) == 1
    }

    private suspend fun enqueueCheckInIfNeeded(checkIn: DailyCheckInEntity, now: Long) {
        if (checkIn.syncState == SyncState.LOCAL_ONLY) return
        enqueueUpsert(
            aggregateType = SyncAggregateType.DAILY_CHECK_IN,
            aggregateId = checkIn.id,
            revision = checkIn.revision,
            now = now,
        )
    }

    private suspend fun enqueueEntryIfNeeded(entry: TrackCheckInEntryEntity, now: Long) {
        if (entry.syncState == SyncState.LOCAL_ONLY) return
        enqueueUpsert(
            aggregateType = SyncAggregateType.TRACK_CHECK_IN_ENTRY,
            aggregateId = entry.id,
            revision = entry.revision,
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

    private suspend fun enqueueDelete(
        aggregateType: SyncAggregateType,
        aggregateId: String,
        revision: Long,
        now: Long,
    ) {
        database.syncOutboxDao().supersedePendingUpsert(aggregateType.name, aggregateId)
        database.syncOutboxDao().enqueue(
            SyncOutboxEntity(
                id = ids.next(),
                idempotencyKey = "${aggregateType.name}:$aggregateId:DELETE:$revision",
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                operation = SyncOperation.DELETE,
                payload = """{"id":"$aggregateId"}""",
                createdAtEpochMillis = now,
                nextAttemptAtEpochMillis = now,
                state = OutboxState.PENDING,
            ),
        )
    }

    private companion object {
        const val DEFAULT_RECENT_LIMIT = 30
        const val MAX_RECENT_LIMIT = 366
        val CHECK_IN_ELIGIBLE_STATUSES = setOf(
            RecoveryTrackStatus.ACTIVE,
            RecoveryTrackStatus.MAINTENANCE,
        )
    }
}

private fun OwnerKey.initialSyncState(): SyncState =
    if (isAuthenticated) SyncState.PENDING else SyncState.LOCAL_ONLY
