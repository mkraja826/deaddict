package com.deaddict.app.sync

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncProcessorTest {
    @Test
    fun `successful upload completes the claimed item`() = runBlocking {
        val item = trackingItem()
        val store = FakeSyncStore(
            items = mutableListOf(item),
            tracking = trackingEvent(),
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.SUCCESS, result)
        assertEquals(listOf("event-1"), remote.uploadedTrackingIds)
        assertEquals(listOf("outbox-1"), store.completedIds)
        assertTrue(store.failures.isEmpty())
    }

    @Test
    fun `signed out user leaves pending work untouched`() = runBlocking {
        val store = FakeSyncStore(mutableListOf(trackingItem()), trackingEvent())
        val remote = FakeRemoteSyncGateway(userId = null)

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.SIGNED_OUT, result)
        assertTrue(store.claimedIds.isEmpty())
        assertTrue(remote.uploadedTrackingIds.isEmpty())
    }

    @Test
    fun `missing local row is dead lettered`() = runBlocking {
        val store = FakeSyncStore(mutableListOf(trackingItem()), tracking = null)

        val result = SyncProcessor(store, FakeRemoteSyncGateway()).runBatch()

        assertEquals(SyncRunResult.IDLE, result)
        assertEquals(
            listOf(Failure("outbox-1", "LOCAL_ROW_MISSING", permanent = true)),
            store.failures,
        )
    }

    @Test
    fun `remote failure requests retry`() = runBlocking {
        val store = FakeSyncStore(mutableListOf(trackingItem()), trackingEvent())
        val remote = FakeRemoteSyncGateway(failWrites = true)

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.RETRY, result)
        assertEquals(
            listOf(Failure("outbox-1", "REMOTE_WRITE_FAILED", permanent = false)),
            store.failures,
        )
        assertFalse(store.completedIds.contains("outbox-1"))
    }
}

private data class Failure(
    val itemId: String,
    val code: String,
    val permanent: Boolean,
)

private class FakeSyncStore(
    private val items: MutableList<SyncOutboxEntity>,
    private val tracking: TrackingEventEntity?,
) : SyncStore {
    val claimedIds = mutableListOf<String>()
    val completedIds = mutableListOf<String>()
    val failures = mutableListOf<Failure>()

    override suspend fun resetInterruptedClaims() = Unit

    override suspend fun nextBatch(limit: Int): List<SyncOutboxEntity> = items.take(limit)

    override suspend fun claim(id: String): Boolean {
        claimedIds += id
        return true
    }

    override suspend fun program(id: String): ActiveProgramEntity? = null

    override suspend fun trackingEvent(id: String): TrackingEventEntity? = tracking

    override suspend fun rescueSession(id: String): RescueSessionEntity? = null

    override suspend fun complete(item: SyncOutboxEntity) {
        completedIds += item.id
    }

    override suspend fun fail(item: SyncOutboxEntity, errorCode: String, permanent: Boolean) {
        failures += Failure(item.id, errorCode, permanent)
    }
}

private class FakeRemoteSyncGateway(
    private val userId: String? = "user-1",
    private val failWrites: Boolean = false,
) : RemoteSyncGateway {
    override val available: Boolean = true
    val uploadedTrackingIds = mutableListOf<String>()

    override suspend fun currentUserId(): String? = userId

    override suspend fun upsertProgram(userId: String, program: ActiveProgramEntity) = Unit

    override suspend fun upsertTrackingEvent(userId: String, event: TrackingEventEntity) {
        if (failWrites) error("offline")
        uploadedTrackingIds += event.id
    }

    override suspend fun upsertRescueSession(userId: String, session: RescueSessionEntity) = Unit

    override suspend fun downloadSnapshot(): CloudSnapshot =
        CloudSnapshot(emptyList(), emptyList(), emptyList())
}

private fun trackingItem() = SyncOutboxEntity(
    id = "outbox-1",
    idempotencyKey = "TRACKING_EVENT:event-1:UPSERT",
    aggregateType = SyncAggregateType.TRACKING_EVENT,
    aggregateId = "event-1",
    operation = SyncOperation.UPSERT,
    payload = "{}",
    createdAtEpochMillis = 1_000L,
    nextAttemptAtEpochMillis = 1_000L,
)

private fun trackingEvent() = TrackingEventEntity(
    id = "event-1",
    programId = "gaming",
    kind = TrackingEventKind.URGE,
    quantity = null,
    unit = null,
    costMinorUnits = null,
    urgeIntensity = 4,
    triggerKey = "stress",
    occurredAtEpochMillis = 1_000L,
    createdAtEpochMillis = 1_100L,
    privateNote = "never upload this",
    syncState = SyncState.PENDING,
)
