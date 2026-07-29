package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncState
import com.deaddict.database.repository.EpochClock
import com.deaddict.database.repository.IdGenerator
import com.deaddict.database.repository.LocalRecoveryTrackRepository
import com.deaddict.database.repository.RecoveryGoalDraft
import com.deaddict.database.repository.SyncPolicy
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalRecoveryTrackRepositoryTest {
    private lateinit var database: DeAddictDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            DeAddictDatabase::class.java,
        )
            .addCallback(RECOVERY_TRACK_DATABASE_CALLBACK)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun firstCloudTrackCreatesPrimaryGoalAndOutboxRecords() = runBlocking {
        val repository = repository()
        val owner = OwnerKey.authenticated("user-1")

        val trackId = repository.create(
            ownerKey = owner,
            programId = ProgramId.of("gaming"),
            initialGoal = dailyLimitGoal(),
            syncPolicy = SyncPolicy.CLOUD_ELIGIBLE,
        )

        val track = database.recoveryTrackDao().byId(trackId.value)!!
        val goal = database.recoveryGoalDao().current(trackId.value)!!
        val outbox = database.syncOutboxDao().nextBatch(NOW, 10)

        assertEquals(RecoveryTrackRole.PRIMARY, track.role)
        assertEquals(RecoveryTrackStatus.ACTIVE, track.status)
        assertEquals(SyncState.PENDING, track.syncState)
        assertEquals(RecoveryGoalType.DAILY_LIMIT, goal.goalType)
        assertEquals(
            setOf(SyncAggregateType.RECOVERY_TRACK, SyncAggregateType.RECOVERY_GOAL),
            outbox.mapTo(mutableSetOf()) { it.aggregateType },
        )
    }

    @Test
    fun pausingPrimaryPromotesOldestEligibleSupportingTrack() = runBlocking {
        val repository = repository()
        val owner = OwnerKey.guest("profile-1")
        val primaryId = repository.create(
            owner,
            ProgramId.of("gaming"),
            dailyLimitGoal(),
            SyncPolicy.LOCAL_ONLY,
        )
        val supportingId = repository.create(
            owner,
            ProgramId.of("caffeine"),
            dailyLimitGoal(unit = "cups"),
            SyncPolicy.LOCAL_ONLY,
        )

        repository.pause(owner, primaryId)

        val paused = database.recoveryTrackDao().byId(primaryId.value)!!
        val promoted = database.recoveryTrackDao().byId(supportingId.value)!!
        assertEquals(RecoveryTrackStatus.PAUSED, paused.status)
        assertEquals(RecoveryTrackRole.SUPPORTING, paused.role)
        assertEquals(RecoveryTrackRole.PRIMARY, promoted.role)
        assertEquals(SyncState.LOCAL_ONLY, promoted.syncState)
        assertTrue(database.syncOutboxDao().nextBatch(NOW, 10).isEmpty())
    }

    @Test
    fun archivedJourneyRestartsWithNewIdentityAndPreservedHistory() = runBlocking {
        val repository = repository()
        val owner = OwnerKey.guest("profile-1")
        val originalId = repository.create(
            owner,
            ProgramId.of("gaming"),
            dailyLimitGoal(),
            SyncPolicy.LOCAL_ONLY,
        )

        repository.archive(owner, originalId)
        val restartedId = repository.restart(
            ownerKey = owner,
            archivedTrackId = originalId,
            initialGoal = RecoveryGoalDraft(RecoveryGoalType.AWARENESS_ONLY),
            syncPolicy = SyncPolicy.LOCAL_ONLY,
        )

        assertNotEquals(originalId, restartedId)
        assertEquals(
            RecoveryTrackStatus.ARCHIVED,
            database.recoveryTrackDao().byId(originalId.value)!!.status,
        )
        assertEquals(
            RecoveryTrackStatus.ACTIVE,
            database.recoveryTrackDao().byId(restartedId.value)!!.status,
        )
        assertEquals(1, database.recoveryGoalDao().allForTrack(originalId.value).size)
        assertEquals(1, database.recoveryGoalDao().allForTrack(restartedId.value).size)
    }

    @Test
    fun changingGoalClosesPreviousVersionWithoutRewritingHistory() = runBlocking {
        val repository = repository()
        val owner = OwnerKey.guest("profile-1")
        val trackId = repository.create(
            owner,
            ProgramId.of("gaming"),
            dailyLimitGoal(),
            SyncPolicy.LOCAL_ONLY,
        )

        val replacement = repository.changeGoal(
            owner,
            trackId,
            RecoveryGoalDraft(RecoveryGoalType.AWARENESS_ONLY),
        )

        val history = database.recoveryGoalDao().allForTrack(trackId.value)
        assertEquals(2, history.size)
        assertFalse(history.first().effectiveUntilEpochMillis == null)
        assertEquals(replacement.id.value, history.last().id)
        assertNull(history.last().effectiveUntilEpochMillis)
    }

    @Test
    fun ownerReconciliationDemotesSourcePrimaryWhenDestinationAlreadyHasOne() = runBlocking {
        val repository = repository()
        val legacy = OwnerKey.legacyLocal()
        val user = OwnerKey.authenticated("user-1")
        val sourceId = repository.create(
            legacy,
            ProgramId.of("gaming"),
            dailyLimitGoal(),
            SyncPolicy.LOCAL_ONLY,
        )
        repository.create(
            user,
            ProgramId.of("caffeine"),
            dailyLimitGoal(unit = "cups"),
            SyncPolicy.LOCAL_ONLY,
        )

        val moved = repository.reconcileOwner(
            from = legacy,
            to = user,
            syncPolicy = SyncPolicy.CLOUD_ELIGIBLE,
        )

        val adopted = database.recoveryTrackDao().byId(sourceId.value)!!
        val adoptedGoal = database.recoveryGoalDao().current(sourceId.value)!!
        val openTracks = repository.observeOpen(user).first()
        val outbox = database.syncOutboxDao().nextBatch(NOW, 20)

        assertEquals(1, moved)
        assertEquals(user.value, adopted.ownerKey)
        assertEquals(RecoveryTrackRole.SUPPORTING, adopted.role)
        assertEquals(SyncState.PENDING, adopted.syncState)
        assertEquals(SyncState.PENDING, adoptedGoal.syncState)
        assertEquals(1, openTracks.count { it.role == RecoveryTrackRole.PRIMARY })
        assertEquals(2, outbox.size)
    }

    private fun repository(): LocalRecoveryTrackRepository {
        val generatedIds = ArrayDeque((1..80).map(::uuid))
        return LocalRecoveryTrackRepository(
            database = database,
            clock = EpochClock { NOW },
            ids = IdGenerator { generatedIds.removeFirst() },
        )
    }

    private fun dailyLimitGoal(unit: String = "minutes") = RecoveryGoalDraft(
        goalType = RecoveryGoalType.DAILY_LIMIT,
        targetValue = 60.0,
        unitKey = unit,
        periodType = GoalPeriodType.DAY,
    )

    private companion object {
        const val NOW = 10_000L

        fun uuid(index: Int): String =
            "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"
    }
}
