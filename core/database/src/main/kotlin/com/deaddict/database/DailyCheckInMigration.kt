package com.deaddict.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_check_ins (
                id TEXT NOT NULL,
                ownerKey TEXT NOT NULL,
                localDate TEXT NOT NULL,
                timezoneId TEXT NOT NULL,
                mood INTEGER,
                stress INTEGER,
                energy INTEGER,
                sleep INTEGER,
                triggerKeys TEXT NOT NULL,
                privateNote TEXT,
                completedAtEpochMillis INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                syncState TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_daily_check_ins_ownerKey ON daily_check_ins(ownerKey)")
        database.execSQL(
            "CREATE INDEX index_daily_check_ins_completedAtEpochMillis " +
                "ON daily_check_ins(completedAtEpochMillis)",
        )
        database.execSQL("CREATE INDEX index_daily_check_ins_syncState ON daily_check_ins(syncState)")
        database.execSQL(
            "CREATE UNIQUE INDEX index_daily_check_ins_ownerKey_localDate " +
                "ON daily_check_ins(ownerKey, localDate)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS track_check_in_entries (
                id TEXT NOT NULL,
                dailyCheckInId TEXT NOT NULL,
                ownerKey TEXT NOT NULL,
                recoveryTrackId TEXT NOT NULL,
                recoveryGoalVersionId TEXT,
                outcome TEXT NOT NULL,
                urgeIntensity INTEGER,
                quantity REAL,
                quantityUnit TEXT,
                durationMinutes INTEGER,
                costMinorUnits INTEGER,
                currencyCode TEXT,
                triggerKeys TEXT NOT NULL,
                privateNote TEXT,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                syncState TEXT NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(dailyCheckInId) REFERENCES daily_check_ins(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(recoveryTrackId) REFERENCES recovery_tracks(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX index_track_check_in_entries_ownerKey ON track_check_in_entries(ownerKey)")
        database.execSQL(
            "CREATE INDEX index_track_check_in_entries_dailyCheckInId " +
                "ON track_check_in_entries(dailyCheckInId)",
        )
        database.execSQL(
            "CREATE INDEX index_track_check_in_entries_recoveryTrackId " +
                "ON track_check_in_entries(recoveryTrackId)",
        )
        database.execSQL(
            "CREATE INDEX index_track_check_in_entries_recoveryGoalVersionId " +
                "ON track_check_in_entries(recoveryGoalVersionId)",
        )
        database.execSQL("CREATE INDEX index_track_check_in_entries_outcome ON track_check_in_entries(outcome)")
        database.execSQL("CREATE INDEX index_track_check_in_entries_syncState ON track_check_in_entries(syncState)")
        database.execSQL(
            "CREATE UNIQUE INDEX index_track_check_in_entries_dailyCheckInId_recoveryTrackId " +
                "ON track_check_in_entries(dailyCheckInId, recoveryTrackId)",
        )
        database.execSQL(
            "CREATE INDEX index_track_check_in_entries_recoveryTrackId_createdAtEpochMillis " +
                "ON track_check_in_entries(recoveryTrackId, createdAtEpochMillis)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_check_in_drafts (
                ownerKey TEXT NOT NULL,
                localDate TEXT NOT NULL,
                timezoneId TEXT NOT NULL,
                mood INTEGER,
                stress INTEGER,
                energy INTEGER,
                sleep INTEGER,
                triggerKeys TEXT NOT NULL,
                privateNote TEXT,
                trackEntriesPayload TEXT NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                PRIMARY KEY(ownerKey)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX index_daily_check_in_drafts_localDate " +
                "ON daily_check_in_drafts(localDate)",
        )

        installDailyCheckInConstraints(database)
    }
}

val DAILY_CHECK_IN_DATABASE_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        installDailyCheckInConstraints(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        installDailyCheckInConstraints(db)
    }
}

private fun installDailyCheckInConstraints(database: SupportSQLiteDatabase) {
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS track_check_in_entries_owner_insert
        BEFORE INSERT ON track_check_in_entries
        WHEN NOT EXISTS (
            SELECT 1
            FROM daily_check_ins parent
            JOIN recovery_tracks track ON track.id = NEW.recoveryTrackId
            WHERE parent.id = NEW.dailyCheckInId
              AND parent.ownerKey = NEW.ownerKey
              AND track.ownerKey = NEW.ownerKey
        )
        BEGIN
            SELECT RAISE(ABORT, 'Check-in entry must match its owner, parent, and Recovery Track');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS track_check_in_entries_owner_update
        BEFORE UPDATE OF dailyCheckInId, ownerKey, recoveryTrackId ON track_check_in_entries
        WHEN NOT EXISTS (
            SELECT 1
            FROM daily_check_ins parent
            JOIN recovery_tracks track ON track.id = NEW.recoveryTrackId
            WHERE parent.id = NEW.dailyCheckInId
              AND parent.ownerKey = NEW.ownerKey
              AND track.ownerKey = NEW.ownerKey
        )
        BEGIN
            SELECT RAISE(ABORT, 'Check-in entry must match its owner, parent, and Recovery Track');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS daily_check_ins_owner_update
        BEFORE UPDATE OF ownerKey ON daily_check_ins
        WHEN EXISTS (
            SELECT 1
            FROM track_check_in_entries entry
            JOIN recovery_tracks track ON track.id = entry.recoveryTrackId
            WHERE entry.dailyCheckInId = NEW.id
              AND track.ownerKey != NEW.ownerKey
        )
        BEGIN
            SELECT RAISE(ABORT, 'Check-in owner must match every Recovery Track entry');
        END
        """.trimIndent(),
    )
}
