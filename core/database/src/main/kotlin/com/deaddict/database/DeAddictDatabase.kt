package com.deaddict.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.deaddict.database.dao.DailyCheckInDao
import com.deaddict.database.dao.ProgramDao
import com.deaddict.database.dao.RecoveryGoalDao
import com.deaddict.database.dao.RecoveryTrackDao
import com.deaddict.database.dao.RescueDao
import com.deaddict.database.dao.SyncOutboxDao
import com.deaddict.database.dao.TrackingDao
import com.deaddict.database.entity.ActiveProgramEntity
import com.deaddict.database.entity.DailyCheckInDraftEntity
import com.deaddict.database.entity.DailyCheckInEntity
import com.deaddict.database.entity.RecoveryGoalVersionEntity
import com.deaddict.database.entity.RecoveryTrackEntity
import com.deaddict.database.entity.RescueSessionEntity
import com.deaddict.database.entity.SyncOutboxEntity
import com.deaddict.database.entity.TrackCheckInEntryEntity
import com.deaddict.database.entity.TrackingEventEntity

@Database(
    entities = [
        ActiveProgramEntity::class,
        RecoveryTrackEntity::class,
        RecoveryGoalVersionEntity::class,
        TrackingEventEntity::class,
        RescueSessionEntity::class,
        DailyCheckInEntity::class,
        TrackCheckInEntryEntity::class,
        DailyCheckInDraftEntity::class,
        SyncOutboxEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class DeAddictDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun recoveryTrackDao(): RecoveryTrackDao
    abstract fun recoveryGoalDao(): RecoveryGoalDao
    abstract fun trackingDao(): TrackingDao
    abstract fun rescueDao(): RescueDao
    abstract fun dailyCheckInDao(): DailyCheckInDao
    abstract fun syncOutboxDao(): SyncOutboxDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tracking_events ADD COLUMN triggerKey TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_tracks` (
                `id` TEXT NOT NULL,
                `ownerKey` TEXT NOT NULL,
                `programId` TEXT NOT NULL,
                `displayAlias` TEXT,
                `role` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `startedAtEpochMillis` INTEGER NOT NULL,
                `pausedAtEpochMillis` INTEGER,
                `maintenanceAtEpochMillis` INTEGER,
                `archivedAtEpochMillis` INTEGER,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL,
                `syncState` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_tracks_ownerKey` " +
                "ON `recovery_tracks` (`ownerKey`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_tracks_programId` " +
                "ON `recovery_tracks` (`programId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_tracks_role` " +
                "ON `recovery_tracks` (`role`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_tracks_status` " +
                "ON `recovery_tracks` (`status`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_tracks_updatedAtEpochMillis` " +
                "ON `recovery_tracks` (`updatedAtEpochMillis`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_tracks_ownerKey_programId` " +
                "ON `recovery_tracks` (`ownerKey`, `programId`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_goal_versions` (
                `id` TEXT NOT NULL,
                `recoveryTrackId` TEXT NOT NULL,
                `goalType` TEXT NOT NULL,
                `targetValue` REAL,
                `unitKey` TEXT,
                `periodType` TEXT,
                `title` TEXT,
                `effectiveFromEpochMillis` INTEGER NOT NULL,
                `effectiveUntilEpochMillis` INTEGER,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL,
                `syncState` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recoveryTrackId`) REFERENCES `recovery_tracks`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_goal_versions_recoveryTrackId` " +
                "ON `recovery_goal_versions` (`recoveryTrackId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_goal_versions_effectiveFromEpochMillis` " +
                "ON `recovery_goal_versions` (`effectiveFromEpochMillis`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_goal_versions_effectiveUntilEpochMillis` " +
                "ON `recovery_goal_versions` (`effectiveUntilEpochMillis`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_recovery_goal_versions_recoveryTrackId_effectiveUntilEpochMillis` " +
                "ON `recovery_goal_versions` (`recoveryTrackId`, `effectiveUntilEpochMillis`)",
        )

        database.execSQL(
            """
            INSERT INTO recovery_tracks (
                id,
                ownerKey,
                programId,
                displayAlias,
                role,
                status,
                startedAtEpochMillis,
                pausedAtEpochMillis,
                maintenanceAtEpochMillis,
                archivedAtEpochMillis,
                createdAtEpochMillis,
                updatedAtEpochMillis,
                revision,
                syncState
            )
            SELECT
                legacy.id,
                '$LEGACY_OWNER_KEY',
                legacy.programId,
                NULL,
                CASE
                    WHEN legacy.archivedAtEpochMillis IS NULL
                     AND legacy.id = (
                        SELECT candidate.id
                        FROM active_programs candidate
                        WHERE candidate.archivedAtEpochMillis IS NULL
                        ORDER BY candidate.activatedAtEpochMillis, candidate.id
                        LIMIT 1
                     )
                    THEN 'PRIMARY'
                    ELSE 'SUPPORTING'
                END,
                CASE
                    WHEN legacy.archivedAtEpochMillis IS NULL THEN 'ACTIVE'
                    ELSE 'ARCHIVED'
                END,
                legacy.activatedAtEpochMillis,
                NULL,
                NULL,
                legacy.archivedAtEpochMillis,
                legacy.activatedAtEpochMillis,
                COALESCE(legacy.archivedAtEpochMillis, legacy.activatedAtEpochMillis),
                0,
                legacy.syncState
            FROM active_programs legacy
            """.trimIndent(),
        )

        database.execSQL(
            """
            INSERT INTO recovery_goal_versions (
                id,
                recoveryTrackId,
                goalType,
                targetValue,
                unitKey,
                periodType,
                title,
                effectiveFromEpochMillis,
                effectiveUntilEpochMillis,
                createdAtEpochMillis,
                updatedAtEpochMillis,
                revision,
                syncState
            )
            SELECT
                lower(
                    hex(randomblob(4)) || '-' ||
                    hex(randomblob(2)) || '-' ||
                    hex(randomblob(2)) || '-' ||
                    hex(randomblob(2)) || '-' ||
                    hex(randomblob(6))
                ),
                track.id,
                'AWARENESS_ONLY',
                NULL,
                NULL,
                NULL,
                NULL,
                track.startedAtEpochMillis,
                track.archivedAtEpochMillis,
                track.createdAtEpochMillis,
                track.updatedAtEpochMillis,
                0,
                track.syncState
            FROM recovery_tracks track
            """.trimIndent(),
        )

        installRecoveryTrackConstraints(database)
    }
}

val RECOVERY_TRACK_DATABASE_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        installRecoveryTrackConstraints(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        installRecoveryTrackConstraints(db)
    }
}

private fun installRecoveryTrackConstraints(database: SupportSQLiteDatabase) {
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS recovery_tracks_one_primary_insert
        BEFORE INSERT ON recovery_tracks
        WHEN NEW.role = 'PRIMARY'
          AND NEW.status IN ('ACTIVE', 'MAINTENANCE')
          AND EXISTS (
              SELECT 1 FROM recovery_tracks current
              WHERE current.ownerKey = NEW.ownerKey
                AND current.role = 'PRIMARY'
                AND current.status IN ('ACTIVE', 'MAINTENANCE')
          )
        BEGIN
            SELECT RAISE(ABORT, 'Only one current primary Recovery Track is allowed');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS recovery_tracks_one_primary_update
        BEFORE UPDATE OF ownerKey, role, status ON recovery_tracks
        WHEN NEW.role = 'PRIMARY'
          AND NEW.status IN ('ACTIVE', 'MAINTENANCE')
          AND EXISTS (
              SELECT 1 FROM recovery_tracks current
              WHERE current.ownerKey = NEW.ownerKey
                AND current.id != OLD.id
                AND current.role = 'PRIMARY'
                AND current.status IN ('ACTIVE', 'MAINTENANCE')
          )
        BEGIN
            SELECT RAISE(ABORT, 'Only one current primary Recovery Track is allowed');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS recovery_tracks_one_open_program_insert
        BEFORE INSERT ON recovery_tracks
        WHEN NEW.status IN ('ACTIVE', 'PAUSED', 'MAINTENANCE')
          AND EXISTS (
              SELECT 1 FROM recovery_tracks current
              WHERE current.ownerKey = NEW.ownerKey
                AND current.programId = NEW.programId
                AND current.status IN ('ACTIVE', 'PAUSED', 'MAINTENANCE')
          )
        BEGIN
            SELECT RAISE(ABORT, 'Only one open Recovery Track per program is allowed');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS recovery_tracks_one_open_program_update
        BEFORE UPDATE OF ownerKey, programId, status ON recovery_tracks
        WHEN NEW.status IN ('ACTIVE', 'PAUSED', 'MAINTENANCE')
          AND EXISTS (
              SELECT 1 FROM recovery_tracks current
              WHERE current.ownerKey = NEW.ownerKey
                AND current.programId = NEW.programId
                AND current.id != OLD.id
                AND current.status IN ('ACTIVE', 'PAUSED', 'MAINTENANCE')
          )
        BEGIN
            SELECT RAISE(ABORT, 'Only one open Recovery Track per program is allowed');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS recovery_goal_versions_one_current_insert
        BEFORE INSERT ON recovery_goal_versions
        WHEN NEW.effectiveUntilEpochMillis IS NULL
          AND EXISTS (
              SELECT 1 FROM recovery_goal_versions current
              WHERE current.recoveryTrackId = NEW.recoveryTrackId
                AND current.effectiveUntilEpochMillis IS NULL
          )
        BEGIN
            SELECT RAISE(ABORT, 'Only one current goal version per Recovery Track is allowed');
        END
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS recovery_goal_versions_one_current_update
        BEFORE UPDATE OF recoveryTrackId, effectiveUntilEpochMillis ON recovery_goal_versions
        WHEN NEW.effectiveUntilEpochMillis IS NULL
          AND EXISTS (
              SELECT 1 FROM recovery_goal_versions current
              WHERE current.recoveryTrackId = NEW.recoveryTrackId
                AND current.id != OLD.id
                AND current.effectiveUntilEpochMillis IS NULL
          )
        BEGIN
            SELECT RAISE(ABORT, 'Only one current goal version per Recovery Track is allowed');
        END
        """.trimIndent(),
    )
}

private const val LEGACY_OWNER_KEY = "legacy-local"
