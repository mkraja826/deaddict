package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deaddict.database.entity.RescueSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RescueDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: RescueSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromCloud(session: RescueSessionEntity)

    @Query("SELECT * FROM rescue_sessions WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): RescueSessionEntity?

    @Query("SELECT * FROM rescue_sessions WHERE syncState = 'LOCAL_ONLY'")
    suspend fun localOnly(): List<RescueSessionEntity>

    @Query("UPDATE rescue_sessions SET syncState = 'PENDING' WHERE id = :id AND syncState = 'LOCAL_ONLY'")
    suspend fun markPending(id: String): Int

    @Query("UPDATE rescue_sessions SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String): Int

    @Query("SELECT * FROM rescue_sessions ORDER BY startedAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RescueSessionEntity>>

    @Query(
        """
        SELECT * FROM rescue_sessions
        WHERE programId = :programId AND startedAtEpochMillis >= :since
        ORDER BY startedAtEpochMillis
        """,
    )
    suspend fun since(programId: String, since: Long): List<RescueSessionEntity>
}
