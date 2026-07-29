package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: TrackingEventEntity)

    @Upsert
    suspend fun upsertFromCloud(event: TrackingEventEntity)

    @Query("SELECT * FROM tracking_events WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): TrackingEventEntity?

    @Query("SELECT * FROM tracking_events WHERE syncState = 'LOCAL_ONLY'")
    suspend fun localOnly(): List<TrackingEventEntity>

    @Query("UPDATE tracking_events SET syncState = 'PENDING' WHERE id = :id AND syncState = 'LOCAL_ONLY'")
    suspend fun markPending(id: String): Int

    @Query(
        """
        SELECT * FROM tracking_events
        WHERE recoveryTrackId = :recoveryTrackId
        ORDER BY occurredAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeForTrack(recoveryTrackId: String, limit: Int): Flow<List<TrackingEventEntity>>

    @Query(
        """
        SELECT * FROM tracking_events
        WHERE recoveryTrackId = :recoveryTrackId AND occurredAtEpochMillis >= :since
        ORDER BY occurredAtEpochMillis
        """,
    )
    suspend fun sinceTrack(recoveryTrackId: String, since: Long): List<TrackingEventEntity>

    @Query(
        """
        SELECT * FROM tracking_events
        WHERE programId = :programId
        ORDER BY occurredAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeForProgram(programId: String, limit: Int): Flow<List<TrackingEventEntity>>

    @Query(
        """
        SELECT * FROM tracking_events
        WHERE programId = :programId AND occurredAtEpochMillis >= :since
        ORDER BY occurredAtEpochMillis
        """,
    )
    suspend fun since(programId: String, since: Long): List<TrackingEventEntity>

    @Query(
        """
        UPDATE tracking_events
        SET ownerKey = :ownerKey,
            syncState = :syncState
        WHERE recoveryTrackId = :recoveryTrackId
        """,
    )
    suspend fun reassignOwner(
        recoveryTrackId: String,
        ownerKey: String,
        syncState: SyncState,
    ): Int

    @Query("SELECT * FROM tracking_events WHERE recoveryTrackId = :recoveryTrackId")
    suspend fun allForTrack(recoveryTrackId: String): List<TrackingEventEntity>

    @Query("UPDATE tracking_events SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String): Int

    @Query("DELETE FROM tracking_events WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM tracking_events")
    suspend fun deleteAll(): Int
}
