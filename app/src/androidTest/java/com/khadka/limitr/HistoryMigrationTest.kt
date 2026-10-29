package com.khadka.limitr

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khadka.limitr.data.local.appdatabase.DatabaseMigrations
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LimitrDatabase::class.java)

    @Test fun version13PreservesHistorySessionsAndUnknownPackages() {
        val name = "history-migration-test"
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
        helper.createDatabase(name, 13).apply {
            execSQL("INSERT INTO HistoryTable VALUES ('Original', 7, 'example.target', 900, 1000, 11000)")
            execSQL("INSERT INTO HistoryTable VALUES ('Unknown', NULL, '', NULL, NULL, NULL)")
            execSQL("INSERT INTO LimitrTable VALUES ('Legacy', 10, 20, NULL, NULL, NULL, NULL)")
            execSQL("INSERT INTO BlockingSession VALUES ('example.target', 'Original', 'revision', 'timer', 1000, 11000, 500, 10000, 'boot', 1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 14, true, DatabaseMigrations.MIGRATION_13_14).use { db ->
            db.query("SELECT * FROM HistoryTable WHERE appName='Original'").use {
                assertTrue(it.moveToFirst())
                assertEquals(7, it.getInt(it.getColumnIndexOrThrow("noOfTimesBlocked")))
                assertEquals("example.target", it.getString(it.getColumnIndexOrThrow("appPackage")))
                assertEquals(900L, it.getLong(it.getColumnIndexOrThrow("blockedDate")))
                assertEquals(1000L, it.getLong(it.getColumnIndexOrThrow("starTime")))
                assertEquals(11000L, it.getLong(it.getColumnIndexOrThrow("endTime")))
            }
            db.query("SELECT * FROM HistoryTable WHERE appName='Unknown'").use {
                assertTrue(it.moveToFirst())
                assertEquals("", it.getString(it.getColumnIndexOrThrow("appPackage")))
                for (column in listOf("noOfTimesBlocked", "blockedDate", "starTime", "endTime")) {
                    assertTrue(it.isNull(it.getColumnIndexOrThrow(column)))
                }
            }
            db.query("SELECT revision, blockNotifications FROM BlockingSession").use {
                assertTrue(it.moveToFirst()); assertEquals("revision", it.getString(0)); assertEquals(1, it.getInt(1))
            }
            db.query("SELECT remainingTime FROM LimitrTable").use {
                assertTrue(it.moveToFirst()); assertEquals(20L, it.getLong(0))
            }
            db.execSQL("INSERT INTO HistoryTable VALUES ('Original', 1, 'another.package', 1000, NULL, NULL)")
            db.query("SELECT COUNT(*) FROM HistoryTable WHERE appName='Original'").use {
                assertTrue(it.moveToFirst()); assertEquals(2, it.getInt(0))
            }
            db.query("EXPLAIN QUERY PLAN SELECT * FROM HistoryTable ORDER BY blockedDate DESC").use {
                assertTrue(it.moveToFirst()); assertTrue(it.getString(3).contains("index_HistoryTable_blockedDate"))
            }
        }
        // Reopen through Room, not just the raw migration helper, to verify generated identity.
        val reopened = androidx.room.Room.databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            LimitrDatabase::class.java, name).addMigrations(*DatabaseMigrations.ALL).build()
        try {
            reopened.openHelper.readableDatabase.query("SELECT COUNT(*) FROM HistoryTable").use { rows ->
                assertTrue(rows.moveToFirst()); assertEquals(3, rows.getInt(0))
            }
        } finally { reopened.close() }
    }
}
