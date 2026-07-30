package com.deaddict.database

import androidx.room.TypeConverter
import com.deaddict.database.entity.OutboxState
import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventKind
import com.deaddict.model.GoalPeriodType
import com.deaddict.model.RecoveryGoalType
import com.deaddict.model.RecoveryTrackRole
import com.deaddict.model.RecoveryTrackStatus
import com.deaddict.model.TrackCheckInOutcome

internal class DatabaseConverters {
    @TypeConverter fun fromSyncState(value: SyncState): String = value.name
    @TypeConverter fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
    @TypeConverter fun fromOutboxState(value: OutboxState): String = value.name
    @TypeConverter fun toOutboxState(value: String): OutboxState = OutboxState.valueOf(value)
    @TypeConverter fun fromSyncOperation(value: SyncOperation): String = value.name
    @TypeConverter fun toSyncOperation(value: String): SyncOperation = SyncOperation.valueOf(value)
    @TypeConverter fun fromAggregateType(value: SyncAggregateType): String = value.name
    @TypeConverter fun toAggregateType(value: String): SyncAggregateType = SyncAggregateType.valueOf(value)
    @TypeConverter fun fromTrackingKind(value: TrackingEventKind): String = value.name
    @TypeConverter fun toTrackingKind(value: String): TrackingEventKind = TrackingEventKind.valueOf(value)
    @TypeConverter fun fromRescueOutcome(value: RescueOutcome?): String? = value?.name
    @TypeConverter fun toRescueOutcome(value: String?): RescueOutcome? = value?.let(RescueOutcome::valueOf)
    @TypeConverter fun fromRecoveryTrackRole(value: RecoveryTrackRole): String = value.name
    @TypeConverter fun toRecoveryTrackRole(value: String): RecoveryTrackRole = RecoveryTrackRole.valueOf(value)
    @TypeConverter fun fromRecoveryTrackStatus(value: RecoveryTrackStatus): String = value.name
    @TypeConverter fun toRecoveryTrackStatus(value: String): RecoveryTrackStatus = RecoveryTrackStatus.valueOf(value)
    @TypeConverter fun fromRecoveryGoalType(value: RecoveryGoalType): String = value.name
    @TypeConverter fun toRecoveryGoalType(value: String): RecoveryGoalType = RecoveryGoalType.valueOf(value)
    @TypeConverter fun fromGoalPeriodType(value: GoalPeriodType?): String? = value?.name
    @TypeConverter fun toGoalPeriodType(value: String?): GoalPeriodType? = value?.let(GoalPeriodType::valueOf)
    @TypeConverter fun fromTrackCheckInOutcome(value: TrackCheckInOutcome): String = value.name
    @TypeConverter fun toTrackCheckInOutcome(value: String): TrackCheckInOutcome = TrackCheckInOutcome.valueOf(value)
    @TypeConverter fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)
    @TypeConverter fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
