package com.deaddict.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyCheckInMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DeAddictDatabase::class.java,
    )

    @Test
    fun migration4To5_createsCanonicalCheckInTablesAndUniqueness() {
        migrationHelper.createDatabase(DATABASE_NAME, 4).close()

        val database = migrationHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            5,
            true,
            MIGRATION_4_5,
        )

        database.execSQL(
            """
            INSERT INTO daily_check_ins (
                id, ownerKey, localDate, timezoneId, mood, stress, energy, sleep,
                triggerKeys, privateNote, completedAtEpochMillis,
                createdAtEpochMillis, updatedAtEpochMillis, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                CHECK_IN_ID,
                OWNER_KEY,
                "2026-07-30",
                "Asia/Kolkata",
                4,
                2,
                null,
                null,
                "stress",
                "private",
                3_000L,
                3_000L,
                3_000L,
                "LOCAL_ONLY",
            ),
        )

        val duplicate = runCatching {
            database.execSQL(
                """
                INSERT INTO daily_check_ins (
                    id, ownerKey, localDate, timezoneId, mood, stress, energy, sleep,
                    triggerKeys, privateNote, completedAtEpochMillis,
                    createdAtEpochMillis, updatedAtEpochMillis, syncState
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    SECOND_CHECK_IN_ID,
                    OWNER_KEY,
                    "2026-07-30",
                    "UTC",
                    null,
                    null,
                    null,
                    null,
                    "",
                    null,
                    4_000L,
                    4_000L,
                    4_000L,
                    "LOCAL_ONLY",
                ),
            )
        }

        assertTrue(duplicate.isFailure)
        database.query("PRAGMA foreign_key_check").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
        database.close()
    }

    @Test
    fun migration4To5_rejectsEntryFromAnotherOwner() {
        migrationHelper.createDatabase(DATABASE_NAME, 4).apply {
            insertTrack(TRACK_ID, TRACK_OWNER_KEY)
            close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            5,
            true,
            MIGRATION_4_5,
        )
        database.execSQL(
            """
            INSERT INTO daily_check_ins (
                id, ownerKey, localDate, timezoneId, mood, stress, energy, sleep,
                triggerKeys, privateNote, completedAtEpochMillis,
                createdAtEpochMillis, updatedAtEpochMillis, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                CHECK_IN_ID,
                OWNER_KEY,
                "2026-07-30",
                "Asia/Kolkata",
                null,
                null,
                null,
                null,
                "",
                null,
                3_000L,
                3_000L,
                3_000L,
                "LOCAL_ONLY",
            ),
        )

        val result = runCatching {
            database.execSQL(
                """
                INSERT INTO track_check_in_entries (
                    id, dailyCheckInId, ownerKey, recoveryTrackId, recoveryGoalVersionId,
                    outcome, urgeIntensity, quantity, quantityUnit, durationMinutes,
                    costMinorUnits, currencyCode, triggerKeys, privateNote,
                    createdAtEpochMillis, updatedAtEpochMillis, syncState
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    ENTRY_ID,
                    CHECK_IN_ID,
                    OWNER_KEY,
                    TRACK_ID,
                    null,
                    "ALIGNED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "",
                    null,
                    3_000L,
                    3_000L,
                    "LOCAL_ONLY",
                ),
            )
        }

        assertTrue(result.isFailure)
        database.close()
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertTrack(
        id: String,
        ownerKey: String,
    ) {
        execSQL(
            """
            INSERT INTO recovery_tracks (
                id, ownerKey, programId, displayAlias, role, status,
                startedAtEpochMillis, pausedAtEpochMillis, maintenanceAtEpochMillis,
                archivedAtEpochMillis, createdAtEpochMillis, updatedAtEpochMillis,
                revision, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                id,
                ownerKey,
                "gaming",
                null,
                "PRIMARY",
                "ACTIVE",
                500L,
                null,
                null,
                null,
                500L,
                500L,
                0,
                "LOCAL_ONLY",
            ),
        )
    }

    private companion object {
        const val DATABASE_NAME = "daily-check-in-migration-test"
        const val OWNER_KEY = "guest:profile-1"
        const val TRACK_OWNER_KEY = "guest:profile-2"
        const val CHECK_IN_ID = "00000000-0000-0000-0000-000000000301"
        const val SECOND_CHECK_IN_ID = "00000000-0000-0000-0000-000000000302"
        const val ENTRY_ID = "00000000-0000-0000-0000-000000000303"
        const val TRACK_ID = "00000000-0000-0000-0000-000000000101"
    }
}
