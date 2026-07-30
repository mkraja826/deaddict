package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncAggregateType
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
import org.junit.Assert.assertFalse
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
    fun authenticatedSaveQueuesParentBeforeSanitizedTrackEntry() = runBlocking {
        val owner = OwnerKey.authenticated("user-1")
        val trackId = RecoveryTrackId.parse(TRACK_ID)
        insertTrackAndGoal(owner, trackId)
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = sequenceIds(CHECK_IN_ID, ENTRY_ID, "outbox-parent", "outbox-entry"),
        )

        repository.save(
            DailyCheckInDraft(
                ownerKey = owner,
                localDateEpochDay = 20_000L,
                mood = 4,
                entries = listOf(
                    TrackCheckInDraft(
                        recoveryTrackId = trackId,
                        outcome = TrackCheckInOutcome.GOAL_PARTLY_MET,
                        peakUrge = 4,
                        privateNote = "This must never leave the device.",
                    ),
                ),
            ),
        )

        val queued = database.syncOutboxDao().nextBatch(2_000L, 10)
        assertEquals(
            listOf(SyncAggregateType.DAILY_CHECK_IN, SyncAggregateType.TRACK_CHECK_IN_ENTRY),
            queued.map { it.aggregateType },
        )
        assertEquals(listOf(CHECK_IN_ID, ENTRY_ID), queued.map { it.aggregateId })
        assertTrue(queued.all { it.payload.contains("revision") })
        assertFalse(queued.any { it.payload.contains("This must never leave the device.") })
        assertEquals(SyncState.PENDING, database.dailyCheckInDao().byId(CHECK_IN_ID)?.syncState)
        assertEquals(SyncState.PENDING, database.dailyCheckInDao().entryById(ENTRY_ID)?.syncState)
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
    fun editingAfterGoalChangePreservesOriginalGoalVersion() = runBlocking {
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
        database.recoveryGoalDao().closeCurrent(
            recoveryTrackId = TRACK_ID,
            closedAtEpochMillis = 2_500L,
            syncState = SyncState.LOCAL_ONLY,
        )
        database.recoveryGoalDao().insert(
            RecoveryGoalVersionEntity(
                id = REPLACEMENT_GOAL_ID,
                recoveryTrackId = TRACK_ID,
                goalType = RecoveryGoalType.QUIT_COMPLETELY,
                targetValue = null,
                unitKey = null,
                periodType = null,
                title = null,
                effectiveFromEpochMillis = 2_500L,
                effectiveUntilEpochMillis = null,
                createdAtEpochMillis = 2_500L,
                updatedAtEpochMillis = 2_500L,
                revision = 0,
                syncState = SyncState.LOCAL_ONLY,
            ),
        )
        now = 3_000L
        repository.save(draft(owner, trackId, TrackCheckInOutcome.GOAL_MET))

        val stored = repository.observeForDate(owner, 20_000L).first()
        assertEquals(GOAL_ID, stored?.entries?.single()?.goalVersionId)
    }

    @Test
    fun editingAfterTrackPausePreservesEarlierTrackEntry() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        val primaryTrackId = RecoveryTrackId.parse(TRACK_ID)
        val pausedTrackId = RecoveryTrackId.parse(PAUSED_TRACK_ID)
        insertTrackAndGoal(owner, primaryTrackId)
        insertTrackAndGoal(
            owner = owner,
            trackId = pausedTrackId,
            role = RecoveryTrackRole.SUPPORTING,
        )
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = sequenceIds(CHECK_IN_ID, ENTRY_ID, PAUSED_ENTRY_ID),
        )
        repository.save(
            DailyCheckInDraft(
                ownerKey = owner,
                localDateEpochDay = 20_000L,
                entries = listOf(
                    TrackCheckInDraft(primaryTrackId, TrackCheckInOutcome.GOAL_MET),
                    TrackCheckInDraft(pausedTrackId, TrackCheckInOutcome.GOAL_PARTLY_MET),
                ),
            ),
        )
        val paused = checkNotNull(database.recoveryTrackDao().byId(PAUSED_TRACK_ID)).copy(
            status = RecoveryTrackStatus.PAUSED,
            pausedAtEpochMillis = 2_500L,
            updatedAtEpochMillis = 2_500L,
            revision = 1,
        )
        assertEquals(1, database.recoveryTrackDao().update(paused))

        repository.save(
            DailyCheckInDraft(
                ownerKey = owner,
                localDateEpochDay = 20_000L,
                mood = 5,
                entries = listOf(
                    TrackCheckInDraft(primaryTrackId, TrackCheckInOutcome.GOAL_MET),
                ),
            ),
        )

        val stored = repository.observeForDate(owner, 20_000L).first()
        assertEquals(2, stored?.entries?.size)
        assertEquals(
            TrackCheckInOutcome.GOAL_PARTLY_MET,
            stored?.entries?.first { it.recoveryTrackId == PAUSED_TRACK_ID }?.outcome,
        )
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

    private suspend fun insertTrackAndGoal(
        owner: OwnerKey,
        trackId: RecoveryTrackId,
        role: RecoveryTrackRole = RecoveryTrackRole.PRIMARY,
    ) {
        database.recoveryTrackDao().insert(
            RecoveryTrackEntity(
                id = trackId.value,
                ownerKey = owner.value,
                programId = if (trackId.value == TRACK_ID) "gaming" else "caffeine",
                displayAlias = null,
                role = role,
                status = RecoveryTrackStatus.ACTIVE,
                startedAtEpochMillis = 500L,
                pausedAtEpochMillis = null,
                maintenanceAtEpochMillis = null,
                archivedAtEpochMillis = null,
                createdAtEpochMillis = 500L,
                updatedAtEpochMillis = 500L,
                revision = 0,
                syncState = if (owner.isAuthenticated) SyncState.PENDING else SyncState.LOCAL_ONLY,
            ),
        )
        database.recoveryGoalDao().insert(
            RecoveryGoalVersionEntity(
                id = if (trackId.value == TRACK_ID) GOAL_ID else PAUSED_GOAL_ID,
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
                syncState = if (owner.isAuthenticated) SyncState.PENDING else SyncState.LOCAL_ONLY,
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
        const val REPLACEMENT_GOAL_ID = "00000000-0000-0000-0000-000000000105"
        const val PAUSED_TRACK_ID = "00000000-0000-0000-0000-000000000106"
        const val PAUSED_GOAL_ID = "00000000-0000-0000-0000-000000000107"
        const val PAUSED_ENTRY_ID = "00000000-0000-0000-0000-000000000108"
    }
}
