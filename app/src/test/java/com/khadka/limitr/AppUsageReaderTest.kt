package com.khadka.limitr

import android.app.Application
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.usage.AppUsageReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUsageStatsManager.EventBuilder

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class AppUsageReaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = 3 * AppUsageReader.DAY_MILLIS
    private fun access(mode: Int) = shadowOf(context.getSystemService(AppOpsManager::class.java))
        .setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, mode)
    private fun event(pkg: String, at: Long, type: Int) {
        shadowOf(context.getSystemService(UsageStatsManager::class.java)).addEvent(EventBuilder.buildEvent()
            .setPackage(pkg).setClass("Main").setTimeStamp(at).setEventType(type).build())
    }

    @Test fun deniedAccessIsDifferentFromAnEmptyGrantedHistory() = runBlocking {
        access(AppOpsManager.MODE_IGNORED)
        assertNull(AppUsageReader.last24Hours(context, now))
        access(AppOpsManager.MODE_ALLOWED)
        assertEquals(emptyMap<String, Long>(), AppUsageReader.last24Hours(context, now))
    }

    @Suppress("DEPRECATION")
    @Test fun queriesLookbackButCountsOnlyForegroundInsideLast24Hours() = runBlocking {
        access(AppOpsManager.MODE_ALLOWED)
        val begin = now - AppUsageReader.DAY_MILLIS
        event("old", begin - 60_000, UsageEvents.Event.MOVE_TO_FOREGROUND)
        event("old", begin - 30_000, UsageEvents.Event.MOVE_TO_BACKGROUND)
        event("crossing", begin - 20_000, UsageEvents.Event.MOVE_TO_FOREGROUND)
        event("crossing", begin + 10_000, UsageEvents.Event.MOVE_TO_BACKGROUND)
        event("background_audio", now - 20_000, UsageEvents.Event.FOREGROUND_SERVICE_START)
        event("recent", now - 5000, UsageEvents.Event.MOVE_TO_FOREGROUND)
        event("recent", now - 1000, UsageEvents.Event.MOVE_TO_BACKGROUND)
        assertEquals(mapOf("crossing" to 10_000L, "recent" to 4000L), AppUsageReader.last24Hours(context, now))
    }

    @Suppress("DEPRECATION")
    @Test fun screenOffClosesAnOpenAppAndRevocationDoesNotReturnCachedUsage() = runBlocking {
        access(AppOpsManager.MODE_ALLOWED)
        event("app", now - 10_000, UsageEvents.Event.MOVE_TO_FOREGROUND)
        event("android", now - 7000, UsageEvents.Event.SCREEN_NON_INTERACTIVE)
        assertEquals(mapOf("app" to 3000L), AppUsageReader.last24Hours(context, now))
        access(AppOpsManager.MODE_IGNORED)
        assertNull(AppUsageReader.last24Hours(context, now))
    }
}
