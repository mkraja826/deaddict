package com.deaddict.app.sync

import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.TrackCheckInEntryEntity
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
    fun `authenticated user applies downloaded snapshot including Recovery Tracks`() = runBlocking {
        val trackId = "00000000-0000-0000-0000-000000000001"
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
            recoveryTracks = listOf(
                RemoteRecoveryTrackRecord(
                    id = trackId,
                    userId = "user-1",
                    programId = "gaming",
                    displayAlias = null,
                    role = "PRIMARY",
                    status = "ACTIVE",
                    startedAtEpochMillis = 1_000L,
                    pausedAtEpochMillis = null,
                    maintenanceAtEpochMillis = null,
                    archivedAtEpochMillis = null,
                    createdAtEpochMillis = 1_000L,
                    clientUpdatedAtEpochMillis = 1_000L,
                    revision = 0,
                ),
            ),
            recoveryGoals = listOf(
                RemoteRecoveryGoalRecord(
                    id = "00000000-0000-0000-0000-000000000002",
                    userId = "user-1",
                    recoveryTrackId = trackId,
                    goalType = "AWARENESS_ONLY",
                    targetValue = null,
                    unitKey = null,
                    periodType = null,
                    title = null,
                    effectiveFromEpochMillis = 1_000L,
                    effectiveUntilEpochMillis = null,
                    createdAtEpochMillis = 1_000L,
                    clientUpdatedAtEpochMillis = 1_000L,
                    revision = 0,
                ),
            ),
            dailyCheckIns = listOf(
                RemoteDailyCheckInRecord(
                    id = "00000000-0000-0000-0000-000000000003",
                    userId = "user-1",
                    localDateEpochDay = 20_000L,
                    mood = 4,
                    stress = 2,
                    energy = 3,
                    sleepQuality = 4,
                    createdAtEpochMillis = 1_000L,
                    clientUpdatedAtEpochMillis = 1_100L,
                    revision = 0,
                ),
            ),
            trackCheckInEntries = listOf(
                RemoteTrackCheckInEntryRecord(
                    id = "00000000-0000-0000-0000-000000000004",
                    userId = "user-1",
                    localDateEpochDay = 20_000L,
                    recoveryTrackId = trackId,
                    goalVersionId = "00000000-0000-0000-0000-000000000002",
                    outcome = "GOAL_MET",
                    measuredValue = null,
                    unitKey = null,
                    peakUrge = 3,
                    createdAtEpochMillis = 1_000L,
                    clientUpdatedAtEpochMillis = 1_100L,
                    revision = 0,
                ),
            ),
        )
        val store = RecordingRestoreStore(
            summary = RestoreSummary(inserted = 5, updated = 0, skipped = 0),
        )
        val remote = RestoreRemoteGateway(snapshot = snapshot)

        val result = CloudRestoreProcessor(store, remote).restore()

        assertEquals(RestoreSummary(5, 0, 0), result)
        assertSame(snapshot, store.appliedSnapshot)
        assertEquals(1, remote.downloadCount)
        assertEquals(trackId, store.appliedSnapshot?.recoveryGoals?.single()?.recoveryTrackId)
        assertEquals(20_000L, store.appliedSnapshot?.trackCheckInEntries?.single()?.localDateEpochDay)
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

    override suspend fun upsertRecoveryTrack(userId: String, track: RecoveryTrackEntity) = Unit

    override suspend fun upsertRecoveryGoal(userId: String, goal: RecoveryGoalVersionEntity) = Unit

    override suspend fun upsertDailyCheckIn(userId: String, checkIn: DailyCheckInEntity) = Unit

    override suspend fun upsertTrackCheckInEntry(
        userId: String,
        checkIn: DailyCheckInEntity,
        entry: TrackCheckInEntryEntity,
    ) = Unit

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
