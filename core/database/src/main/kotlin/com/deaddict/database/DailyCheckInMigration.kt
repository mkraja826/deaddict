package com.deaddict.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        createDailyCheckInTables(database)
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

private fun createDailyCheckInTables(database: SupportSQLiteDatabase) {
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
            sleepQuality INTEGER,
            sharedTriggerKeys TEXT NOT NULL,
            privateNote TEXT,
            completedAtEpochMillis INTEGER NOT NULL,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            revision INTEGER NOT NULL,
            syncState TEXT NOT NULL,
            PRIMARY KEY(id)
        )
        """.trimIndent(),
    )
    database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_check_ins_ownerKey ON daily_check_ins(ownerKey)")
    database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_check_ins_localDate ON daily_check_ins(localDate)")
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_daily_check_ins_completedAtEpochMillis " +
            "ON daily_check_ins(completedAtEpochMillis)",
    )
    database.execSQL("CREATE INDEX IF NOT EXISTS index_daily_check_ins_syncState ON daily_check_ins(syncState)")
    database.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_check_ins_ownerKey_localDate " +
            "ON daily_check_ins(ownerKey, localDate)",
    )

    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS track_check_in_entries (
            id TEXT NOT NULL,
            dailyCheckInId TEXT NOT NULL,
            ownerKey TEXT NOT NULL,
            recoveryTrackId TEXT NOT NULL,
            programId TEXT NOT NULL,
            goalVersionId TEXT,
            outcome TEXT NOT NULL,
            quantity REAL,
            unit TEXT,
            durationMinutes INTEGER,
            costMinorUnits INTEGER,
            urgeIntensity INTEGER,
            triggerKeys TEXT NOT NULL,
            privateNote TEXT,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            revision INTEGER NOT NULL,
            syncState TEXT NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(dailyCheckInId) REFERENCES daily_check_ins(id)
                ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(recoveryTrackId) REFERENCES recovery_tracks(id)
                ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(goalVersionId) REFERENCES recovery_goal_versions(id)
                ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_track_check_in_entries_dailyCheckInId " +
            "ON track_check_in_entries(dailyCheckInId)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_track_check_in_entries_ownerKey " +
            "ON track_check_in_entries(ownerKey)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_track_check_in_entries_recoveryTrackId " +
            "ON track_check_in_entries(recoveryTrackId)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_track_check_in_entries_programId " +
            "ON track_check_in_entries(programId)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_track_check_in_entries_goalVersionId " +
            "ON track_check_in_entries(goalVersionId)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_track_check_in_entries_outcome " +
            "ON track_check_in_entries(outcome)",
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_track_check_in_entries_syncState " +
            "ON track_check_in_entries(syncState)",
    )
    database.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_track_check_in_entries_dailyCheckInId_recoveryTrackId " +
            "ON track_check_in_entries(dailyCheckInId, recoveryTrackId)",
    )

    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS daily_check_in_drafts (
            ownerKey TEXT NOT NULL,
            localDate TEXT NOT NULL,
            timezoneId TEXT NOT NULL,
            payloadJson TEXT NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            PRIMARY KEY(ownerKey, localDate)
        )
        """.trimIndent(),
    )
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS index_daily_check_in_drafts_updatedAtEpochMillis " +
            "ON daily_check_in_drafts(updatedAtEpochMillis)",
    )
}

private fun installDailyCheckInConstraints(database: SupportSQLiteDatabase) {
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS track_check_in_entries_ownership_insert
        BEFORE INSERT ON track_check_in_entries
        WHEN NOT EXISTS (
            SELECT 1
            FROM daily_check_ins checkIn
            JOIN recovery_tracks track ON track.id = NEW.recoveryTrackId
            WHERE checkIn.id = NEW.dailyCheckInId
              AND checkIn.ownerKey = NEW.ownerKey
              AND track.ownerKey = NEW.ownerKey
              AND track.programId = NEW.programId
        )
        BEGIN
            SELECT RAISE(ABORT, 'Check-in entry must match its owner, parent, and Recovery Track');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS track_check_in_entries_ownership_update
        BEFORE UPDATE OF dailyCheckInId, ownerKey, recoveryTrackId, programId
        ON track_check_in_entries
        WHEN NOT EXISTS (
            SELECT 1
            FROM daily_check_ins checkIn
            JOIN recovery_tracks track ON track.id = NEW.recoveryTrackId
            WHERE checkIn.id = NEW.dailyCheckInId
              AND checkIn.ownerKey = NEW.ownerKey
              AND track.ownerKey = NEW.ownerKey
              AND track.programId = NEW.programId
        )
        BEGIN
            SELECT RAISE(ABORT, 'Check-in entry must match its owner, parent, and Recovery Track');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS track_check_in_entries_goal_insert
        BEFORE INSERT ON track_check_in_entries
        WHEN NEW.goalVersionId IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM recovery_goal_versions goal
              WHERE goal.id = NEW.goalVersionId
                AND goal.recoveryTrackId = NEW.recoveryTrackId
          )
        BEGIN
            SELECT RAISE(ABORT, 'Goal version must belong to the Recovery Track');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS track_check_in_entries_goal_update
        BEFORE UPDATE OF goalVersionId, recoveryTrackId ON track_check_in_entries
        WHEN NEW.goalVersionId IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM recovery_goal_versions goal
              WHERE goal.id = NEW.goalVersionId
                AND goal.recoveryTrackId = NEW.recoveryTrackId
          )
        BEGIN
            SELECT RAISE(ABORT, 'Goal version must belong to the Recovery Track');
        END
        """.trimIndent(),
    )
}
