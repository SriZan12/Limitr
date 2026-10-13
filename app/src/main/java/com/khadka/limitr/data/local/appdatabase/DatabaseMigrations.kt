package com.khadka.limitr.data.local.appdatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Schemas established from Git, not invented historical Room exports. Unknown versions fail closed. */
object DatabaseMigrations {
    val supportedVersions = listOf(3, 4, 6, 7, 8, 9, 10, 12)
    val ALL: Array<Migration> = supportedVersions.map { version ->
        object : Migration(version, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(`LimitrTable`)").use { cursor ->
                    while (cursor.moveToNext()) columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                // Add columns in place: do not drop legacy rows, extra columns, or historical data.
                if ("blockedTime" !in columns) {
                    db.execSQL("ALTER TABLE LimitrTable ADD COLUMN blockedTime INTEGER")
                    val old = when { "Time" in columns -> "Time"; "startTime" in columns -> "startTime"; else -> null }
                    if (old != null) db.execSQL("UPDATE LimitrTable SET blockedTime = `$old`")
                }
                for ((name, type) in listOf("appPackage" to "TEXT", "starTime" to "INTEGER", "endTime" to "INTEGER", "notificationStatus" to "INTEGER")) {
                    if (name !in columns) db.execSQL("ALTER TABLE LimitrTable ADD COLUMN `$name` $type")
                }
                db.execSQL("CREATE TABLE IF NOT EXISTS `HistoryTable` (`appName` TEXT NOT NULL, `noOfTimesBlocked` INTEGER, `appPackage` TEXT NOT NULL, `blockedDate` INTEGER, `starTime` INTEGER, `endTime` INTEGER, PRIMARY KEY(`appName`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `BlockingSession` (`packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `revision` TEXT NOT NULL, `kind` TEXT NOT NULL, `startWall` INTEGER NOT NULL, `endWall` INTEGER NOT NULL, `startElapsed` INTEGER, `duration` INTEGER NOT NULL, `bootId` TEXT, `blockNotifications` INTEGER NOT NULL, PRIMARY KEY(`packageName`))")
                db.execSQL("""
                    INSERT OR REPLACE INTO BlockingSession
                    SELECT appPackage, appName, 'legacy:' || appName,
                        CASE WHEN starTime IS NOT NULL AND endTime IS NOT NULL THEN 'interval' ELSE 'timer' END,
                        COALESCE(starTime, blockedTime, 0),
                        COALESCE(endTime, COALESCE(blockedTime, 0) + COALESCE(remainingTime, 0)),
                        NULL,
                        CASE WHEN starTime IS NOT NULL AND endTime IS NOT NULL THEN endTime - starTime ELSE COALESCE(remainingTime, 0) END,
                        NULL, COALESCE(notificationStatus, 0)
                    FROM LimitrTable WHERE appPackage IS NOT NULL AND appPackage != ''
                    ORDER BY COALESCE(blockedTime, 0), appName
                """.trimIndent())
                // Legacy columns differ even within version 9. Archive verbatim; canonical table is
                // copied without deleting the archive, so Room can validate every established variant.
                db.execSQL("ALTER TABLE LimitrTable RENAME TO LegacyLimitrTable")
                db.execSQL("CREATE TABLE LimitrTable (appName TEXT NOT NULL PRIMARY KEY, blockedTime INTEGER, remainingTime INTEGER, appPackage TEXT, starTime INTEGER, endTime INTEGER, notificationStatus INTEGER)")
                db.execSQL("INSERT INTO LimitrTable SELECT appName, blockedTime, remainingTime, appPackage, starTime, endTime, notificationStatus FROM LegacyLimitrTable")
            }
        }
    }.toTypedArray()
}
