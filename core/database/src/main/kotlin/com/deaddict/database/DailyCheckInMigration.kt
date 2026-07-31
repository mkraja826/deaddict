package com.deaddict.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_check_ins` (
                `id` TEXT NOT NULL,
                `ownerKey` TEXT NOT NULL,
                `localDateEpochDay` INTEGER NOT NULL,
                `mood` INTEGER,
                `stress` INTEGER,
                `energy` INTEGER,
                `sleepQuality` INTEGER,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL,
                `syncState` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_daily_check_ins_ownerKey` " +
                "ON `daily_check_ins` (`ownerKey`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_daily_check_ins_localDateEpochDay` " +
                "ON `daily_check_ins` (`localDateEpochDay`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_daily_check_ins_updatedAtEpochMillis` " +
                "ON `daily_check_ins` (`updatedAtEpochMillis`)",
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_check_ins_ownerKey_localDateEpochDay` " +
                "ON `daily_check_ins` (`ownerKey`, `localDateEpochDay`)",
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `track_check_in_entries` (
                `id` TEXT NOT NULL,
                `dailyCheckInId` TEXT NOT NULL,
                `recoveryTrackId` TEXT NOT NULL,
                `goalVersionId` TEXT,
                `outcome` TEXT NOT NULL,
                `measuredValue` REAL,
                `unitKey` TEXT,
                `peakUrge` INTEGER,
                `privateNote` TEXT,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `updatedAtEpochMillis` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL,
                `syncState` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`dailyCheckInId`) REFERENCES `daily_check_ins`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`recoveryTrackId`) REFERENCES `recovery_tracks`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`goalVersionId`) REFERENCES `recovery_goal_versions`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_track_check_in_entries_dailyCheckInId` " +
                "ON `track_check_in_entries` (`dailyCheckInId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_track_check_in_entries_recoveryTrackId` " +
                "ON `track_check_in_entries` (`recoveryTrackId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_track_check_in_entries_goalVersionId` " +
                "ON `track_check_in_entries` (`goalVersionId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_track_check_in_entries_updatedAtEpochMillis` " +
                "ON `track_check_in_entries` (`updatedAtEpochMillis`)",
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_track_check_in_entries_dailyCheckInId_recoveryTrackId` " +
                "ON `track_check_in_entries` (`dailyCheckInId`, `recoveryTrackId`)",
        )
    }
}
