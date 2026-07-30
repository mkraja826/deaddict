package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.repository.CompleteDailyCheckInInput
import com.deaddict.database.repository.DailyCheckInDraft
import com.deaddict.database.repository.EpochClock
import com.deaddict.database.repository.IdGenerator
import com.deaddict.database.repository.LocalDailyCheckInRepository
import com.deaddict.database.repository.TrackCheckInInput
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.model.TrackCheckInOutcome
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalDailyCheckInRepositoryTest {
    private lateinit var database: DeAddictDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            DeAddictDatabase::class.java,
        )
            .addCallback(DAILY_CHECK_IN_DATABASE_CALLBACK)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completePersistsOneParentAndIndependentTrackEntries() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        insertTrack(owner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        insertTrack(owner, SUPPORTING_TRACK_ID, "caffeine", RecoveryTrackRole.SUPPORTING)
        insertGoal(PRIMARY_TRACK_ID, PRIMARY_GOAL_ID)
        insertGoal(SUPPORTING_TRACK_ID, SUPPORTING_GOAL_ID)
        val ids = ArrayDeque(listOf(CHECK_IN_ID, PRIMARY_ENTRY_ID, SUPPORTING_ENTRY_ID))
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 3_000L },
            ids = IdGenerator { ids.removeFirst() },
        )

        val savedId = repository.complete(
            CompleteDailyCheckInInput(
                ownerKey = owner,
                localDate = LocalDate.parse("2026-07-30"),
                timezoneId = "Asia/Kolkata",
                mood = 4,
                stress = 2,
                triggerKeys = setOf("routine", "stress"),
                privateNote = "shared private note",
                entries = listOf(
                    TrackCheckInInput(
                        recoveryTrackId = RecoveryTrackId.parse(PRIMARY_TRACK_ID),
                        outcome = TrackCheckInOutcome.URGE_MANAGED,
                        urgeIntensity = 4,
                        durationMinutes = 35,
                        triggerKeys = setOf("stress"),
                    ),
                    TrackCheckInInput(
                        recoveryTrackId = RecoveryTrackId.parse(SUPPORTING_TRACK_ID),
                        outcome = TrackCheckInOutcome.OBSERVED,
                        quantity = 2.0,
                        quantityUnit = "cups",
                    ),
                ),
            ),
        )

        assertEquals(CHECK_IN_ID, savedId.value)
        val stored = repository.getForDate(owner, LocalDate.parse("2026-07-30"))
        assertNotNull(stored)
        assertEquals("shared private note", stored!!.checkIn.privateNote)
        assertEquals(setOf("routine", "stress"), stored.checkIn.triggerKeys)
        assertEquals(2, stored.entries.size)
        assertEquals(
            setOf(PRIMARY_GOAL_ID, SUPPORTING_GOAL_ID),
            stored.entries.mapNotNull { it.recoveryGoalVersionId?.value }.toSet(),
        )
        assertEquals(
            setOf(TrackCheckInOutcome.URGE_MANAGED, TrackCheckInOutcome.OBSERVED),
            stored.entries.map { it.outcome }.toSet(),
        )
        assertTrue(database.syncOutboxDao().nextBatch(3_000L, 10).isEmpty())
    }

    @Test
    fun duplicateOwnerDateIsRejectedWithoutPartialSecondAggregate() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        insertTrack(owner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        insertGoal(PRIMARY_TRACK_ID, PRIMARY_GOAL_ID)
        val ids = ArrayDeque(listOf(CHECK_IN_ID, PRIMARY_ENTRY_ID, SECOND_CHECK_IN_ID, SECOND_ENTRY_ID))
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 3_000L },
            ids = IdGenerator { ids.removeFirst() },
        )
        val input = CompleteDailyCheckInInput(
            ownerKey = owner,
            localDate = LocalDate.parse("2026-07-30"),
            timezoneId = "Asia/Kolkata",
            entries = listOf(
                TrackCheckInInput(
                    recoveryTrackId = RecoveryTrackId.parse(PRIMARY_TRACK_ID),
                    outcome = TrackCheckInOutcome.ALIGNED,
                ),
            ),
        )

        repository.complete(input)
        val duplicate = runCatching { repository.complete(input) }

        assertTrue(duplicate.isFailure)
        val recent = repository.observeRecent(owner).first()
        assertEquals(1, recent.size)
        assertEquals(1, recent.single().entries.size)
    }

    @Test
    fun anotherOwnersTrackIsRejectedAndDraftRemains() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        val anotherOwner = OwnerKey.guest("profile-2")
        insertTrack(anotherOwner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        insertGoal(PRIMARY_TRACK_ID, PRIMARY_GOAL_ID)
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 3_000L },
            ids = IdGenerator { CHECK_IN_ID },
        )
        repository.saveDraft(
            DailyCheckInDraft(
                ownerKey = owner,
                localDate = LocalDate.parse("2026-07-30"),
                timezoneId = "Asia/Kolkata",
                trackEntriesPayload = "draft-payload",
                updatedAtEpochMillis = 2_000L,
            ),
        )

        val result = runCatching {
            repository.complete(
                CompleteDailyCheckInInput(
                    ownerKey = owner,
                    localDate = LocalDate.parse("2026-07-30"),
                    timezoneId = "Asia/Kolkata",
                    entries = listOf(
                        TrackCheckInInput(
                            recoveryTrackId = RecoveryTrackId.parse(PRIMARY_TRACK_ID),
                            outcome = TrackCheckInOutcome.ALIGNED,
                        ),
                    ),
                ),
            )
        }

        assertTrue(result.isFailure)
        assertNotNull(repository.loadDraft(owner))
        assertNull(repository.getForDate(owner, LocalDate.parse("2026-07-30")))
    }

    @Test
    fun successfulCompletionDeletesTheOwnersDraft() = runBlocking {
        val owner = OwnerKey.guest("profile-1")
        insertTrack(owner, PRIMARY_TRACK_ID, "gaming", RecoveryTrackRole.PRIMARY)
        insertGoal(PRIMARY_TRACK_ID, PRIMARY_GOAL_ID)
        val ids = ArrayDeque(listOf(CHECK_IN_ID, PRIMARY_ENTRY_ID))
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 3_000L },
            ids = IdGenerator { ids.removeFirst() },
        )
        repository.saveDraft(
            DailyCheckInDraft(
                ownerKey = owner,
                localDate = LocalDate.parse("2026-07-30"),
                timezoneId = "Asia/Kolkata",
                trackEntriesPayload = "draft-payload",
                updatedAtEpochMillis = 2_000L,
            ),
        )

        repository.complete(
            CompleteDailyCheckInInput(
                ownerKey = owner,
                localDate = LocalDate.parse("2026-07-30"),
                timezoneId = "Asia/Kolkata",
                entries = listOf(
                    TrackCheckInInput(
                        recoveryTrackId = RecoveryTrackId.parse(PRIMARY_TRACK_ID),
                        outcome = TrackCheckInOutcome.ALIGNED,
                    ),
                ),
            ),
        )

        assertNull(repository.loadDraft(owner))
        assertFalse(repository.deleteDraft(owner))
    }

    private suspend fun insertTrack(
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
    }

    private suspend fun insertGoal(trackId: String, goalId: String) {
        database.recoveryGoalDao().insert(
            RecoveryGoalVersionEntity(
                id = goalId,
                recoveryTrackId = trackId,
                goalType = RecoveryGoalType.AWARENESS_ONLY,
                targetValue = null,
                unitKey = null,
                periodType = GoalPeriodType.DAY,
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
        const val CHECK_IN_ID = "00000000-0000-0000-0000-000000000301"
        const val PRIMARY_ENTRY_ID = "00000000-0000-0000-0000-000000000302"
        const val SUPPORTING_ENTRY_ID = "00000000-0000-0000-0000-000000000303"
        const val SECOND_CHECK_IN_ID = "00000000-0000-0000-0000-000000000304"
        const val SECOND_ENTRY_ID = "00000000-0000-0000-0000-000000000305"
        const val PRIMARY_TRACK_ID = "00000000-0000-0000-0000-000000000101"
        const val SUPPORTING_TRACK_ID = "00000000-0000-0000-0000-000000000102"
        const val PRIMARY_GOAL_ID = "00000000-0000-0000-0000-000000000201"
        const val SUPPORTING_GOAL_ID = "00000000-0000-0000-0000-000000000202"
    }
}
