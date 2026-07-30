package com.deaddict.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.TrackCheckInEntryEntity
import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.RecoveryGoalType
import kotlinx.coroutines.flow.Flow

data class DailyCheckInWithEntries(
    @Embedded val checkIn: DailyCheckInEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dailyCheckInId",
    )
    val entries: List<TrackCheckInEntryEntity>,
)

data class TrackCheckInProgressRow(
    val localDateEpochDay: Long,
    val goalVersionId: String?,
    val outcome: TrackCheckInOutcome,
    val measuredValue: Double?,
    val unitKey: String?,
    val peakUrge: Int?,
    val goalType: RecoveryGoalType?,
    val targetValue: Double?,
    val goalUnitKey: String?,
    val periodType: GoalPeriodType?,
    val goalTitle: String?,
)

data class CrossTrackOutcomeRow(
    val localDateEpochDay: Long,
    val mood: Int?,
    val stress: Int?,
    val energy: Int?,
    val sleepQuality: Int?,
    val selectedOutcome: TrackCheckInOutcome,
    val otherTrackId: String,
    val otherOutcome: TrackCheckInOutcome,
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
        SELECT
            checkIn.localDateEpochDay AS localDateEpochDay,
            entry.goalVersionId AS goalVersionId,
            entry.outcome AS outcome,
            entry.measuredValue AS measuredValue,
            entry.unitKey AS unitKey,
            entry.peakUrge AS peakUrge,
            goal.goalType AS goalType,
            goal.targetValue AS targetValue,
            goal.unitKey AS goalUnitKey,
            goal.periodType AS periodType,
            goal.title AS goalTitle
        FROM daily_check_ins AS checkIn
        INNER JOIN track_check_in_entries AS entry
            ON entry.dailyCheckInId = checkIn.id
        LEFT JOIN recovery_goal_versions AS goal
            ON goal.id = entry.goalVersionId
        WHERE checkIn.ownerKey = :ownerKey
          AND entry.recoveryTrackId = :recoveryTrackId
          AND checkIn.localDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY checkIn.localDateEpochDay, entry.id
        """,
    )
    suspend fun progressRows(
        ownerKey: String,
        recoveryTrackId: String,
        startEpochDay: Long,
        endEpochDay: Long,
    ): List<TrackCheckInProgressRow>

    @Query(
        """
        SELECT
            checkIn.localDateEpochDay AS localDateEpochDay,
            checkIn.mood AS mood,
            checkIn.stress AS stress,
            checkIn.energy AS energy,
            checkIn.sleepQuality AS sleepQuality,
            selected.outcome AS selectedOutcome,
            other.recoveryTrackId AS otherTrackId,
            other.outcome AS otherOutcome
        FROM daily_check_ins AS checkIn
        INNER JOIN track_check_in_entries AS selected
            ON selected.dailyCheckInId = checkIn.id
           AND selected.recoveryTrackId = :selectedRecoveryTrackId
        INNER JOIN track_check_in_entries AS other
            ON other.dailyCheckInId = checkIn.id
           AND other.recoveryTrackId != :selectedRecoveryTrackId
        INNER JOIN recovery_tracks AS otherTrack
            ON otherTrack.id = other.recoveryTrackId
        WHERE checkIn.ownerKey = :ownerKey
          AND otherTrack.ownerKey = :ownerKey
          AND checkIn.localDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY other.recoveryTrackId, checkIn.localDateEpochDay, other.id
        """,
    )
    suspend fun crossTrackOutcomeRows(
        ownerKey: String,
        selectedRecoveryTrackId: String,
        startEpochDay: Long,
        endEpochDay: Long,
    ): List<CrossTrackOutcomeRow>

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

    @Query(
        """
        SELECT * FROM daily_check_ins
        WHERE ownerKey = :ownerKey
        ORDER BY localDateEpochDay, id
        """,
    )
    suspend fun allForOwner(ownerKey: String): List<DailyCheckInEntity>

    @Query("SELECT * FROM track_check_in_entries WHERE id = :id LIMIT 1")
    suspend fun entryById(id: String): TrackCheckInEntryEntity?

    @Query(
        """
        SELECT * FROM track_check_in_entries
        WHERE dailyCheckInId = :dailyCheckInId AND recoveryTrackId = :recoveryTrackId
        LIMIT 1
        """,
    )
    suspend fun entryForTrack(
        dailyCheckInId: String,
        recoveryTrackId: String,
    ): TrackCheckInEntryEntity?

    @Query("SELECT * FROM track_check_in_entries WHERE dailyCheckInId = :dailyCheckInId")
    suspend fun entries(dailyCheckInId: String): List<TrackCheckInEntryEntity>

    @Upsert
    suspend fun upsertCheckIn(checkIn: DailyCheckInEntity)

    @Upsert
    suspend fun upsertEntry(entry: TrackCheckInEntryEntity)

    @Upsert
    suspend fun upsertEntries(entries: List<TrackCheckInEntryEntity>)

    @Update
    suspend fun updateCheckIn(checkIn: DailyCheckInEntity): Int

    @Update
    suspend fun updateEntry(entry: TrackCheckInEntryEntity): Int

    @Query("UPDATE daily_check_ins SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markCheckInSynced(id: String): Int

    @Query("UPDATE track_check_in_entries SET syncState = 'SYNCED' WHERE id = :id")
    suspend fun markEntrySynced(id: String): Int

    @Query("DELETE FROM track_check_in_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String): Int

    @Query("DELETE FROM track_check_in_entries WHERE dailyCheckInId = :dailyCheckInId")
    suspend fun deleteEntries(dailyCheckInId: String): Int

    @Query("DELETE FROM daily_check_ins WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM track_check_in_entries")
    suspend fun deleteAllEntries(): Int

    @Query("DELETE FROM daily_check_ins")
    suspend fun deleteAllCheckIns(): Int
}
