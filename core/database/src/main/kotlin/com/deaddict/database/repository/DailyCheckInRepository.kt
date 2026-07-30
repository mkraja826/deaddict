package com.deaddict.database.repository

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.dao.DailyCheckInWithEntries
import com.deaddict.database.entity.DailyCheckInEntity
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
            syncState = SyncState.LOCAL_ONLY,
        )
        dao.upsertCheckIn(checkIn)

        val entries = resolvedEntries.map { (draftEntry, track, goal) ->
            val previous = existingEntries[track.id]
            TrackCheckInEntryEntity(
                id = previous?.id ?: ids.next(),
                dailyCheckInId = checkInId,
                recoveryTrackId = track.id,
                goalVersionId = goal.id,
                outcome = draftEntry.outcome,
                measuredValue = draftEntry.measuredValue,
                unitKey = draftEntry.unitKey?.trim()?.takeIf(String::isNotEmpty),
                peakUrge = draftEntry.peakUrge,
                privateNote = draftEntry.privateNote?.trim()?.takeIf(String::isNotEmpty),
                createdAtEpochMillis = previous?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now,
                revision = previous?.revision?.plus(1) ?: 0,
                syncState = SyncState.LOCAL_ONLY,
            )
        }
        // Entries omitted from an edit can belong to a track paused or archived later that day.
        // Preserve them so lifecycle changes never erase an already-recorded Recovery Track outcome.
        dao.upsertEntries(entries)
        checkInId
    }

    suspend fun delete(id: String): Boolean = database.withTransaction {
        database.dailyCheckInDao().deleteById(id) == 1
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
