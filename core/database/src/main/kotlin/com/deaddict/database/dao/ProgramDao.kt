package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deaddict.database.entity.ActiveProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM active_programs WHERE archivedAtEpochMillis IS NULL ORDER BY activatedAtEpochMillis")
    fun observeActive(): Flow<List<ActiveProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(program: ActiveProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromCloud(program: ActiveProgramEntity)

    @Query("SELECT * FROM active_programs WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): ActiveProgramEntity?

    @Query("SELECT * FROM active_programs WHERE programId = :programId LIMIT 1")
    suspend fun byProgramId(programId: String): ActiveProgramEntity?

    @Query("SELECT * FROM active_programs WHERE syncState = 'LOCAL_ONLY'")
    suspend fun localOnly(): List<ActiveProgramEntity>

    @Query("UPDATE active_programs SET syncState = 'PENDING' WHERE id = :id AND syncState = 'LOCAL_ONLY'")
    suspend fun markPending(id: String): Int

    @Query("UPDATE active_programs SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String): Int

    @Query(
        """
        UPDATE active_programs
        SET archivedAtEpochMillis = :archivedAt, syncState = :pending
        WHERE id = :id AND archivedAtEpochMillis IS NULL
        """,
    )
    suspend fun archive(id: String, archivedAt: Long, pending: String = "PENDING"): Int
}
