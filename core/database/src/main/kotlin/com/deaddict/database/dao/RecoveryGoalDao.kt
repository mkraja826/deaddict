package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryGoalDao {
    @Query(
        """
        SELECT * FROM recovery_goal_versions
        WHERE recoveryTrackId = :recoveryTrackId
          AND effectiveUntilEpochMillis IS NULL
        LIMIT 1
        """,
    )
    fun observeCurrent(recoveryTrackId: String): Flow<RecoveryGoalVersionEntity?>

    @Query(
        """
        SELECT * FROM recovery_goal_versions
        WHERE recoveryTrackId = :recoveryTrackId
        ORDER BY effectiveFromEpochMillis DESC, id DESC
        """,
    )
    fun observeHistory(recoveryTrackId: String): Flow<List<RecoveryGoalVersionEntity>>

    @Query("SELECT * FROM recovery_goal_versions WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): RecoveryGoalVersionEntity?

    @Query(
        """
        SELECT * FROM recovery_goal_versions
        WHERE recoveryTrackId = :recoveryTrackId
        ORDER BY effectiveFromEpochMillis, id
        """,
    )
    suspend fun allForTrack(recoveryTrackId: String): List<RecoveryGoalVersionEntity>

    @Query(
        """
        SELECT * FROM recovery_goal_versions
        WHERE recoveryTrackId = :recoveryTrackId
          AND effectiveUntilEpochMillis IS NULL
        LIMIT 1
        """,
    )
    suspend fun current(recoveryTrackId: String): RecoveryGoalVersionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(goal: RecoveryGoalVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromCloud(goal: RecoveryGoalVersionEntity)

    @Update
    suspend fun update(goal: RecoveryGoalVersionEntity): Int

    @Query("UPDATE recovery_goal_versions SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String): Int

    @Query(
        """
        UPDATE recovery_goal_versions
        SET effectiveUntilEpochMillis = :closedAtEpochMillis,
            updatedAtEpochMillis = :closedAtEpochMillis,
            revision = revision + 1,
            syncState = :syncState
        WHERE recoveryTrackId = :recoveryTrackId
          AND effectiveUntilEpochMillis IS NULL
          AND effectiveFromEpochMillis < :closedAtEpochMillis
        """,
    )
    suspend fun closeCurrent(
        recoveryTrackId: String,
        closedAtEpochMillis: Long,
        syncState: SyncState,
    ): Int

    @Transaction
    suspend fun replaceCurrent(
        recoveryTrackId: String,
        closedAtEpochMillis: Long,
        replacement: RecoveryGoalVersionEntity,
        syncState: SyncState,
    ) {
        require(replacement.recoveryTrackId == recoveryTrackId) {
            "Replacement goal must belong to the requested Recovery Track"
        }
        check(closeCurrent(recoveryTrackId, closedAtEpochMillis, syncState) == 1) {
            "Recovery Track must have one current goal to replace"
        }
        insert(replacement)
    }

    @Query("DELETE FROM recovery_goal_versions WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
