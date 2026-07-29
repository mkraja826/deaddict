package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deaddict.database.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(item: SyncOutboxEntity): Long

    @Query(
        """
        SELECT * FROM sync_outbox
        WHERE state = 'PENDING' AND nextAttemptAtEpochMillis <= :now
        ORDER BY
            CASE aggregateType
                WHEN 'RECOVERY_TRACK' THEN 0
                WHEN 'RECOVERY_GOAL' THEN 1
                WHEN 'ACTIVE_PROGRAM' THEN 2
                WHEN 'TRACKING_EVENT' THEN 3
                WHEN 'RESCUE_SESSION' THEN 3
                ELSE 9
            END,
            createdAtEpochMillis,
            id
        LIMIT :limit
        """,
    )
    suspend fun nextBatch(now: Long, limit: Int): List<SyncOutboxEntity>

    @Query("UPDATE sync_outbox SET state = 'PENDING' WHERE state = 'IN_FLIGHT'")
    suspend fun resetInterruptedClaims(): Int

    @Query(
        """
        UPDATE sync_outbox SET state = 'IN_FLIGHT'
        WHERE id = :id AND state = 'PENDING'
        """,
    )
    suspend fun claim(id: String): Int

    @Query("UPDATE sync_outbox SET state = 'COMPLETED', lastErrorCode = NULL WHERE id = :id")
    suspend fun complete(id: String): Int

    @Query(
        """
        UPDATE sync_outbox SET state = 'COMPLETED', lastErrorCode = 'SUPERSEDED_BY_DELETE'
        WHERE aggregateType = :aggregateType
          AND aggregateId = :aggregateId
          AND operation = 'UPSERT'
          AND state IN ('PENDING', 'IN_FLIGHT')
        """,
    )
    suspend fun supersedePendingUpsert(aggregateType: String, aggregateId: String): Int

    @Query(
        """
        SELECT aggregateId FROM sync_outbox
        WHERE aggregateType = :aggregateType AND operation = 'DELETE'
        """,
    )
    suspend fun deleteTombstoneIds(aggregateType: String): List<String>

    @Query("DELETE FROM sync_outbox")
    suspend fun deleteAll(): Int

    @Query(
        """
        UPDATE sync_outbox SET
          state = CASE WHEN :deadLetter THEN 'DEAD_LETTER' ELSE 'PENDING' END,
          attemptCount = attemptCount + 1,
          nextAttemptAtEpochMillis = :nextAttempt,
          lastErrorCode = :errorCode
        WHERE id = :id
        """,
    )
    suspend fun retry(id: String, nextAttempt: Long, errorCode: String, deadLetter: Boolean): Int
}
