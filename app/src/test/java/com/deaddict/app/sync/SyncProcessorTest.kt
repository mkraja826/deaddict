package com.deaddict.app.sync

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
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
    fun `Recovery Track upload uses its dedicated remote operation`() = runBlocking {
        val item = recoveryTrackItem()
        val store = FakeSyncStore(
            items = mutableListOf(item),
            track = recoveryTrack(),
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.SUCCESS, result)
        assertEquals(listOf(TRACK_ID), remote.uploadedTrackIds)
        assertEquals(listOf("outbox-track-1"), store.completedIds)
    }

    @Test
    fun `tracking row without Recovery Track is dead lettered`() = runBlocking {
        val store = FakeSyncStore(
            items = mutableListOf(trackingItem()),
            tracking = trackingEvent().copy(recoveryTrackId = null),
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.IDLE, result)
        assertEquals(
            listOf(Failure("outbox-1", "RECOVERY_TRACK_MISSING", permanent = true)),
            store.failures,
        )
        assertTrue(remote.uploadedTrackingIds.isEmpty())
    }

    @Test
    fun `tracking row from another account is dead lettered`() = runBlocking {
        val store = FakeSyncStore(
            items = mutableListOf(trackingItem()),
            tracking = trackingEvent().copy(ownerKey = "user:another-user"),
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.IDLE, result)
        assertEquals(
            listOf(Failure("outbox-1", "ACCOUNT_SCOPE_MISMATCH", permanent = true)),
            store.failures,
        )
        assertTrue(remote.uploadedTrackingIds.isEmpty())
    }

    @Test
    fun `successful delete does not require a local row`() = runBlocking {
        val item = trackingDeleteItem()
        val store = FakeSyncStore(
            items = mutableListOf(item),
            tracking = null,
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.SUCCESS, result)
        assertEquals(
            listOf(SyncAggregateType.TRACKING_EVENT to "event-1"),
            remote.deletedRecords,
        )
        assertEquals(listOf("outbox-delete-1"), store.completedIds)
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

    @Test
    fun `remote delete failure requests retry`() = runBlocking {
        val store = FakeSyncStore(mutableListOf(trackingDeleteItem()), tracking = null)
        val remote = FakeRemoteSyncGateway(failWrites = true)

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.RETRY, result)
        assertEquals(
            listOf(Failure("outbox-delete-1", "REMOTE_WRITE_FAILED", permanent = false)),
            store.failures,
        )
        assertTrue(remote.deletedRecords.isEmpty())
    }
}

private data class Failure(
    val itemId: String,
    val code: String,
    val permanent: Boolean,
)

private class FakeSyncStore(
    private val items: MutableList<SyncOutboxEntity>,
    private val tracking: TrackingEventEntity? = null,
    private val track: RecoveryTrackEntity? = null,
    private val goal: RecoveryGoalVersionEntity? = null,
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

    override suspend fun recoveryTrack(id: String): RecoveryTrackEntity? = track

    override suspend fun recoveryGoal(id: String): RecoveryGoalVersionEntity? = goal

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
    val uploadedTrackIds = mutableListOf<String>()
    val deletedRecords = mutableListOf<Pair<SyncAggregateType, String>>()

    override suspend fun currentUserId(): String? = userId

    override suspend fun upsertProgram(userId: String, program: ActiveProgramEntity) = Unit

    override suspend fun upsertRecoveryTrack(userId: String, track: RecoveryTrackEntity) {
        if (failWrites) error("offline")
        uploadedTrackIds += track.id
    }

    override suspend fun upsertRecoveryGoal(userId: String, goal: RecoveryGoalVersionEntity) = Unit

    override suspend fun upsertTrackingEvent(userId: String, event: TrackingEventEntity) {
        if (failWrites) error("offline")
        uploadedTrackingIds += event.id
    }

    override suspend fun upsertRescueSession(userId: String, session: RescueSessionEntity) = Unit

    override suspend fun deleteRecord(
        userId: String,
        aggregateType: SyncAggregateType,
        aggregateId: String,
    ) {
        if (failWrites) error("offline")
        deletedRecords += aggregateType to aggregateId
    }

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

private fun recoveryTrackItem() = SyncOutboxEntity(
    id = "outbox-track-1",
    idempotencyKey = "RECOVERY_TRACK:$TRACK_ID:UPSERT:0",
    aggregateType = SyncAggregateType.RECOVERY_TRACK,
    aggregateId = TRACK_ID,
    operation = SyncOperation.UPSERT,
    payload = "{}",
    createdAtEpochMillis = 1_000L,
    nextAttemptAtEpochMillis = 1_000L,
)

private fun trackingDeleteItem() = SyncOutboxEntity(
    id = "outbox-delete-1",
    idempotencyKey = "TRACKING_EVENT:event-1:DELETE",
    aggregateType = SyncAggregateType.TRACKING_EVENT,
    aggregateId = "event-1",
    operation = SyncOperation.DELETE,
    payload = "{}",
    createdAtEpochMillis = 2_000L,
    nextAttemptAtEpochMillis = 2_000L,
)

private fun trackingEvent() = TrackingEventEntity(
    id = "event-1",
    ownerKey = "user:user-1",
    recoveryTrackId = TRACK_ID,
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

private fun recoveryTrack() = RecoveryTrackEntity(
    id = TRACK_ID,
    ownerKey = "user:user-1",
    programId = "gaming",
    displayAlias = null,
    role = RecoveryTrackRole.PRIMARY,
    status = RecoveryTrackStatus.ACTIVE,
    startedAtEpochMillis = 1_000L,
    pausedAtEpochMillis = null,
    maintenanceAtEpochMillis = null,
    archivedAtEpochMillis = null,
    createdAtEpochMillis = 1_000L,
    updatedAtEpochMillis = 1_000L,
    revision = 0,
    syncState = SyncState.PENDING,
)

private const val TRACK_ID = "7ebdbd0b-4676-45f1-82cd-e632b3ec6092"
