package com.deaddict.app.sync

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackCheckInEntryEntity
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.database.entity.TrackingEventEntity
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.model.RecoveryGoalType
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
    fun `daily check-in uploads parent before track entry`() = runBlocking {
        val store = FakeSyncStore(
            items = mutableListOf(dailyCheckInItem(), trackCheckInEntryItem()),
            track = recoveryTrack(),
            goal = recoveryGoal(),
            dailyCheckIn = dailyCheckIn(),
            trackCheckInEntry = trackCheckInEntry(),
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.SUCCESS, result)
        assertEquals(
            listOf("daily:$CHECK_IN_ID", "entry:$ENTRY_ID:$LOCAL_DATE_EPOCH_DAY"),
            remote.dailyUploadOrder,
        )
        assertEquals(listOf("outbox-daily-1", "outbox-entry-1"), store.completedIds)
        assertTrue(store.failures.isEmpty())
    }

    @Test
    fun `daily check-in from another account is dead lettered`() = runBlocking {
        val store = FakeSyncStore(
            items = mutableListOf(dailyCheckInItem()),
            dailyCheckIn = dailyCheckIn().copy(ownerKey = "user:another-user"),
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.IDLE, result)
        assertEquals(
            listOf(Failure("outbox-daily-1", "ACCOUNT_SCOPE_MISMATCH", permanent = true)),
            store.failures,
        )
        assertTrue(remote.dailyUploadOrder.isEmpty())
    }

    @Test
    fun `track check-in entry with mismatched goal is dead lettered`() = runBlocking {
        val store = FakeSyncStore(
            items = mutableListOf(trackCheckInEntryItem()),
            track = recoveryTrack(),
            goal = recoveryGoal().copy(recoveryTrackId = OTHER_TRACK_ID),
            dailyCheckIn = dailyCheckIn(),
            trackCheckInEntry = trackCheckInEntry(),
        )
        val remote = FakeRemoteSyncGateway()

        val result = SyncProcessor(store, remote).runBatch()

        assertEquals(SyncRunResult.IDLE, result)
        assertEquals(
            listOf(
                Failure(
                    "outbox-entry-1",
                    "RECOVERY_GOAL_SCOPE_MISMATCH",
                    permanent = true,
                ),
            ),
            store.failures,
        )
        assertTrue(remote.dailyUploadOrder.isEmpty())
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
    private val dailyCheckIn: DailyCheckInEntity? = null,
    private val trackCheckInEntry: TrackCheckInEntryEntity? = null,
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

    override suspend fun dailyCheckIn(id: String): DailyCheckInEntity? = dailyCheckIn

    override suspend fun trackCheckInEntry(id: String): TrackCheckInEntryEntity? = trackCheckInEntry

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
    val dailyUploadOrder = mutableListOf<String>()
    val deletedRecords = mutableListOf<Pair<SyncAggregateType, String>>()

    override suspend fun currentUserId(): String? = userId

    override suspend fun upsertProgram(userId: String, program: ActiveProgramEntity) = Unit

    override suspend fun upsertRecoveryTrack(userId: String, track: RecoveryTrackEntity) {
        if (failWrites) error("offline")
        uploadedTrackIds += track.id
    }

    override suspend fun upsertRecoveryGoal(userId: String, goal: RecoveryGoalVersionEntity) = Unit

    override suspend fun upsertDailyCheckIn(userId: String, checkIn: DailyCheckInEntity) {
        if (failWrites) error("offline")
        dailyUploadOrder += "daily:${checkIn.id}"
    }

    override suspend fun upsertTrackCheckInEntry(
        userId: String,
        checkIn: DailyCheckInEntity,
        entry: TrackCheckInEntryEntity,
    ) {
        if (failWrites) error("offline")
        dailyUploadOrder += "entry:${entry.id}:${checkIn.localDateEpochDay}"
    }

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

private fun dailyCheckInItem() = SyncOutboxEntity(
    id = "outbox-daily-1",
    idempotencyKey = "DAILY_CHECK_IN:$CHECK_IN_ID:UPSERT:0",
    aggregateType = SyncAggregateType.DAILY_CHECK_IN,
    aggregateId = CHECK_IN_ID,
    operation = SyncOperation.UPSERT,
    payload = "{}",
    createdAtEpochMillis = 1_000L,
    nextAttemptAtEpochMillis = 1_000L,
)

private fun trackCheckInEntryItem() = SyncOutboxEntity(
    id = "outbox-entry-1",
    idempotencyKey = "TRACK_CHECK_IN_ENTRY:$ENTRY_ID:UPSERT:0",
    aggregateType = SyncAggregateType.TRACK_CHECK_IN_ENTRY,
    aggregateId = ENTRY_ID,
    operation = SyncOperation.UPSERT,
    payload = "{}",
    createdAtEpochMillis = 1_001L,
    nextAttemptAtEpochMillis = 1_001L,
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

private fun recoveryGoal() = RecoveryGoalVersionEntity(
    id = GOAL_ID,
    recoveryTrackId = TRACK_ID,
    goalType = RecoveryGoalType.AWARENESS_ONLY,
    targetValue = null,
    unitKey = null,
    periodType = null,
    title = null,
    effectiveFromEpochMillis = 1_000L,
    effectiveUntilEpochMillis = null,
    createdAtEpochMillis = 1_000L,
    updatedAtEpochMillis = 1_000L,
    revision = 0,
    syncState = SyncState.PENDING,
)

private fun dailyCheckIn() = DailyCheckInEntity(
    id = CHECK_IN_ID,
    ownerKey = "user:user-1",
    localDateEpochDay = LOCAL_DATE_EPOCH_DAY,
    mood = 4,
    stress = 2,
    energy = 3,
    sleepQuality = 4,
    createdAtEpochMillis = 1_000L,
    updatedAtEpochMillis = 1_100L,
    revision = 0,
    syncState = SyncState.PENDING,
)

private fun trackCheckInEntry() = TrackCheckInEntryEntity(
    id = ENTRY_ID,
    dailyCheckInId = CHECK_IN_ID,
    recoveryTrackId = TRACK_ID,
    goalVersionId = GOAL_ID,
    outcome = TrackCheckInOutcome.GOAL_MET,
    measuredValue = null,
    unitKey = null,
    peakUrge = 3,
    privateNote = "never upload this note",
    createdAtEpochMillis = 1_000L,
    updatedAtEpochMillis = 1_100L,
    revision = 0,
    syncState = SyncState.PENDING,
)

private const val TRACK_ID = "7ebdbd0b-4676-45f1-82cd-e632b3ec6092"
private const val OTHER_TRACK_ID = "7ebdbd0b-4676-45f1-82cd-e632b3ec6093"
private const val GOAL_ID = "7ebdbd0b-4676-45f1-82cd-e632b3ec6094"
private const val CHECK_IN_ID = "7ebdbd0b-4676-45f1-82cd-e632b3ec6095"
private const val ENTRY_ID = "7ebdbd0b-4676-45f1-82cd-e632b3ec6096"
private const val LOCAL_DATE_EPOCH_DAY = 20_000L
