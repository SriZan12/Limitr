package com.khadka.limitr

import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khadka.limitr.usage.AppUsageReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt in on an isolated device: temporarily changes this app's usage AppOp and restores it. */
@RunWith(AndroidJUnit4::class)
class AppUsageInstrumentedTest {
    @Test fun realForegroundEventsAreMeasuredOnlyWithUsageAccess() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("usageChecks") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val target = instrumentation.context.packageName
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        val old = shell("appops get ${context.packageName} GET_USAGE_STATS")
        val previousMode = Regex("GET_USAGE_STATS: (allow|ignore|deny|default|foreground)").find(old)?.groupValues?.get(1) ?: "default"
        try {
            shell("appops set ${context.packageName} GET_USAGE_STATS ignore")
            assertNull(AppUsageReader.last24Hours(context))
            shell("appops set ${context.packageName} GET_USAGE_STATS allow")
            val before = AppUsageReader.last24Hours(context)?.get(target) ?: 0L
            context.startActivity(Intent().setComponent(ComponentName(target, LaunchFixtureActivity::class.java.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            // The fixture has no background work; its foreground span must be observable.
            SystemClock.sleep(3000)
            shell("input keyevent 3")
            withTimeout(15_000) {
                while ((AppUsageReader.last24Hours(context)?.get(target) ?: 0L) < before + 1000) delay(250)
            }
            val measured = AppUsageReader.last24Hours(context)
            assertNotNull(measured)
            assertTrue(measured!!.getValue(target) in 1000..AppUsageReader.DAY_MILLIS)
            shell("appops set ${context.packageName} GET_USAGE_STATS ignore")
            assertNull(AppUsageReader.last24Hours(context))
        } finally {
            shell("input keyevent 3")
            shell("appops set ${context.packageName} GET_USAGE_STATS $previousMode")
        }
    }
}
