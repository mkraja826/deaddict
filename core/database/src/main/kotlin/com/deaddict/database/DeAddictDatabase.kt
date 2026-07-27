package com.deaddict.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.deaddict.database.dao.ProgramDao
import com.deaddict.database.dao.RescueDao
import com.deaddict.database.dao.SyncOutboxDao
import com.deaddict.database.dao.TrackingDao
import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.TrackingEventEntity

@Database(
    entities = [
        ActiveProgramEntity::class,
        TrackingEventEntity::class,
        RescueSessionEntity::class,
        SyncOutboxEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class DeAddictDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun trackingDao(): TrackingDao
    abstract fun rescueDao(): RescueDao
    abstract fun syncOutboxDao(): SyncOutboxDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tracking_events ADD COLUMN triggerKey TEXT")
    }
}
