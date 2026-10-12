package com.khadka.limitr.blocking

import androidx.room.withTransaction
import android.content.SharedPreferences
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(private val database: LimitrDatabase, private val clock: SessionClock, private val preferences: SharedPreferences) {
    val changes = database.sessions().observe()
    suspend fun all() = database.sessions().all()

    suspend fun timer(packageName: String, appName: String, duration: Long, notifications: Boolean) {
        require(duration in 1..86_400_000L)
        val now = clock.now()
        save(BlockingSession(packageName, appName, UUID.randomUUID().toString(), "timer",
            now.wall, now.wall + duration, now.elapsed, duration, now.bootId, notifications))
    }

    suspend fun interval(packageName: String, appName: String, start: Long, end: Long, notifications: Boolean) {
        require(end > start)
        save(BlockingSession(packageName, appName, UUID.randomUUID().toString(), "interval",
            start, end, null, end - start, null, notifications))
    }

    private suspend fun save(session: BlockingSession) = database.withTransaction {
        database.sessions().save(session)
        database.openHelper.writableDatabase.execSQL("""
            INSERT OR REPLACE INTO HistoryTable (appName, noOfTimesBlocked, appPackage, blockedDate, starTime, endTime)
            VALUES (?, COALESCE((SELECT noOfTimesBlocked FROM HistoryTable WHERE appName = ?), 0) + 1, ?, ?, ?, ?)
        """.trimIndent(), arrayOf<Any>(session.appName, session.appName, session.packageName, clock.now().wall, session.startWall, session.endWall))
    }

    suspend fun cancel(session: BlockingSession): Boolean =
        database.sessions().cancel(session.packageName, session.revision) > 0

    suspend fun notifications(session: BlockingSession, enabled: Boolean) {
        database.sessions().notifications(session.packageName, session.revision, enabled)
    }

    /** Rebase only once after reboot/legacy migration, preventing subsequent clock changes extending a timer. */
    suspend fun reconcile(now: TimeSnapshot): List<BlockingSession> = database.withTransaction {
        database.sessions().all().forEach { original ->
            val session = if (original.revision.startsWith("legacy:")) {
                original.copy(revision = "imported:" + original.revision.removePrefix("legacy:"),
                    blockNotifications = original.blockNotifications || preferences.getBoolean(original.appName, false))
                    .also { database.sessions().save(it) }
            } else original
            val value = ScheduleEvaluator.evaluate(session, now)
            if (value.phase == Phase.EXPIRED) database.sessions().cancel(session.packageName, session.revision)
            else if (session.kind == "timer" && (session.bootId != now.bootId || session.startElapsed == null || session.startElapsed > now.elapsed)) {
                database.sessions().save(session.copy(startElapsed = now.elapsed,
                    duration = value.remaining, startWall = now.wall, endWall = now.wall + value.remaining, bootId = now.bootId))
            }
        }
        database.sessions().all()
    }
}
