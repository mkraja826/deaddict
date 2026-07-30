package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.deaddict.database.entity.DailyCheckInDraftEntity
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.TrackCheckInEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCheckInDao {
    @Transaction
    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey AND localDate = :localDate
        LIMIT 1
        """,
    )
    fun observeByOwnerDate(ownerKey: String, localDate: String): Flow<DailyCheckInWithEntries?>

    @Transaction
    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey AND localDate = :localDate
        LIMIT 1
        """,
    )
    suspend fun byOwnerDate(ownerKey: String, localDate: String): DailyCheckInWithEntries?

    @Query("SELECT * FROM daily_check_ins WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): DailyCheckInEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(checkIn: DailyCheckInEntity)

    @Update
    suspend fun update(checkIn: DailyCheckInEntity): Int

    @Query("DELETE FROM daily_check_ins WHERE id = :id AND ownerKey = :ownerKey")
    suspend fun deleteById(ownerKey: String, id: String): Int
}

@Dao
interface TrackCheckInEntryDao {
    @Query(
        """
        SELECT * FROM track_check_in_entries
        WHERE dailyCheckInId = :dailyCheckInId
        ORDER BY createdAtEpochMillis, id
        """,
    )
    suspend fun forDailyCheckIn(dailyCheckInId: String): List<TrackCheckInEntryEntity>

    @Query(
        """
        SELECT * FROM track_check_in_entries
        WHERE recoveryTrackId = :recoveryTrackId
          AND createdAtEpochMillis >= :sinceEpochMillis
        ORDER BY createdAtEpochMillis, id
        """,
    )
    fun observeForTrackSince(
        recoveryTrackId: String,
        sinceEpochMillis: Long,
    ): Flow<List<TrackCheckInEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<TrackCheckInEntryEntity>)

    @Upsert
    suspend fun upsertFromCloud(entries: List<TrackCheckInEntryEntity>)

    @Query("DELETE FROM track_check_in_entries WHERE id = :id AND ownerKey = :ownerKey")
    suspend fun deleteById(ownerKey: String, id: String): Int
}

@Dao
interface DailyCheckInDraftDao {
    @Query(
        """
        SELECT * FROM daily_check_in_drafts
        WHERE ownerKey = :ownerKey AND localDate = :localDate
        LIMIT 1
        """,
    )
    fun observe(ownerKey: String, localDate: String): Flow<DailyCheckInDraftEntity?>

    @Query(
        """
        SELECT * FROM daily_check_in_drafts
        WHERE ownerKey = :ownerKey AND localDate = :localDate
        LIMIT 1
        """,
    )
    suspend fun get(ownerKey: String, localDate: String): DailyCheckInDraftEntity?

    @Upsert
    suspend fun upsert(draft: DailyCheckInDraftEntity)

    @Query(
        """
        DELETE FROM daily_check_in_drafts
        WHERE ownerKey = :ownerKey AND localDate = :localDate
        """,
    )
    suspend fun delete(ownerKey: String, localDate: String): Int
}

data class DailyCheckInWithEntries(
    @Embedded val checkIn: DailyCheckInEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dailyCheckInId",
    )
    val entries: List<TrackCheckInEntryEntity>,
)
