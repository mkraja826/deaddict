package com.deaddict.app.sync

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.TrackingEventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CloudRestoreProcessorTest {
    @Test
    fun `signed out user does not download or apply cloud data`() = runBlocking {
        val store = RecordingRestoreStore()
        val remote = RestoreRemoteGateway(userId = null)

        val result = CloudRestoreProcessor(store, remote).restore()

        assertNull(result)
        assertEquals(0, remote.downloadCount)
        assertNull(store.appliedSnapshot)
    }

    @Test
    fun `authenticated user applies downloaded snapshot`() = runBlocking {
        val snapshot = CloudSnapshot(
            programs = listOf(
                RemoteProgramRecord(
                    id = "program-1",
                    programId = "gaming",
                    activatedAtEpochMillis = 1_000L,
                    archivedAtEpochMillis = null,
                    clientUpdatedAtEpochMillis = 1_000L,
                ),
            ),
            trackingEvents = emptyList(),
            rescueSessions = emptyList(),
        )
        val store = RecordingRestoreStore(
            summary = RestoreSummary(inserted = 1, updated = 0, skipped = 0),
        )
        val remote = RestoreRemoteGateway(snapshot = snapshot)

        val result = CloudRestoreProcessor(store, remote).restore()

        assertEquals(RestoreSummary(1, 0, 0), result)
        assertSame(snapshot, store.appliedSnapshot)
        assertEquals(1, remote.downloadCount)
    }
}

private class RecordingRestoreStore(
    private val summary: RestoreSummary = RestoreSummary(0, 0, 0),
) : RestoreStore {
    var appliedSnapshot: CloudSnapshot? = null

    override suspend fun apply(snapshot: CloudSnapshot): RestoreSummary {
        appliedSnapshot = snapshot
        return summary
    }
}

private class RestoreRemoteGateway(
    private val userId: String? = "user-1",
    private val snapshot: CloudSnapshot = CloudSnapshot(emptyList(), emptyList(), emptyList()),
) : RemoteSyncGateway {
    override val available: Boolean = true
    var downloadCount: Int = 0

    override suspend fun currentUserId(): String? = userId

    override suspend fun upsertProgram(userId: String, program: ActiveProgramEntity) = Unit

    override suspend fun upsertTrackingEvent(userId: String, event: TrackingEventEntity) = Unit

    override suspend fun upsertRescueSession(userId: String, session: RescueSessionEntity) = Unit

    override suspend fun deleteRecord(
        userId: String,
        aggregateType: SyncAggregateType,
        aggregateId: String,
    ) = Unit

    override suspend fun downloadSnapshot(): CloudSnapshot {
        downloadCount += 1
        return snapshot
    }
}
