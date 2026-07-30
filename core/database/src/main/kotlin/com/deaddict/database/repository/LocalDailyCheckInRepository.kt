package com.deaddict.database.repository

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.dao.DailyCheckInWithEntries
import com.deaddict.database.entity.DailyCheckInDraftEntity
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInEntryEntity
import com.deaddict.model.DailyCheckIn
import com.deaddict.model.DailyCheckInId
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalVersionId
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.model.TrackCheckInEntry
import com.deaddict.model.TrackCheckInEntryId
import com.deaddict.model.TrackCheckInOutcome
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CompleteDailyCheckInInput(
    val ownerKey: OwnerKey,
    val localDate: LocalDate,
    val timezoneId: String,
    val mood: Int? = null,
    val stress: Int? = null,
    val energy: Int? = null,
    val sleep: Int? = null,
    val triggerKeys: Set<String> = emptySet(),
    val privateNote: String? = null,
    val entries: List<TrackCheckInInput>,
    val completedAtEpochMillis: Long? = null,
)

data class TrackCheckInInput(
    val recoveryTrackId: RecoveryTrackId,
    val outcome: TrackCheckInOutcome,
    val urgeIntensity: Int? = null,
    val quantity: Double? = null,
    val quantityUnit: String? = null,
    val durationMinutes: Long? = null,
    val costMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val triggerKeys: Set<String> = emptySet(),
    val privateNote: String? = null,
)

data class DailyCheckInDraft(
    val ownerKey: OwnerKey,
    val localDate: LocalDate,
    val timezoneId: String,
    val mood: Int? = null,
    val stress: Int? = null,
    val energy: Int? = null,
    val sleep: Int? = null,
    val triggerKeys: Set<String> = emptySet(),
    val privateNote: String? = null,
    val trackEntriesPayload: String = "",
    val updatedAtEpochMillis: Long,
)

data class CompletedDailyCheckIn(
    val checkIn: DailyCheckIn,
    val entries: List<TrackCheckInEntry>,
)

class LocalDailyCheckInRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    suspend fun complete(input: CompleteDailyCheckInInput): DailyCheckInId {
        require(input.entries.isNotEmpty()) { "At least one Recovery Track outcome is required" }
        require(input.entries.map { it.recoveryTrackId }.distinct().size == input.entries.size) {
            "A Recovery Track can appear only once in a daily check-in"
        }
        ZoneId.of(input.timezoneId)

        val now = clock.nowMillis()
        val completedAt = input.completedAtEpochMillis ?: now
        require(completedAt > 0) { "Completion time must be positive" }
        val checkInId = DailyCheckInId.parse(ids.next())

        database.withTransaction {
            check(database.dailyCheckInDao().byOwnerDate(input.ownerKey.value, input.localDate.toString()) == null) {
                "A completed daily check-in already exists for this date"
            }

            val parent = DailyCheckInEntity(
                id = checkInId.value,
                ownerKey = input.ownerKey.value,
                localDate = input.localDate.toString(),
                timezoneId = input.timezoneId,
                mood = input.mood,
                stress = input.stress,
                energy = input.energy,
                sleep = input.sleep,
                triggerKeys = input.triggerKeys.sorted(),
                privateNote = input.privateNote,
                completedAtEpochMillis = completedAt,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                syncState = SyncState.LOCAL_ONLY,
            )

            val entries = input.entries.map { entry ->
                val track = checkNotNull(database.recoveryTrackDao().byId(entry.recoveryTrackId.value)) {
                    "Recovery Track does not exist"
                }
                require(track.ownerKey == input.ownerKey.value) {
                    "Recovery Track belongs to another owner"
                }
                require(track.status in setOf(RecoveryTrackStatus.ACTIVE, RecoveryTrackStatus.MAINTENANCE)) {
                    "Only active or maintenance Recovery Tracks can be checked in"
                }
                val goal = database.recoveryGoalDao()
                    .allForTrack(entry.recoveryTrackId.value)
                    .lastOrNull { version ->
                        version.effectiveFromEpochMillis <= completedAt &&
                            (version.effectiveUntilEpochMillis == null || completedAt < version.effectiveUntilEpochMillis)
                    }

                TrackCheckInEntryEntity(
                    id = TrackCheckInEntryId.parse(ids.next()).value,
                    dailyCheckInId = checkInId.value,
                    ownerKey = input.ownerKey.value,
                    recoveryTrackId = entry.recoveryTrackId.value,
                    recoveryGoalVersionId = goal?.id,
                    outcome = entry.outcome,
                    urgeIntensity = entry.urgeIntensity,
                    quantity = entry.quantity,
                    quantityUnit = entry.quantityUnit,
                    durationMinutes = entry.durationMinutes,
                    costMinorUnits = entry.costMinorUnits,
                    currencyCode = entry.currencyCode?.uppercase(),
                    triggerKeys = entry.triggerKeys.sorted(),
                    privateNote = entry.privateNote,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    syncState = SyncState.LOCAL_ONLY,
                )
            }

            database.dailyCheckInDao().insertCheckIn(parent)
            database.dailyCheckInDao().insertEntries(entries)
            database.dailyCheckInDao().deleteDraft(input.ownerKey.value)
        }

        return checkInId
    }

    fun observeForDate(ownerKey: OwnerKey, localDate: LocalDate): Flow<CompletedDailyCheckIn?> =
        database.dailyCheckInDao()
            .observeOwnerDate(ownerKey.value, localDate.toString())
            .map { aggregate -> aggregate?.toDomain() }

    fun observeRecent(ownerKey: OwnerKey, limit: Int = DEFAULT_RECENT_LIMIT): Flow<List<CompletedDailyCheckIn>> {
        require(limit in 1..MAX_RECENT_LIMIT)
        return database.dailyCheckInDao()
            .observeRecent(ownerKey.value, limit)
            .map { values -> values.map(DailyCheckInWithEntries::toDomain) }
    }

    suspend fun getForDate(ownerKey: OwnerKey, localDate: LocalDate): CompletedDailyCheckIn? =
        database.dailyCheckInDao()
            .byOwnerDate(ownerKey.value, localDate.toString())
            ?.toDomain()

    suspend fun saveDraft(draft: DailyCheckInDraft) {
        ZoneId.of(draft.timezoneId)
        database.dailyCheckInDao().upsertDraft(
            DailyCheckInDraftEntity(
                ownerKey = draft.ownerKey.value,
                localDate = draft.localDate.toString(),
                timezoneId = draft.timezoneId,
                mood = draft.mood,
                stress = draft.stress,
                energy = draft.energy,
                sleep = draft.sleep,
                triggerKeys = draft.triggerKeys.sorted(),
                privateNote = draft.privateNote,
                trackEntriesPayload = draft.trackEntriesPayload,
                updatedAtEpochMillis = draft.updatedAtEpochMillis,
            ),
        )
    }

    suspend fun loadDraft(ownerKey: OwnerKey): DailyCheckInDraft? =
        database.dailyCheckInDao().draft(ownerKey.value)?.toDraft()

    suspend fun deleteDraft(ownerKey: OwnerKey): Boolean =
        database.dailyCheckInDao().deleteDraft(ownerKey.value) == 1

    private companion object {
        const val DEFAULT_RECENT_LIMIT = 30
        const val MAX_RECENT_LIMIT = 366
    }
}

private fun DailyCheckInWithEntries.toDomain(): CompletedDailyCheckIn = CompletedDailyCheckIn(
    checkIn = checkIn.toDomain(),
    entries = entries.sortedBy { it.recoveryTrackId }.map(TrackCheckInEntryEntity::toDomain),
)

private fun DailyCheckInEntity.toDomain(): DailyCheckIn = DailyCheckIn(
    id = DailyCheckInId.parse(id),
    ownerKey = OwnerKey.parse(ownerKey),
    localDate = LocalDate.parse(localDate),
    timezoneId = timezoneId,
    mood = mood,
    stress = stress,
    energy = energy,
    sleep = sleep,
    triggerKeys = triggerKeys.toSet(),
    privateNote = privateNote,
    completedAt = Instant.ofEpochMilli(completedAtEpochMillis),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

private fun TrackCheckInEntryEntity.toDomain(): TrackCheckInEntry = TrackCheckInEntry(
    id = TrackCheckInEntryId.parse(id),
    dailyCheckInId = DailyCheckInId.parse(dailyCheckInId),
    ownerKey = OwnerKey.parse(ownerKey),
    recoveryTrackId = RecoveryTrackId.parse(recoveryTrackId),
    recoveryGoalVersionId = recoveryGoalVersionId?.let(RecoveryGoalVersionId::parse),
    outcome = outcome,
    urgeIntensity = urgeIntensity,
    quantity = quantity,
    quantityUnit = quantityUnit,
    durationMinutes = durationMinutes,
    costMinorUnits = costMinorUnits,
    currencyCode = currencyCode,
    triggerKeys = triggerKeys.toSet(),
    privateNote = privateNote,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

private fun DailyCheckInDraftEntity.toDraft(): DailyCheckInDraft = DailyCheckInDraft(
    ownerKey = OwnerKey.parse(ownerKey),
    localDate = LocalDate.parse(localDate),
    timezoneId = timezoneId,
    mood = mood,
    stress = stress,
    energy = energy,
    sleep = sleep,
    triggerKeys = triggerKeys.toSet(),
    privateNote = privateNote,
    trackEntriesPayload = trackEntriesPayload,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
