package com.deaddict.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryTrackMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DeAddictDatabase::class.java,
    )

    @Test
    fun migration2To3_preservesLegacyDataAndCreatesRecoveryTracks() {
        migrationHelper.createDatabase(DATABASE_NAME, 2).apply {
            insertLegacyPrograms()
            insertLegacyTrackingEvent()
            insertLegacyRescueSession()
            insertLegacyOutboxItem()
            close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            3,
            true,
            MIGRATION_2_3,
        )

        assertEquals(3, database.count("recovery_tracks"))
        assertEquals(3, database.count("recovery_goal_versions"))
        assertEquals(1, database.count("tracking_events"))
        assertEquals(1, database.count("rescue_sessions"))
        assertEquals(1, database.count("sync_outbox"))

        database.query(
            """
            SELECT id, ownerKey, role, status
            FROM recovery_tracks
            ORDER BY startedAtEpochMillis, id
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(FIRST_TRACK_ID, cursor.getString(0))
            assertEquals("legacy-local", cursor.getString(1))
            assertEquals("PRIMARY", cursor.getString(2))
            assertEquals("ACTIVE", cursor.getString(3))

            assertTrue(cursor.moveToNext())
            assertEquals(SECOND_TRACK_ID, cursor.getString(0))
            assertEquals("SUPPORTING", cursor.getString(2))
            assertEquals("ACTIVE", cursor.getString(3))

            assertTrue(cursor.moveToNext())
            assertEquals(ARCHIVED_TRACK_ID, cursor.getString(0))
            assertEquals("SUPPORTING", cursor.getString(2))
            assertEquals("ARCHIVED", cursor.getString(3))
            assertFalse(cursor.moveToNext())
        }

        database.query(
            """
            SELECT goalType, targetValue, effectiveUntilEpochMillis
            FROM recovery_goal_versions
            WHERE recoveryTrackId = ?
            """.trimIndent(),
            arrayOf(ARCHIVED_TRACK_ID),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("AWARENESS_ONLY", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertEquals(3_000L, cursor.getLong(2))
        }

        database.query(
            "SELECT privateNote FROM tracking_events WHERE id = ?",
            arrayOf(TRACKING_EVENT_ID),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("local-only note", cursor.getString(0))
        }

        database.query("PRAGMA foreign_key_check").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
        database.close()
    }

    @Test
    fun migration2To3_installsRecoveryTrackConstraints() {
        migrationHelper.createDatabase(DATABASE_NAME, 2).apply {
            insertLegacyPrograms()
            close()
        }

        val database = migrationHelper.runMigrationsAndValidate(
            DATABASE_NAME,
            3,
            true,
            MIGRATION_2_3,
        )

        assertFails {
            database.execSQL(
                """
                INSERT INTO recovery_tracks (
                    id, ownerKey, programId, displayAlias, role, status,
                    startedAtEpochMillis, pausedAtEpochMillis, maintenanceAtEpochMillis,
                    archivedAtEpochMillis, createdAtEpochMillis, updatedAtEpochMillis,
                    revision, syncState
                ) VALUES (
                    '00000000-0000-0000-0000-000000000099',
                    'legacy-local',
                    'new-program',
                    NULL,
                    'PRIMARY',
                    'ACTIVE',
                    4000,
                    NULL,
                    NULL,
                    NULL,
                    4000,
                    4000,
                    0,
                    'LOCAL_ONLY'
                )
                """.trimIndent(),
            )
        }

        assertFails {
            database.execSQL(
                """
                INSERT INTO recovery_tracks (
                    id, ownerKey, programId, displayAlias, role, status,
                    startedAtEpochMillis, pausedAtEpochMillis, maintenanceAtEpochMillis,
                    archivedAtEpochMillis, createdAtEpochMillis, updatedAtEpochMillis,
                    revision, syncState
                ) VALUES (
                    '00000000-0000-0000-0000-000000000098',
                    'legacy-local',
                    'smoking',
                    NULL,
                    'SUPPORTING',
                    'ACTIVE',
                    4000,
                    NULL,
                    NULL,
                    NULL,
                    4000,
                    4000,
                    0,
                    'LOCAL_ONLY'
                )
                """.trimIndent(),
            )
        }

        val currentGoalId = database.query(
            """
            SELECT id FROM recovery_goal_versions
            WHERE recoveryTrackId = ? AND effectiveUntilEpochMillis IS NULL
            """.trimIndent(),
            arrayOf(FIRST_TRACK_ID),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getString(0)
        }
        assertNotNull(currentGoalId)

        assertFails {
            database.execSQL(
                """
                INSERT INTO recovery_goal_versions (
                    id, recoveryTrackId, goalType, targetValue, unitKey, periodType,
                    title, effectiveFromEpochMillis, effectiveUntilEpochMillis,
                    createdAtEpochMillis, updatedAtEpochMillis, revision, syncState
                ) VALUES (
                    '00000000-0000-0000-0000-000000000097',
                    '$FIRST_TRACK_ID',
                    'AWARENESS_ONLY',
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    4000,
                    NULL,
                    4000,
                    4000,
                    0,
                    'LOCAL_ONLY'
                )
                """.trimIndent(),
            )
        }
        database.close()
    }

    private fun SupportSQLiteDatabase.insertLegacyPrograms() {
        execSQL(
            "INSERT INTO active_programs VALUES (?, ?, ?, ?, ?)",
            arrayOf(FIRST_TRACK_ID, "smoking", 1_000L, null, "LOCAL_ONLY"),
        )
        execSQL(
            "INSERT INTO active_programs VALUES (?, ?, ?, ?, ?)",
            arrayOf(SECOND_TRACK_ID, "social-media", 2_000L, null, "PENDING"),
        )
        execSQL(
            "INSERT INTO active_programs VALUES (?, ?, ?, ?, ?)",
            arrayOf(ARCHIVED_TRACK_ID, "gaming", 500L, 3_000L, "SYNCED"),
        )
    }

    private fun SupportSQLiteDatabase.insertLegacyTrackingEvent() {
        execSQL(
            """
            INSERT INTO tracking_events (
                id, programId, kind, quantity, unit, costMinorUnits,
                urgeIntensity, triggerKey, occurredAtEpochMillis,
                createdAtEpochMillis, privateNote, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                TRACKING_EVENT_ID,
                "smoking",
                "URGE",
                null,
                null,
                null,
                4,
                "stress",
                2_500L,
                2_500L,
                "local-only note",
                "LOCAL_ONLY",
            ),
        )
    }

    private fun SupportSQLiteDatabase.insertLegacyRescueSession() {
        execSQL(
            """
            INSERT INTO rescue_sessions (
                id, programId, startedAtEpochMillis, completedAtEpochMillis,
                initialUrge, finalUrge, triggerKey, actionKeys, outcome, syncState
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                RESCUE_SESSION_ID,
                "smoking",
                2_600L,
                2_700L,
                5,
                3,
                "stress",
                "breathe",
                "REDUCED",
                "PENDING",
            ),
        )
    }

    private fun SupportSQLiteDatabase.insertLegacyOutboxItem() {
        execSQL(
            """
            INSERT INTO sync_outbox (
                id, idempotencyKey, aggregateType, aggregateId, operation,
                payload, createdAtEpochMillis, attemptCount,
                nextAttemptAtEpochMillis, state, lastErrorCode
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                OUTBOX_ID,
                "tracking:$TRACKING_EVENT_ID",
                "TRACKING_EVENT",
                TRACKING_EVENT_ID,
                "UPSERT",
                "{}",
                2_500L,
                0,
                2_500L,
                "PENDING",
                null,
            ),
        )
    }

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }

    private fun assertFails(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }

    private companion object {
        const val DATABASE_NAME = "recovery-track-migration-test"
        const val FIRST_TRACK_ID = "00000000-0000-0000-0000-000000000001"
        const val SECOND_TRACK_ID = "00000000-0000-0000-0000-000000000002"
        const val ARCHIVED_TRACK_ID = "00000000-0000-0000-0000-000000000003"
        const val TRACKING_EVENT_ID = "00000000-0000-0000-0000-000000000010"
        const val RESCUE_SESSION_ID = "00000000-0000-0000-0000-000000000020"
        const val OUTBOX_ID = "00000000-0000-0000-0000-000000000030"
    }
}
