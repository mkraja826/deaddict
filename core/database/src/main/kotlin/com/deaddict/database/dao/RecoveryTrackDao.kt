package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.deaddict.database.entity.RecoveryTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryTrackDao {
    @Query(
        """
        SELECT * FROM recovery_tracks
        WHERE ownerKey = :ownerKey
          AND status IN ('ACTIVE', 'PAUSED', 'MAINTENANCE')
        ORDER BY CASE role WHEN 'PRIMARY' THEN 0 ELSE 1 END,
                 startedAtEpochMillis,
                 id
        """,
    )
    fun observeOpen(ownerKey: String): Flow<List<RecoveryTrackEntity>>

    @Query(
        """
        SELECT * FROM recovery_tracks
        WHERE ownerKey = :ownerKey AND status = 'ARCHIVED'
        ORDER BY archivedAtEpochMillis DESC, id
        """,
    )
    fun observeArchived(ownerKey: String): Flow<List<RecoveryTrackEntity>>

    @Query(
        """
        SELECT * FROM recovery_tracks
        WHERE ownerKey = :ownerKey
        ORDER BY createdAtEpochMillis, id
        """,
    )
    suspend fun allForOwner(ownerKey: String): List<RecoveryTrackEntity>

    @Query(
        """
        SELECT * FROM recovery_tracks
        WHERE ownerKey = :ownerKey
          AND status IN ('ACTIVE', 'PAUSED', 'MAINTENANCE')
        ORDER BY CASE role WHEN 'PRIMARY' THEN 0 ELSE 1 END,
                 startedAtEpochMillis,
                 id
        """,
    )
    suspend fun openForOwner(ownerKey: String): List<RecoveryTrackEntity>

    @Query(
        """
        SELECT * FROM recovery_tracks
        WHERE ownerKey = :ownerKey
          AND id != :excludedTrackId
          AND status IN ('ACTIVE', 'MAINTENANCE')
        ORDER BY startedAtEpochMillis, id
        LIMIT 1
        """,
    )
    suspend fun primaryFallback(
        ownerKey: String,
        excludedTrackId: String,
    ): RecoveryTrackEntity?

    @Query("SELECT * FROM recovery_tracks WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): RecoveryTrackEntity?

    @Query(
        """
        SELECT * FROM recovery_tracks
        WHERE ownerKey = :ownerKey
          AND programId = :programId
          AND status IN ('ACTIVE', 'PAUSED', 'MAINTENANCE')
        LIMIT 1
        """,
    )
    suspend fun openByProgramId(ownerKey: String, programId: String): RecoveryTrackEntity?

    @Query(
        """
        SELECT * FROM recovery_tracks
        WHERE ownerKey = :ownerKey
          AND role = 'PRIMARY'
          AND status IN ('ACTIVE', 'MAINTENANCE')
        LIMIT 1
        """,
    )
    suspend fun primary(ownerKey: String): RecoveryTrackEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(track: RecoveryTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromCloud(track: RecoveryTrackEntity)

    @Update
    suspend fun update(track: RecoveryTrackEntity): Int

    @Query("UPDATE recovery_tracks SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String): Int

    @Query(
        """
        UPDATE recovery_tracks
        SET role = 'SUPPORTING',
            updatedAtEpochMillis = :updatedAtEpochMillis,
            revision = revision + 1,
            syncState = 'PENDING'
        WHERE ownerKey = :ownerKey
          AND role = 'PRIMARY'
          AND status IN ('ACTIVE', 'MAINTENANCE')
        """,
    )
    suspend fun demoteCurrentPrimary(ownerKey: String, updatedAtEpochMillis: Long): Int

    @Query(
        """
        UPDATE recovery_tracks
        SET role = 'PRIMARY',
            updatedAtEpochMillis = :updatedAtEpochMillis,
            revision = revision + 1,
            syncState = 'PENDING'
        WHERE id = :trackId
          AND ownerKey = :ownerKey
          AND status IN ('ACTIVE', 'MAINTENANCE')
        """,
    )
    suspend fun promotePrimary(
        ownerKey: String,
        trackId: String,
        updatedAtEpochMillis: Long,
    ): Int

    @Transaction
    suspend fun setPrimary(
        ownerKey: String,
        trackId: String,
        updatedAtEpochMillis: Long,
    ) {
        demoteCurrentPrimary(ownerKey, updatedAtEpochMillis)
        check(promotePrimary(ownerKey, trackId, updatedAtEpochMillis) == 1) {
            "Primary track must exist, belong to the owner, and be active or in maintenance"
        }
    }

    @Query("DELETE FROM recovery_tracks WHERE id = :id AND ownerKey = :ownerKey")
    suspend fun deleteById(ownerKey: String, id: String): Int
}
