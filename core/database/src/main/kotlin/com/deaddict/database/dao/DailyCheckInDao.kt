package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
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
    @Transaction
    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey AND localDateEpochDay = :localDateEpochDay
        LIMIT 1
        """,
    )
    fun observeForDate(ownerKey: String, localDateEpochDay: Long): Flow<DailyCheckInWithEntries?>

    @Transaction
    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey
        ORDER BY localDateEpochDay DESC, updatedAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(ownerKey: String, limit: Int): Flow<List<DailyCheckInWithEntries>>

    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey AND localDateEpochDay = :localDateEpochDay
        LIMIT 1
        """,
    )
    suspend fun byDate(ownerKey: String, localDateEpochDay: Long): DailyCheckInEntity?

    @Query("SELECT * FROM daily_check_ins WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): DailyCheckInEntity?

    @Query("SELECT * FROM track_check_in_entries WHERE dailyCheckInId = :dailyCheckInId")
    suspend fun entries(dailyCheckInId: String): List<TrackCheckInEntryEntity>

    @Upsert
    suspend fun upsertCheckIn(checkIn: DailyCheckInEntity)

    @Upsert
    suspend fun upsertEntries(entries: List<TrackCheckInEntryEntity>)

    @Query("DELETE FROM track_check_in_entries WHERE dailyCheckInId = :dailyCheckInId")
    suspend fun deleteEntries(dailyCheckInId: String): Int

    @Query("DELETE FROM daily_check_ins WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM track_check_in_entries")
    suspend fun deleteAllEntries(): Int

    @Query("DELETE FROM daily_check_ins")
    suspend fun deleteAllCheckIns(): Int
}
