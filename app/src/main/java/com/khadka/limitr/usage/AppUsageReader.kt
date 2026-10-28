package com.khadka.limitr.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.khadka.limitr.utils.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object AppUsageReader {
    const val DAY_MILLIS = 86_400_000L

    /** Null means unavailable; an empty map means access is granted but no usage was observed. */
    suspend fun last24Hours(context: Context, now: Long = System.currentTimeMillis()): Map<String, Long>? = withContext(Dispatchers.IO) {
        if (!Permissions.isUsageStateManagerEnabled(context)) return@withContext null
        try {
            val manager = context.getSystemService(UsageStatsManager::class.java) ?: return@withContext null
            val begin = now - DAY_MILLIS
            // The extra day seeds activities already foreground at the window's start.
            // Aggregated UsageStats buckets may include time outside the requested window.
            val events = manager.queryEvents((begin - DAY_MILLIS).coerceAtLeast(0), now) ?: return@withContext null
            val usage = ForegroundUsage(begin, now, multiResume = Build.VERSION.SDK_INT >= 29)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                currentCoroutineContext().ensureActive()
                if (!events.getNextEvent(event)) break
                @Suppress("DEPRECATION")
                val kind = when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> ForegroundUsage.Kind.RESUME
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> ForegroundUsage.Kind.PAUSE
                    UsageEvents.Event.ACTIVITY_STOPPED -> ForegroundUsage.Kind.STOP
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> ForegroundUsage.Kind.SCREEN_OFF
                    UsageEvents.Event.DEVICE_SHUTDOWN -> ForegroundUsage.Kind.SHUTDOWN
                    UsageEvents.Event.DEVICE_STARTUP -> ForegroundUsage.Kind.STARTUP
                    else -> null
                }
                kind?.let { usage.event(event.timeStamp, it, event.packageName, event.className) }
            }
            // Access can be revoked while the background query is running.
            if (Permissions.isUsageStateManagerEnabled(context)) usage.result() else null
        } catch (_: SecurityException) { null }
    }
}
