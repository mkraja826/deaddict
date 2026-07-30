package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.DailyCheckInDraftEntity
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.database.repository.EpochClock
import com.deaddict.database.repository.IdGenerator
import com.deaddict.database.repository.LocalDailyCheckInRepository
import com.deaddict.database.repository.NewDailyCheckIn
import com.deaddict.database.repository.NewTrackCheckInEntry
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        )
            .addCallback(RECOVERY_TRACK_DATABASE_CALLBACK)
            .addCallback(RECOVERY_EVENT_DATABASE_CALLBACK)
            .addCallback(DAILY_CHECK_IN_DATABASE_CALLBACK)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completePersistsOneParentAndIndependentTrackEntriesAtomically() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        insertTrackAndGoal(owner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        insertTrackAndGoal(owner, SUPPORTING_TRACK_ID, "caffeine", RecoveryTrackRole.SUPPORTING)
        database.dailyCheckInDraftDao().upsert(
            DailyCheckInDraftEntity(
                ownerKey = owner.value,
                localDate = LOCAL_DATE,
                timezoneId = TIMEZONE,
                payloadJson = "{\"draft\":true}",
                updatedAtEpochMillis = 1_500L,
            ),
        )
        val generatedIds = ArrayDeque(listOf("check-in-1", "entry-1", "entry-2"))
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = IdGenerator { generatedIds.removeFirst() },
        )

        val id = repository.complete(
            NewDailyCheckIn(
                ownerKey = owner,
                localDate = LOCAL_DATE,
                timezoneId = TIMEZONE,
                mood = 3,
                stress = 4,
                sharedTriggerKeys = listOf("stress", "stress"),
                privateNote = "shared note stays local",
                entries = listOf(
                    NewTrackCheckInEntry(
                        recoveryTrackId = RecoveryTrackId.parse(PRIMARY_TRACK_ID),
                        programId = ProgramId.of("gaming"),
                        outcome = TrackCheckInOutcome.URGE_MANAGED,
                        durationMinutes = 35,
                        urgeIntensity = 4,
                        privateNote = "gaming note stays local",
                    ),
                    NewTrackCheckInEntry(
                        recoveryTrackId = RecoveryTrackId.parse(SUPPORTING_TRACK_ID),
                        programId = ProgramId.of("caffeine"),
                        outcome = TrackCheckInOutcome.OBSERVED,
                        quantity = 2.0,
                        unit = "cups",
                    ),
                ),
            ),
        )

        val stored = database.dailyCheckInDao().byOwnerDate(owner.value, LOCAL_DATE)
        assertEquals("check-in-1", id)
        assertNotNull(stored)
        assertEquals(listOf("stress"), stored!!.checkIn.sharedTriggerKeys)
        assertEquals("shared note stays local", stored.checkIn.privateNote)
        assertEquals(2, stored.entries.size)
        assertEquals(
            setOf(PRIMARY_TRACK_ID, SUPPORTING_TRACK_ID),
            stored.entries.map { it.recoveryTrackId }.toSet(),
        )
        assertTrue(stored.entries.all { it.goalVersionId != null })
        assertTrue(stored.entries.all { it.syncState == SyncState.LOCAL_ONLY })
        assertNull(database.dailyCheckInDraftDao().get(owner.value, LOCAL_DATE))
        assertTrue(database.syncOutboxDao().nextBatch(2_000L, 10).isEmpty())
    }

    @Test
    fun duplicateOwnerDateIsRejectedWithoutCreatingMoreEntries() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        insertTrackAndGoal(owner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        val generatedIds = ArrayDeque(listOf("check-in-1", "entry-1", "check-in-2"))
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = IdGenerator { generatedIds.removeFirst() },
        )
        val input = singleTrackInput(owner)

        repository.complete(input)
        val duplicate = runCatching { repository.complete(input) }

        assertTrue(duplicate.isFailure)
        val stored = database.dailyCheckInDao().byOwnerDate(owner.value, LOCAL_DATE)
        assertEquals(1, stored?.entries?.size)
    }

    @Test
    fun allNotTrackedEntriesAreRejected() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        insertTrackAndGoal(owner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = IdGenerator { "unused" },
        )

        val result = runCatching {
            repository.complete(
                singleTrackInput(owner).copy(
                    entries = listOf(
                        NewTrackCheckInEntry(
                            recoveryTrackId = RecoveryTrackId.parse(PRIMARY_TRACK_ID),
                            programId = ProgramId.of("gaming"),
                            outcome = TrackCheckInOutcome.NOT_TRACKED,
                        ),
                    ),
                ),
            )
        }

        assertTrue(result.isFailure)
        assertNull(database.dailyCheckInDao().byOwnerDate(owner.value, LOCAL_DATE))
    }

    @Test
    fun anotherOwnersTrackCannotReceiveTheCheckIn() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        val otherOwner = OwnerKey.guest("profile-2")
        insertTrackAndGoal(otherOwner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = IdGenerator { "unused" },
        )

        val result = runCatching { repository.complete(singleTrackInput(owner)) }

        assertTrue(result.isFailure)
        assertNull(database.dailyCheckInDao().byOwnerDate(owner.value, LOCAL_DATE))
    }

    private fun singleTrackInput(owner: OwnerKey): NewDailyCheckIn = NewDailyCheckIn(
        ownerKey = owner,
        localDate = LOCAL_DATE,
        timezoneId = TIMEZONE,
        entries = listOf(
            NewTrackCheckInEntry(
                recoveryTrackId = RecoveryTrackId.parse(PRIMARY_TRACK_ID),
                programId = ProgramId.of("gaming"),
                outcome = TrackCheckInOutcome.ALIGNED,
            ),
        ),
    )

    private suspend fun insertTrackAndGoal(
        owner: OwnerKey,
        trackId: String,
        programId: String,
        role: RecoveryTrackRole,
    ) {
        database.recoveryTrackDao().insert(
            RecoveryTrackEntity(
                id = trackId,
                ownerKey = owner.value,
                programId = programId,
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
                syncState = SyncState.LOCAL_ONLY,
            ),
        )
        database.recoveryGoalDao().insert(
            RecoveryGoalVersionEntity(
                id = "goal-$trackId",
                recoveryTrackId = trackId,
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

    private companion object {
        const val PRIMARY_TRACK_ID = "00000000-0000-0000-0000-000000000101"
        const val SUPPORTING_TRACK_ID = "00000000-0000-0000-0000-000000000102"
        const val LOCAL_DATE = "2026-07-30"
        const val TIMEZONE = "Asia/Kolkata"
    }
}
