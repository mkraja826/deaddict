package com.deaddict.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TEMP TABLE tracking_event_track_map AS
            SELECT legacy.id AS eventId,
                   COALESCE(
                       (
                           SELECT track.id
                           FROM recovery_tracks track
                           WHERE track.programId = legacy.programId
                             AND track.startedAtEpochMillis <= legacy.occurredAtEpochMillis
                             AND (
                                 track.archivedAtEpochMillis IS NULL OR
                                 legacy.occurredAtEpochMillis <= track.archivedAtEpochMillis
                             )
                           ORDER BY track.startedAtEpochMillis DESC, track.id
                           LIMIT 1
                       ),
                       (
                           SELECT track.id
                           FROM recovery_tracks track
                           WHERE track.programId = legacy.programId
                           ORDER BY track.startedAtEpochMillis DESC, track.id
                           LIMIT 1
                       )
                   ) AS recoveryTrackId
            FROM tracking_events legacy
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE tracking_events_v4 (
                id TEXT NOT NULL,
                ownerKey TEXT NOT NULL,
                recoveryTrackId TEXT,
                programId TEXT NOT NULL,
                kind TEXT NOT NULL,
                quantity REAL,
                unit TEXT,
                costMinorUnits INTEGER,
                urgeIntensity INTEGER,
                triggerKey TEXT,
                occurredAtEpochMillis INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                privateNote TEXT,
                syncState TEXT NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(recoveryTrackId) REFERENCES recovery_tracks(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO tracking_events_v4 (
                id,
                ownerKey,
                recoveryTrackId,
                programId,
                kind,
                quantity,
                unit,
                costMinorUnits,
                urgeIntensity,
                triggerKey,
                occurredAtEpochMillis,
                createdAtEpochMillis,
                privateNote,
                syncState
            )
            SELECT legacy.id,
                   COALESCE(track.ownerKey, '$LEGACY_OWNER_KEY'),
                   mapping.recoveryTrackId,
                   legacy.programId,
                   legacy.kind,
                   legacy.quantity,
                   legacy.unit,
                   legacy.costMinorUnits,
                   legacy.urgeIntensity,
                   legacy.triggerKey,
                   legacy.occurredAtEpochMillis,
                   legacy.createdAtEpochMillis,
                   legacy.privateNote,
                   legacy.syncState
            FROM tracking_events legacy
            LEFT JOIN tracking_event_track_map mapping ON mapping.eventId = legacy.id
            LEFT JOIN recovery_tracks track ON track.id = mapping.recoveryTrackId
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE tracking_events")
        database.execSQL("ALTER TABLE tracking_events_v4 RENAME TO tracking_events")
        database.execSQL("DROP TABLE tracking_event_track_map")
        createTrackingIndexes(database)

        database.execSQL(
            """
            CREATE TEMP TABLE rescue_session_track_map AS
            SELECT legacy.id AS sessionId,
                   COALESCE(
                       (
                           SELECT track.id
                           FROM recovery_tracks track
                           WHERE track.programId = legacy.programId
                             AND track.startedAtEpochMillis <= legacy.startedAtEpochMillis
                             AND (
                                 track.archivedAtEpochMillis IS NULL OR
                                 legacy.startedAtEpochMillis <= track.archivedAtEpochMillis
                             )
                           ORDER BY track.startedAtEpochMillis DESC, track.id
                           LIMIT 1
                       ),
                       (
                           SELECT track.id
                           FROM recovery_tracks track
                           WHERE track.programId = legacy.programId
                           ORDER BY track.startedAtEpochMillis DESC, track.id
                           LIMIT 1
                       )
                   ) AS recoveryTrackId
            FROM rescue_sessions legacy
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE rescue_sessions_v4 (
                id TEXT NOT NULL,
                ownerKey TEXT NOT NULL,
                recoveryTrackId TEXT,
                programId TEXT NOT NULL,
                startedAtEpochMillis INTEGER NOT NULL,
                completedAtEpochMillis INTEGER,
                initialUrge INTEGER NOT NULL,
                finalUrge INTEGER,
                triggerKey TEXT,
                actionKeys TEXT NOT NULL,
                outcome TEXT,
                syncState TEXT NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(recoveryTrackId) REFERENCES recovery_tracks(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            INSERT INTO rescue_sessions_v4 (
                id,
                ownerKey,
                recoveryTrackId,
                programId,
                startedAtEpochMillis,
                completedAtEpochMillis,
                initialUrge,
                finalUrge,
                triggerKey,
                actionKeys,
                outcome,
                syncState
            )
            SELECT legacy.id,
                   COALESCE(track.ownerKey, '$LEGACY_OWNER_KEY'),
                   mapping.recoveryTrackId,
                   legacy.programId,
                   legacy.startedAtEpochMillis,
                   legacy.completedAtEpochMillis,
                   legacy.initialUrge,
                   legacy.finalUrge,
                   legacy.triggerKey,
                   legacy.actionKeys,
                   legacy.outcome,
                   legacy.syncState
            FROM rescue_sessions legacy
            LEFT JOIN rescue_session_track_map mapping ON mapping.sessionId = legacy.id
            LEFT JOIN recovery_tracks track ON track.id = mapping.recoveryTrackId
            """.trimIndent(),
        )
        database.execSQL("DROP TABLE rescue_sessions")
        database.execSQL("ALTER TABLE rescue_sessions_v4 RENAME TO rescue_sessions")
        database.execSQL("DROP TABLE rescue_session_track_map")
        createRescueIndexes(database)

        installRecoveryEventConstraints(database)
    }
}

val RECOVERY_EVENT_DATABASE_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        installRecoveryEventConstraints(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        installRecoveryEventConstraints(db)
    }
}

private fun createTrackingIndexes(database: SupportSQLiteDatabase) {
    database.execSQL("CREATE INDEX index_tracking_events_ownerKey ON tracking_events(ownerKey)")
    database.execSQL("CREATE INDEX index_tracking_events_recoveryTrackId ON tracking_events(recoveryTrackId)")
    database.execSQL("CREATE INDEX index_tracking_events_programId ON tracking_events(programId)")
    database.execSQL("CREATE INDEX index_tracking_events_occurredAtEpochMillis ON tracking_events(occurredAtEpochMillis)")
    database.execSQL("CREATE INDEX index_tracking_events_syncState ON tracking_events(syncState)")
    database.execSQL(
        "CREATE INDEX index_tracking_events_recoveryTrackId_occurredAtEpochMillis " +
            "ON tracking_events(recoveryTrackId, occurredAtEpochMillis)",
    )
}

private fun createRescueIndexes(database: SupportSQLiteDatabase) {
    database.execSQL("CREATE INDEX index_rescue_sessions_ownerKey ON rescue_sessions(ownerKey)")
    database.execSQL("CREATE INDEX index_rescue_sessions_recoveryTrackId ON rescue_sessions(recoveryTrackId)")
    database.execSQL("CREATE INDEX index_rescue_sessions_programId ON rescue_sessions(programId)")
    database.execSQL("CREATE INDEX index_rescue_sessions_startedAtEpochMillis ON rescue_sessions(startedAtEpochMillis)")
    database.execSQL("CREATE INDEX index_rescue_sessions_syncState ON rescue_sessions(syncState)")
    database.execSQL(
        "CREATE INDEX index_rescue_sessions_recoveryTrackId_startedAtEpochMillis " +
            "ON rescue_sessions(recoveryTrackId, startedAtEpochMillis)",
    )
}

private fun installRecoveryEventConstraints(database: SupportSQLiteDatabase) {
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS tracking_events_track_owner_insert
        BEFORE INSERT ON tracking_events
        WHEN NEW.recoveryTrackId IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM recovery_tracks track
              WHERE track.id = NEW.recoveryTrackId
                AND track.ownerKey = NEW.ownerKey
                AND track.programId = NEW.programId
          )
        BEGIN
            SELECT RAISE(ABORT, 'Tracking event must match its Recovery Track owner and program');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS tracking_events_track_owner_update
        BEFORE UPDATE OF ownerKey, recoveryTrackId, programId ON tracking_events
        WHEN NEW.recoveryTrackId IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM recovery_tracks track
              WHERE track.id = NEW.recoveryTrackId
                AND track.ownerKey = NEW.ownerKey
                AND track.programId = NEW.programId
          )
        BEGIN
            SELECT RAISE(ABORT, 'Tracking event must match its Recovery Track owner and program');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS rescue_sessions_track_owner_insert
        BEFORE INSERT ON rescue_sessions
        WHEN NEW.recoveryTrackId IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM recovery_tracks track
              WHERE track.id = NEW.recoveryTrackId
                AND track.ownerKey = NEW.ownerKey
                AND track.programId = NEW.programId
          )
        BEGIN
            SELECT RAISE(ABORT, 'Rescue session must match its Recovery Track owner and program');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS rescue_sessions_track_owner_update
        BEFORE UPDATE OF ownerKey, recoveryTrackId, programId ON rescue_sessions
        WHEN NEW.recoveryTrackId IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM recovery_tracks track
              WHERE track.id = NEW.recoveryTrackId
                AND track.ownerKey = NEW.ownerKey
                AND track.programId = NEW.programId
          )
        BEGIN
            SELECT RAISE(ABORT, 'Rescue session must match its Recovery Track owner and program');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS recovery_tracks_cascade_event_owner
        AFTER UPDATE OF ownerKey, syncState ON recovery_tracks
        WHEN NEW.ownerKey != OLD.ownerKey OR NEW.syncState != OLD.syncState
        BEGIN
            UPDATE tracking_events
            SET ownerKey = NEW.ownerKey,
                syncState = NEW.syncState
            WHERE recoveryTrackId = NEW.id;
            UPDATE rescue_sessions
            SET ownerKey = NEW.ownerKey,
                syncState = NEW.syncState
            WHERE recoveryTrackId = NEW.id;
        END
        """.trimIndent(),
    )
}

private const val LEGACY_OWNER_KEY = "legacy-local"
