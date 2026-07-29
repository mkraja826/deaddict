package com.deaddict.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.SyncState
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryCloudUpsertTest {
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
    fun updatingTrackFromCloudDoesNotCascadeDeleteGoalHistory() = runBlocking {
        val track = RecoveryTrackEntity(
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
            syncState = SyncState.SYNCED,
        )
        val goal = RecoveryGoalVersionEntity(
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
            syncState = SyncState.SYNCED,
        )
        database.recoveryTrackDao().insert(track)
        database.recoveryGoalDao().insert(goal)

        database.recoveryTrackDao().upsertFromCloud(
            track.copy(
                displayAlias = "Cloud alias",
                updatedAtEpochMillis = 2_000L,
                revision = 1,
            ),
        )

        assertEquals("Cloud alias", database.recoveryTrackDao().byId(TRACK_ID)?.displayAlias)
        assertNotNull(database.recoveryGoalDao().byId(GOAL_ID))
        assertEquals(1, database.recoveryGoalDao().allForTrack(TRACK_ID).size)
    }

    private companion object {
        const val TRACK_ID = "00000000-0000-0000-0000-000000000101"
        const val GOAL_ID = "00000000-0000-0000-0000-000000000102"
    }
}
