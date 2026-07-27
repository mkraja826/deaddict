package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.repository.EpochClock
import com.deaddict.database.repository.IdGenerator
import com.deaddict.database.repository.LocalTrackingRepository
import com.deaddict.database.repository.NewTrackingEvent
import com.deaddict.database.repository.SyncPolicy
import com.deaddict.database.entity.TrackingEventKind
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
            ),
            syncPolicy = SyncPolicy.CLOUD_ELIGIBLE,
        )

        val stored = database.trackingDao().observeForProgram("gaming").first()
        val queued = database.syncOutboxDao().nextBatch(2_000L, 10)
        assertEquals("must stay local", stored.single().privateNote)
        assertEquals(1, queued.size)
        assertTrue(queued.single().payload.contains("event-1"))
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
}
