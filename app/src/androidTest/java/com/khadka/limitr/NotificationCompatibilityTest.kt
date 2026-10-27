package com.khadka.limitr

import android.content.ComponentName
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khadka.limitr.services.notifications.NotificationListener
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationCompatibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fixture = instrumentation.context.packageName
    private val receiver = "$fixture/${NotificationFixtureReceiver::class.java.name}"
    private val entry = EntryPointAccessors.fromApplication(context, TestSessions::class.java)

    @Test fun onlySelectedNonOngoingNotificationsAreHiddenAcrossTimingBoundaries() = runBlocking {
        assumeTrue("Requires an isolated device and -e notificationChecks true",
            InstrumentationRegistry.getArguments().getString("notificationChecks") == "true")
        val component = ComponentName(context, NotificationListener::class.java).flattenToString()
        val old = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val wasGranted = if (Build.VERSION.SDK_INT >= 27) context.getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(ComponentName(context, NotificationListener::class.java))
            else component in old.orEmpty().split(':')
        try {
            if (Build.VERSION.SDK_INT >= 33) shell("pm grant $fixture android.permission.POST_NOTIFICATIONS")
            val enabled = (old.orEmpty().split(':').filter { it.isNotBlank() } + component).distinct().joinToString(":")
            if (Build.VERSION.SDK_INT >= 27) shell("cmd notification allow_listener $component")
            else shell("settings put secure enabled_notification_listeners $enabled")
            broadcast("qa.CLEAR")
            broadcast("qa.POST", "--ei id 201")
            broadcast("qa.POST", "--ei id 202 --ez ongoing true")
            await { ids().containsAll(listOf(201, 202)) }

            entry.repository().timer(fixture, "Notification fixture", 60000, false)
            entry.coordinator().refresh()
            broadcast("qa.POST", "--ei id 203")
            await { ids().containsAll(listOf(201, 202, 203)) }
            val saved = entry.repository().all().single { it.packageName == fixture }
            entry.repository().notifications(saved, true)
            entry.coordinator().refresh()
            await { ids() == setOf(202) }
            broadcast("qa.POST", "--ei id 204")
            await { ids() == setOf(202) }

            entry.repository().cancel(saved)
            entry.coordinator().refresh()
            broadcast("qa.POST", "--ei id 205")
            await { ids().contains(205) }
            val now = entry.clock().now().wall
            entry.repository().interval(fixture, "Notification fixture", now + 5000, now + 60000, true)
            entry.coordinator().refresh()
            assertTrue(ids().contains(205))
            await { ids() == setOf(202) }
        } finally {
            entry.repository().all().filter { it.packageName == fixture }.forEach { entry.repository().cancel(it) }
            entry.coordinator().refresh()
            broadcast("qa.CLEAR")
            if (Build.VERSION.SDK_INT >= 27) {
                if (!wasGranted) shell("cmd notification disallow_listener $component")
            } else if (old == null) shell("settings delete secure enabled_notification_listeners")
            else shell("settings put secure enabled_notification_listeners $old")
        }
    }

    private fun broadcast(action: String, extras: String = "") = shell("am broadcast -f 0x20 -n $receiver -a $action $extras")
    private fun ids(): Set<Int> {
        val result = broadcast("qa.QUERY")
        val payload = Regex("data=\"([^\"]*)\"").find(result)?.groupValues?.get(1)
        assertNotNull("Fixture did not report its active notifications: $result", payload)
        return payload!!.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    }
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun await(predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (!predicate() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        assertTrue("Notification condition not reached", predicate())
    }
}
