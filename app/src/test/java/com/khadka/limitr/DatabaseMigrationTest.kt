package com.khadka.limitr

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.data.local.appdatabase.DatabaseMigrations
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun allEstablishedSchemasPreserveRowsAndValidateAgainstRoom13() {
        (DatabaseMigrations.supportedVersions.map { it to false } + (9 to true)).forEach { (version, extra) ->
            val name = "migration-$version-$extra"
            context.deleteDatabase(name)
            fixture(name, version, extra)
            val db = Room.databaseBuilder(context, LimitrDatabase::class.java, name)
                .addMigrations(*DatabaseMigrations.ALL).build()
            try {
                val sql = db.openHelper.writableDatabase // invokes Room's real schema validation
                sql.query("SELECT * FROM LimitrTable WHERE appName = 'Original'").use {
                    assertTrue("v$version", it.moveToFirst())
                    assertEquals(1000, it.getLong(it.getColumnIndexOrThrow("blockedTime")))
                    assertEquals(10000, it.getLong(it.getColumnIndexOrThrow("remainingTime")))
                }
                sql.query("SELECT COUNT(*) FROM LegacyLimitrTable").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
                sql.query("SELECT * FROM BlockingSession").use {
                    if (version == 3) assertFalse(it.moveToFirst()) else {
                        assertTrue(it.moveToFirst())
                        assertEquals("example.target", it.getString(it.getColumnIndexOrThrow("packageName")))
                        assertEquals(11000, it.getLong(it.getColumnIndexOrThrow("endWall")))
                        assertEquals("timer", it.getString(it.getColumnIndexOrThrow("kind")))
                    }
                }
                if (version == 12) sql.query("SELECT noOfTimesBlocked FROM HistoryTable WHERE appName='Original'").use {
                    assertTrue(it.moveToFirst()); assertEquals(7, it.getInt(0))
                }
                if (extra) sql.query("SELECT isAppBlockedOrLimited FROM LegacyLimitrTable").use {
                    assertTrue(it.moveToFirst()); assertEquals("blocked", it.getString(0))
                }
            } finally { db.close() }
            // Reopen must not import cancelled/edited legacy rows a second time.
            val reopened = Room.databaseBuilder(context, LimitrDatabase::class.java, name).addMigrations(*DatabaseMigrations.ALL).build()
            reopened.openHelper.writableDatabase.execSQL("DELETE FROM BlockingSession")
            reopened.close()
            val again = Room.databaseBuilder(context, LimitrDatabase::class.java, name).addMigrations(*DatabaseMigrations.ALL).build()
            again.openHelper.readableDatabase.query("SELECT COUNT(*) FROM BlockingSession").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            again.close()
        }
    }

    @Test fun unknownVersionDoesNotWipeData() {
        val name = "unknown-version"
        context.deleteDatabase(name)
        fixture(name, 11, false)
        val db = Room.databaseBuilder(context, LimitrDatabase::class.java, name).addMigrations(*DatabaseMigrations.ALL).build()
        try {
            assertThrows(IllegalStateException::class.java) { db.openHelper.writableDatabase }
        } finally { db.close() }
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            raw.rawQuery("SELECT appName FROM LimitrTable", null).use { assertTrue(it.moveToFirst()); assertEquals("Original", it.getString(0)) }
        }
    }

    private fun fixture(name: String, version: Int, extra: Boolean) {
        val timeColumn = when (version) { 3, 4 -> "startTime"; 6 -> "Time"; else -> "blockedTime" }
        val fields = mutableListOf("appName TEXT NOT NULL PRIMARY KEY", "$timeColumn INTEGER", "remainingTime INTEGER")
        if (version >= 4) fields += "appPackage TEXT"
        if (version >= 6) fields += listOf("starTime INTEGER", "endTime INTEGER")
        if (version >= 8) fields += "notificationStatus INTEGER"
        if (extra) fields += "isAppBlockedOrLimited TEXT NOT NULL DEFAULT 'blocked'"
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE LimitrTable (${fields.joinToString()})")
            db.execSQL("INSERT INTO LimitrTable (appName, `$timeColumn`, remainingTime) VALUES ('Original', 1000, 10000)")
            if (version >= 4) db.execSQL("UPDATE LimitrTable SET appPackage='example.target'")
            if (version == 12) {
                db.execSQL("CREATE TABLE HistoryTable (appName TEXT NOT NULL PRIMARY KEY, noOfTimesBlocked INTEGER, appPackage TEXT NOT NULL, blockedDate INTEGER, starTime INTEGER, endTime INTEGER)")
                db.execSQL("INSERT INTO HistoryTable VALUES ('Original', 7, 'example.target', 900, 1000, 11000)")
            }
            db.version = version
        }
    }
}
