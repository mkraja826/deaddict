package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deaddict.database.entity.TrackingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: TrackingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
        WHERE programId = :programId
        ORDER BY occurredAtEpochMillis DESC
        """,
    )
    fun observeForProgram(programId: String): Flow<List<TrackingEventEntity>>

    @Query(
        """
        SELECT * FROM tracking_events
        WHERE programId = :programId AND occurredAtEpochMillis >= :since
        ORDER BY occurredAtEpochMillis
        """,
    )
    suspend fun since(programId: String, since: Long): List<TrackingEventEntity>

    @Query("UPDATE tracking_events SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String): Int

    @Query("DELETE FROM tracking_events WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM tracking_events")
    suspend fun deleteAll(): Int
}
