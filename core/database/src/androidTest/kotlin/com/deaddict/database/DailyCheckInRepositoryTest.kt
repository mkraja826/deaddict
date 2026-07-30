package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.database.repository.DailyCheckInDraft
import com.deaddict.database.repository.EpochClock
import com.deaddict.database.repository.IdGenerator
import com.deaddict.database.repository.LocalDailyCheckInRepository
import com.deaddict.database.repository.TrackCheckInDraft
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyCheckInRepositoryTest {
    private lateinit var database: DeAddictDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            DeAddictDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveBindsSharedContextAndEntryToCurrentGoal() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        val trackId = RecoveryTrackId.parse(TRACK_ID)
        insertTrackAndGoal(owner, trackId)
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = sequenceIds(CHECK_IN_ID, ENTRY_ID),
        )

        val savedId = repository.save(
            DailyCheckInDraft(
                ownerKey = owner,
                localDateEpochDay = 20_000L,
                mood = 4,
                stress = 2,
                energy = 3,
                sleepQuality = 4,
                entries = listOf(
                    TrackCheckInDraft(
                        recoveryTrackId = trackId,
                        outcome = TrackCheckInOutcome.GOAL_MET,
                        measuredValue = 0.0,
                        unitKey = "units",
                        peakUrge = 3,
                        privateNote = "Stayed aligned after a difficult afternoon.",
                    ),
                ),
            ),
        )

        val stored = repository.observeForDate(owner, 20_000L).first()
        assertEquals(CHECK_IN_ID, savedId)
        assertEquals(4, stored?.checkIn?.mood)
        assertEquals(2, stored?.checkIn?.stress)
        assertEquals(GOAL_ID, stored?.entries?.single()?.goalVersionId)
        assertEquals(TRACK_ID, stored?.entries?.single()?.recoveryTrackId)
        assertEquals("Stayed aligned after a difficult afternoon.", stored?.entries?.single()?.privateNote)
        assertEquals(SyncState.LOCAL_ONLY, stored?.checkIn?.syncState)
    }

    @Test
    fun editingSameDatePreservesStableIdsAndIncrementsRevisions() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        val trackId = RecoveryTrackId.parse(TRACK_ID)
        insertTrackAndGoal(owner, trackId)
        var now = 2_000L
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { now },
            ids = sequenceIds(CHECK_IN_ID, ENTRY_ID),
        )

        repository.save(draft(owner, trackId, TrackCheckInOutcome.GOAL_PARTLY_MET))
        now = 3_000L
        repository.save(draft(owner, trackId, TrackCheckInOutcome.GOAL_MET))

        val stored = repository.observeForDate(owner, 20_000L).first()
        assertEquals(CHECK_IN_ID, stored?.checkIn?.id)
        assertEquals(1L, stored?.checkIn?.revision)
        assertEquals(ENTRY_ID, stored?.entries?.single()?.id)
        assertEquals(1L, stored?.entries?.single()?.revision)
        assertEquals(TrackCheckInOutcome.GOAL_MET, stored?.entries?.single()?.outcome)
        assertEquals(3_000L, stored?.entries?.single()?.updatedAtEpochMillis)
    }

    @Test
    fun saveRejectsRecoveryTrackFromAnotherOwner() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        val anotherOwner = OwnerKey.guest("profile-2")
        val trackId = RecoveryTrackId.parse(TRACK_ID)
        insertTrackAndGoal(anotherOwner, trackId)
        val repository = LocalDailyCheckInRepository(database)

        val result = runCatching {
            repository.save(draft(owner, trackId, TrackCheckInOutcome.GOAL_MET))
        }

        assertTrue(result.isFailure)
        assertTrue(database.dailyCheckInDao().byDate(owner.value, 20_000L) == null)
    }

    private fun draft(
        owner: OwnerKey,
        trackId: RecoveryTrackId,
        outcome: TrackCheckInOutcome,
    ) = DailyCheckInDraft(
        ownerKey = owner,
        localDateEpochDay = 20_000L,
        mood = 3,
        stress = 3,
        energy = 3,
        sleepQuality = 3,
        entries = listOf(
            TrackCheckInDraft(
                recoveryTrackId = trackId,
                outcome = outcome,
                peakUrge = 2,
            ),
        ),
    )

    private suspend fun insertTrackAndGoal(owner: OwnerKey, trackId: RecoveryTrackId) {
        database.recoveryTrackDao().insert(
            RecoveryTrackEntity(
                id = trackId.value,
                ownerKey = owner.value,
                programId = "gaming",
                displayAlias = null,
                role = RecoveryTrackRole.PRIMARY,
                status = RecoveryTrackStatus.ACTIVE,
                startedAtEpochMillis = 500L,
                pausedAtEpochMillis = null,
                maintenanceAtEpochMillis = null,
                archivedAtEpochMillis = null,
                createdAtEpochMillis = 500L,
                updatedAtEpochMillis = 500L,
                revision = 0,
                syncState = SyncState.LOCAL_ONLY,
            ),
        )
        database.recoveryGoalDao().insert(
            RecoveryGoalVersionEntity(
                id = GOAL_ID,
                recoveryTrackId = trackId.value,
                goalType = RecoveryGoalType.AWARENESS_ONLY,
                targetValue = null,
                unitKey = null,
                periodType = null,
                title = null,
                effectiveFromEpochMillis = 500L,
                effectiveUntilEpochMillis = null,
                createdAtEpochMillis = 500L,
                updatedAtEpochMillis = 500L,
                revision = 0,
                syncState = SyncState.LOCAL_ONLY,
            ),
        )
    }

    private fun sequenceIds(vararg values: String): IdGenerator {
        val ids = ArrayDeque(values.toList())
        return IdGenerator { ids.removeFirst() }
    }

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000101"
        const val GOAL_ID = "00000000-0000-0000-0000-000000000102"
        const val CHECK_IN_ID = "00000000-0000-0000-0000-000000000103"
        const val ENTRY_ID = "00000000-0000-0000-0000-000000000104"
    }
}
