package com.deaddict.database

import androidx.room.TypeConverter
import com.deaddict.database.entity.TrackCheckInOutcome

internal class DailyCheckInConverters {
    @TypeConverter
    fun fromOutcome(value: TrackCheckInOutcome): String = value.name

    @TypeConverter
    fun toOutcome(value: String): TrackCheckInOutcome = TrackCheckInOutcome.valueOf(value)
}
