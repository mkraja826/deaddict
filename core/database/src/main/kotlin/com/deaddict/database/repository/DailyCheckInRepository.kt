package com.deaddict.database.repository

import androidx.room.withTransaction
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.dao.DailyCheckInWithEntries
import com.deaddict.database.entity.DailyCheckInDraftEntity
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInEntryEntity
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.database.entity.acceptsCheckIns
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import com.deaddict.programs.ProgramId
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Shared fields recorded once for a local calendar day. */
data class NewDailyCheckIn(
    val ownerKey: OwnerKey,
    val localDate: String,
    val timezoneId: String,
    val mood: Int? = null,
    val stress: Int? = null,
    val energy: Int? = null,
    val sleepQuality: Int? = null,
    val sharedTriggerKeys: List<String> = emptyList(),
    val privateNote: String? = null,
    val entries: List<NewTrackCheckInEntry>,
)

data class NewTrackCheckInEntry(
    val recoveryTrackId: RecoveryTrackId,
    val programId: ProgramId,
    val goalVersionId: String? = null,
    val outcome: TrackCheckInOutcome,
    val quantity: Double? = null,
    val unit: String? = null,
    val durationMinutes: Long? = null,
    val costMinorUnits: Long? = null,
    val urgeIntensity: Int? = null,
    val triggerKeys: List<String> = emptyList(),
    val privateNote: String? = null,
)

/**
 * Local-first canonical daily check-in persistence.
 *
 * Cloud payloads and generated tracking events are intentionally deferred to the next Phase 5 slice;
 * this repository first establishes one atomic parent plus track-entry record per owner and local day.
 */
class LocalDailyCheckInRepository(
    private val database: DeAddictDatabase,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val ids: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
) {
    fun observeForDate(ownerKey: OwnerKey, localDate: String): Flow<DailyCheckInWithEntries?> {
        validateDate(localDate)
        return database.dailyCheckInDao().observeByOwnerDate(ownerKey.value, localDate)
    }

    suspend fun complete(input: NewDailyCheckIn): String {
        validateInput(input)
        val checkInId = ids.next()
        val now = clock.nowMillis()

        database.withTransaction {
            require(database.dailyCheckInDao().byOwnerDate(input.ownerKey.value, input.localDate) == null) {
                "A daily check-in already exists for this owner and local date"
            }

            val resolvedEntries = input.entries.map { requested ->
                val track = checkNotNull(database.recoveryTrackDao().byId(requested.recoveryTrackId.value)) {
                    "Recovery Track does not exist"
                }
                require(track.ownerKey == input.ownerKey.value) {
                    "Recovery Track belongs to another owner"
                }
                require(track.programId == requested.programId.value) {
                    "Recovery Track program does not match the check-in entry"
                }
                require(track.status.acceptsCheckIns) {
                    "Only active or maintenance Recovery Tracks can receive check-ins"
                }

                val goal = requested.goalVersionId
                    ?.let { explicitId ->
                        checkNotNull(database.recoveryGoalDao().byId(explicitId)) {
                            "Goal version does not exist"
                        }
                    }
                    ?: database.recoveryGoalDao().current(track.id)
                require(goal == null || goal.recoveryTrackId == track.id) {
                    "Goal version must belong to the Recovery Track"
                }

                requested to goal?.id
            }

            database.dailyCheckInDao().insert(
                DailyCheckInEntity(
                    id = checkInId,
                    ownerKey = input.ownerKey.value,
                    localDate = input.localDate,
                    timezoneId = input.timezoneId,
                    mood = input.mood,
                    stress = input.stress,
                    energy = input.energy,
                    sleepQuality = input.sleepQuality,
                    sharedTriggerKeys = input.sharedTriggerKeys.distinct(),
                    privateNote = input.privateNote,
                    completedAtEpochMillis = now,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    revision = 0,
                    syncState = SyncState.LOCAL_ONLY,
                ),
            )
            database.trackCheckInEntryDao().insertAll(
                resolvedEntries.map { (requested, goalVersionId) ->
                    TrackCheckInEntryEntity(
                        id = ids.next(),
                        dailyCheckInId = checkInId,
                        ownerKey = input.ownerKey.value,
                        recoveryTrackId = requested.recoveryTrackId.value,
                        programId = requested.programId.value,
                        goalVersionId = goalVersionId,
                        outcome = requested.outcome,
                        quantity = requested.quantity,
                        unit = requested.unit,
                        durationMinutes = requested.durationMinutes,
                        costMinorUnits = requested.costMinorUnits,
                        urgeIntensity = requested.urgeIntensity,
                        triggerKeys = requested.triggerKeys.distinct(),
                        privateNote = requested.privateNote,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                        revision = 0,
                        syncState = SyncState.LOCAL_ONLY,
                    )
                },
            )
            database.dailyCheckInDraftDao().delete(input.ownerKey.value, input.localDate)
        }

        return checkInId
    }

    suspend fun saveDraft(
        ownerKey: OwnerKey,
        localDate: String,
        timezoneId: String,
        payloadJson: String,
    ) {
        validateDate(localDate)
        ZoneId.of(timezoneId)
        database.dailyCheckInDraftDao().upsert(
            DailyCheckInDraftEntity(
                ownerKey = ownerKey.value,
                localDate = localDate,
                timezoneId = timezoneId,
                payloadJson = payloadJson,
                updatedAtEpochMillis = clock.nowMillis(),
            ),
        )
    }

    suspend fun deleteDraft(ownerKey: OwnerKey, localDate: String): Boolean {
        validateDate(localDate)
        return database.dailyCheckInDraftDao().delete(ownerKey.value, localDate) == 1
    }

    private fun validateInput(input: NewDailyCheckIn) {
        validateDate(input.localDate)
        ZoneId.of(input.timezoneId)
        require(input.entries.isNotEmpty()) { "At least one Recovery Track entry is required" }
        require(input.entries.any { it.outcome != TrackCheckInOutcome.NOT_TRACKED }) {
            "At least one Recovery Track must have a recorded outcome"
        }
        require(input.entries.map { it.recoveryTrackId.value }.distinct().size == input.entries.size) {
            "A Recovery Track can appear only once in a daily check-in"
        }
    }

    private fun validateDate(localDate: String) {
        LocalDate.parse(localDate)
    }
}
