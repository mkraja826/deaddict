package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyCheckInDeletionSyncTest {
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
    fun authenticatedDeleteSupersedesWritesAndQueuesDateTombstone() = runBlocking {
        val owner = OwnerKey.authenticated("user-1")
        val trackId = RecoveryTrackId.parse(TRACK_ID)
        insertTrackAndGoal(owner, trackId)
        val generatedIds = ArrayDeque(
            listOf(
                CHECK_IN_ID,
                ENTRY_ID,
                "outbox-parent",
                "outbox-entry",
                "outbox-delete",
            ),
        )
        val repository = LocalDailyCheckInRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = IdGenerator { generatedIds.removeFirst() },
        )
        repository.save(
            DailyCheckInDraft(
                ownerKey = owner,
                localDateEpochDay = LOCAL_DATE_EPOCH_DAY,
                entries = listOf(
                    TrackCheckInDraft(
                        recoveryTrackId = trackId,
                        outcome = TrackCheckInOutcome.GOAL_MET,
                    ),
                ),
            ),
        )

        assertEquals(true, repository.delete(owner, CHECK_IN_ID))

        assertNull(database.dailyCheckInDao().byId(CHECK_IN_ID))
        val pending = database.syncOutboxDao().nextBatch(2_000L, 10)
        assertEquals(1, pending.size)
        assertEquals(SyncAggregateType.DAILY_CHECK_IN, pending.single().aggregateType)
        assertEquals(SyncOperation.DELETE, pending.single().operation)
        assertEquals(LOCAL_DATE_EPOCH_DAY.toString(), pending.single().aggregateId)
    }

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
                syncState = SyncState.PENDING,
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
                syncState = SyncState.PENDING,
            ),
        )
    }

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000201"
        const val GOAL_ID = "00000000-0000-0000-0000-000000000202"
        const val CHECK_IN_ID = "00000000-0000-0000-0000-000000000203"
        const val ENTRY_ID = "00000000-0000-0000-0000-000000000204"
        const val LOCAL_DATE_EPOCH_DAY = 20_000L
    }
}
