package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.deaddict.database.entity.DailyCheckInDraftEntity
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.TrackCheckInEntryEntity
import kotlinx.coroutines.flow.Flow

data class DailyCheckInWithEntries(
    @Embedded val checkIn: DailyCheckInEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dailyCheckInId",
    )
    val entries: List<TrackCheckInEntryEntity>,
)

@Dao
interface DailyCheckInDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCheckIn(checkIn: DailyCheckInEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<TrackCheckInEntryEntity>)

    @Upsert
    suspend fun upsertCheckIn(checkIn: DailyCheckInEntity)

    @Upsert
    suspend fun upsertEntries(entries: List<TrackCheckInEntryEntity>)

    @Transaction
    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey AND localDate = :localDate
        LIMIT 1
        """,
    )
    suspend fun byOwnerDate(ownerKey: String, localDate: String): DailyCheckInWithEntries?

    @Transaction
    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey AND localDate = :localDate
        LIMIT 1
        """,
    )
    fun observeOwnerDate(ownerKey: String, localDate: String): Flow<DailyCheckInWithEntries?>

    @Transaction
    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey
        ORDER BY localDate DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(ownerKey: String, limit: Int): Flow<List<DailyCheckInWithEntries>>

    @Query("SELECT * FROM track_check_in_entries WHERE id = :id LIMIT 1")
    suspend fun entryById(id: String): TrackCheckInEntryEntity?

    @Query(
        """
        SELECT * FROM track_check_in_entries
        WHERE recoveryTrackId = :recoveryTrackId
        ORDER BY createdAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeForTrack(recoveryTrackId: String, limit: Int): Flow<List<TrackCheckInEntryEntity>>

    @Upsert
    suspend fun upsertDraft(draft: DailyCheckInDraftEntity)

    @Query("SELECT * FROM daily_check_in_drafts WHERE ownerKey = :ownerKey LIMIT 1")
    suspend fun draft(ownerKey: String): DailyCheckInDraftEntity?

    @Query("DELETE FROM daily_check_in_drafts WHERE ownerKey = :ownerKey")
    suspend fun deleteDraft(ownerKey: String): Int

    @Query("DELETE FROM daily_check_ins WHERE id = :id")
    suspend fun deleteCheckIn(id: String): Int
}
