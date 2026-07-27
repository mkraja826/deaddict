package com.deaddict.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = DeAddictDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2PreservesTrackingAndAddsTrigger() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO tracking_events (
                  id, programId, kind, quantity, unit, costMinorUnits, urgeIntensity,
                  occurredAtEpochMillis, createdAtEpochMillis, privateNote, syncState
                ) VALUES (
                  'event-1', 'gaming', 'URGE', NULL, NULL, NULL, 4,
                  1000, 1000, 'local note', 'LOCAL_ONLY'
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { database ->
            database.query(
                "SELECT triggerKey, privateNote FROM tracking_events WHERE id = 'event-1'",
            ).use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.isNull(0))
                check(cursor.getString(1) == "local note")
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}

