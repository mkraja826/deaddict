package com.deaddict.database

import androidx.room.TypeConverter
import com.deaddict.database.entity.OutboxState
import com.deaddict.database.entity.RescueOutcome
import com.deaddict.database.entity.SyncAggregateType
import com.deaddict.database.entity.SyncOperation
import com.deaddict.database.entity.SyncState
import com.deaddict.database.entity.TrackingEventKind

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
    @TypeConverter fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)
    @TypeConverter fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}

