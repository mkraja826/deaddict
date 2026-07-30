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
    fun migration4To5_createsGoalAwareDailyCheckInTables() {
        migrationHelper.createDatabase(DATABASE_NAME, 4).apply {
            insertTrackAndGoal()
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
                id, ownerKey, localDateEpochDay, mood, stress, energy, sleepQuality,
                createdAtEpochMillis, updatedAtEpochMillis, revision, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                CHECK_IN_ID,
                OWNER_KEY,
                20_000L,
                4,
                2,
                3,
                4,
                2_000L,
                2_000L,
                0,
                "LOCAL_ONLY",
            ),
        )
        database.execSQL(
            """
            INSERT INTO track_check_in_entries (
                id, dailyCheckInId, recoveryTrackId, goalVersionId, outcome,
                measuredValue, unitKey, peakUrge, privateNote,
                createdAtEpochMillis, updatedAtEpochMillis, revision, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                ENTRY_ID,
                CHECK_IN_ID,
                TRACK_ID,
                GOAL_ID,
                "GOAL_MET",
                0.0,
                "units",
                3,
                "private local context",
                2_000L,
                2_000L,
                0,
                "LOCAL_ONLY",
            ),
        )

        database.query(
            "SELECT recoveryTrackId, goalVersionId, privateNote FROM track_check_in_entries WHERE id = ?",
            arrayOf(ENTRY_ID),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getString(0) == TRACK_ID)
            assertTrue(cursor.getString(1) == GOAL_ID)
            assertTrue(cursor.getString(2) == "private local context")
        }
        database.query("PRAGMA foreign_key_check").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
        database.close()
    }

    @Test
    fun migration4To5_enforcesOneDailyCheckInPerOwnerAndDate() {
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
                id, ownerKey, localDateEpochDay, mood, stress, energy, sleepQuality,
                createdAtEpochMillis, updatedAtEpochMillis, revision, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(CHECK_IN_ID, OWNER_KEY, 20_000L, null, null, null, null, 1L, 1L, 0, "LOCAL_ONLY"),
        )

        val duplicate = runCatching {
            database.execSQL(
                """
                INSERT INTO daily_check_ins (
                    id, ownerKey, localDateEpochDay, mood, stress, energy, sleepQuality,
                    createdAtEpochMillis, updatedAtEpochMillis, revision, syncState
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(SECOND_CHECK_IN_ID, OWNER_KEY, 20_000L, null, null, null, null, 2L, 2L, 0, "LOCAL_ONLY"),
            )
        }

        assertTrue(duplicate.isFailure)
        database.close()
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertTrackAndGoal() {
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
                TRACK_ID,
                OWNER_KEY,
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
        execSQL(
            """
            INSERT INTO recovery_goal_versions (
                id, recoveryTrackId, goalType, targetValue, unitKey, periodType, title,
                effectiveFromEpochMillis, effectiveUntilEpochMillis,
                createdAtEpochMillis, updatedAtEpochMillis, revision, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                GOAL_ID,
                TRACK_ID,
                "AWARENESS_ONLY",
                null,
                null,
                null,
                null,
                500L,
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
        const val TRACK_ID = "00000000-0000-0000-0000-000000000201"
        const val GOAL_ID = "00000000-0000-0000-0000-000000000202"
        const val CHECK_IN_ID = "00000000-0000-0000-0000-000000000203"
        const val SECOND_CHECK_IN_ID = "00000000-0000-0000-0000-000000000204"
        const val ENTRY_ID = "00000000-0000-0000-0000-000000000205"
    }
}
