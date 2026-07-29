package com.deaddict.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryEventMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DeAddictDatabase::class.java,
    )

    @Test
    fun migration3To4_backfillsClosestJourneyAndPreservesSensitiveLocalData() {
        migrationHelper.createDatabase(DATABASE_NAME, 3).apply {
            insertTrack(
                id = ARCHIVED_TRACK_ID,
                ownerKey = OWNER_KEY,
                status = "ARCHIVED",
                startedAt = 100L,
                archivedAt = 1_000L,
            )
            insertTrack(
                id = ACTIVE_TRACK_ID,
                ownerKey = OWNER_KEY,
                status = "ACTIVE",
                startedAt = 2_000L,
                archivedAt = null,
            )
            execSQL(
                """
                INSERT INTO tracking_events (
                    id, programId, kind, quantity, unit, costMinorUnits,
                    urgeIntensity, triggerKey, occurredAtEpochMillis,
                    createdAtEpochMillis, privateNote, syncState
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    TRACKING_ID,
                    PROGRAM_ID,
                    "URGE",
                    null,
                    null,
                    null,
                    4,
                    "stress",
                    500L,
                    500L,
                    "preserve this private note",
                    "LOCAL_ONLY",
                ),
            )
            execSQL(
                """
                INSERT INTO rescue_sessions (
                    id, programId, startedAtEpochMillis, completedAtEpochMillis,
                    initialUrge, finalUrge, triggerKey, actionKeys, outcome, syncState
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    RESCUE_ID,
                    PROGRAM_ID,
                    2_500L,
                    2_600L,
                    5,
                    3,
                    "stress",
                    "breathe",
                    "REDUCED",
                    "PENDING",
                ),
            )
            close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            4,
            true,
            MIGRATION_3_4,
        )

        database.query(
            "SELECT ownerKey, recoveryTrackId, privateNote FROM tracking_events WHERE id = ?",
            arrayOf(TRACKING_ID),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(OWNER_KEY, cursor.getString(0))
            assertEquals(ARCHIVED_TRACK_ID, cursor.getString(1))
            assertEquals("preserve this private note", cursor.getString(2))
        }
        database.query(
            "SELECT ownerKey, recoveryTrackId, actionKeys FROM rescue_sessions WHERE id = ?",
            arrayOf(RESCUE_ID),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(OWNER_KEY, cursor.getString(0))
            assertEquals(ACTIVE_TRACK_ID, cursor.getString(1))
            assertEquals("breathe", cursor.getString(2))
        }
        database.query("PRAGMA foreign_key_check").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
        database.close()
    }

    @Test
    fun migration3To4_installsOwnerAndProgramConstraint() {
        migrationHelper.createDatabase(DATABASE_NAME, 3).apply {
            insertTrack(
                id = ACTIVE_TRACK_ID,
                ownerKey = OWNER_KEY,
                status = "ACTIVE",
                startedAt = 2_000L,
                archivedAt = null,
            )
            close()
        }
        val database = migrationHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            4,
            true,
            MIGRATION_3_4,
        )

        assertTrue(
            runCatching {
                database.execSQL(
                    """
                    INSERT INTO tracking_events (
                        id, ownerKey, recoveryTrackId, programId, kind, quantity, unit,
                        costMinorUnits, urgeIntensity, triggerKey, occurredAtEpochMillis,
                        createdAtEpochMillis, privateNote, syncState
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        "00000000-0000-0000-0000-000000000099",
                        "guest:another-owner",
                        ACTIVE_TRACK_ID,
                        PROGRAM_ID,
                        "URGE",
                        null,
                        null,
                        null,
                        3,
                        null,
                        3_000L,
                        3_000L,
                        null,
                        "LOCAL_ONLY",
                    ),
                )
            }.isFailure,
        )
        database.close()
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertTrack(
        id: String,
        ownerKey: String,
        status: String,
        startedAt: Long,
        archivedAt: Long?,
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
                PROGRAM_ID,
                null,
                if (status == "ACTIVE") "PRIMARY" else "SUPPORTING",
                status,
                startedAt,
                null,
                null,
                archivedAt,
                startedAt,
                archivedAt ?: startedAt,
                0,
                "LOCAL_ONLY",
            ),
        )
    }

    private companion object {
        const val DATABASE_NAME = "recovery-event-migration-test"
        const val OWNER_KEY = "guest:profile-1"
        const val PROGRAM_ID = "smoking"
        const val ARCHIVED_TRACK_ID = "00000000-0000-0000-0000-000000000201"
        const val ACTIVE_TRACK_ID = "00000000-0000-0000-0000-000000000202"
        const val TRACKING_ID = "00000000-0000-0000-0000-000000000210"
        const val RESCUE_ID = "00000000-0000-0000-0000-000000000220"
    }
}
