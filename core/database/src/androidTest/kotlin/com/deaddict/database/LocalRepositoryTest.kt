package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.database.repository.EpochClock
import com.deaddict.database.repository.IdGenerator
import com.deaddict.database.repository.LocalTrackingRepository
import com.deaddict.database.repository.NewTrackingEvent
import com.deaddict.database.repository.SyncPolicy
import com.deaddict.model.OwnerKey
import com.deaddict.model.RecoveryTrackId
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.programs.ProgramId
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
class LocalRepositoryTest {
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
    fun cloudEligibleWriteStoresLocallyAndQueuesSanitizedPayload() = runBlocking {
        val ids = ArrayDeque(listOf("event-1", "outbox-1"))
        val owner = OwnerKey.authenticated("user-1")
        val trackId = RecoveryTrackId.parse(TRACK_ID)
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
        val repository = LocalTrackingRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = IdGenerator { ids.removeFirst() },
        )

        repository.record(
            input = NewTrackingEvent(
                programId = ProgramId.of("gaming"),
                kind = TrackingEventKind.URGE,
                urgeIntensity = 4,
                triggerKey = "stress",
                occurredAtEpochMillis = 1_000L,
                privateNote = "must stay local",
                ownerKey = owner,
                recoveryTrackId = trackId,
            ),
            syncPolicy = SyncPolicy.CLOUD_ELIGIBLE,
        )

        val stored = database.trackingDao().observeForTrack(TRACK_ID, limit = 100).first()
        val queued = database.syncOutboxDao().nextBatch(2_000L, 10)
        assertEquals("must stay local", stored.single().privateNote)
        assertEquals(owner.value, stored.single().ownerKey)
        assertEquals(TRACK_ID, stored.single().recoveryTrackId)
        assertEquals(1, queued.size)
        assertTrue(queued.single().payload.contains("event-1"))
        assertTrue(queued.single().payload.contains(TRACK_ID))
        assertFalse(queued.single().payload.contains("must stay local"))
    }

    @Test
    fun localOnlyWriteNeverCreatesOutboxEntry() = runBlocking {
        val repository = LocalTrackingRepository(
            database = database,
            clock = EpochClock { 2_000L },
            ids = IdGenerator { "local-event" },
        )

        repository.record(
            NewTrackingEvent(
                programId = ProgramId.of("caffeine"),
                kind = TrackingEventKind.QUANTITY,
                quantity = 1.0,
                unit = "cup",
                triggerKey = "routine",
                occurredAtEpochMillis = 1_000L,
            ),
            SyncPolicy.LOCAL_ONLY,
        )

        assertTrue(database.syncOutboxDao().nextBatch(2_000L, 10).isEmpty())
    }

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000101"
    }
}
