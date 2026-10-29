package com.khadka.limitr

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.khadka.limitr.services.notifications.NotificationPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khadka.limitr.services.notifications.BlockingAlerts
import com.khadka.limitr.services.notifications.NotificationAccess
import com.khadka.limitr.services.notifications.NotificationListener
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Isolated-device checks. POST_NOTIFICATIONS is granted for this run; reset it after instrumentation. */
@RunWith(AndroidJUnit4::class)
class NotificationControlsInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val entry = EntryPointAccessors.fromApplication(context, TestSessions::class.java)
    private val fixture = instrumentation.context.packageName
    private val manager = context.getSystemService(NotificationManager::class.java)
    private fun enabled() = assumeTrue(InstrumentationRegistry.getArguments().getString("notificationChecks") == "true")
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private suspend fun await(condition: () -> Boolean) = withTimeout(15000) { while (!condition()) delay(100) }
    private fun broadcast(action: String, extras: String = "") = shell("am broadcast -f 0x20 -n $fixture/${NotificationFixtureReceiver::class.java.name} -a $action $extras")
    private fun fixtureIds(): Set<Int> {
        val result = broadcast("qa.QUERY")
        val payload = Regex("data=\"([^\"]*)\"").find(result)?.groupValues?.get(1)
        assertNotNull(result, payload)
        return payload!!.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    }

    private suspend fun awaitChoice(expected: Boolean, step: String, scenario: ActivityScenario<BlockAppActivity>) {
        try {
            withTimeout(15000) {
                while (entry.notifications().choices.first()[fixture] != expected) delay(100)
                // Wait for persistence AND rendering to complete before simulating another user tap.
                var ready = false
                while (!ready) {
                    scenario.onActivity {
                        val toggle = it.findViewById<SwitchCompat>(R.id.blockNotification)
                        ready = toggle.isChecked == expected && toggle.isEnabled
                    }
                    if (!ready) delay(100)
                }
            }
        }
        catch (timeout: TimeoutCancellationException) {
            var ui = ""
            scenario.onActivity {
                val toggle = it.findViewById<SwitchCompat>(R.id.blockNotification)
                ui = "checked=${toggle.isChecked}, enabled=${toggle.isEnabled}, status=${it.findViewById<TextView>(R.id.notificationHidingStatus).text}, error=${it.findViewById<TextView>(R.id.sessionError).text}"
            }
            throw AssertionError("$step: expected $expected, saved=${entry.notifications().choices.first()[fixture]}, $ui", timeout)
        }
    }

    @Test fun notificationChoiceSurvivesClosingAndReopeningTheAndroidDataStore(): Unit = runBlocking {
        enabled()
        val file = File(context.cacheDir, "notification-test.preferences_pb")
        file.delete()
        val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val preferences = NotificationPreferences(PreferenceDataStoreFactory.create(scope = writer) { file })
            preferences.set("test.first", true)
            preferences.set("test.second", false)
        } finally { writer.coroutineContext[Job]!!.cancelAndJoin() }
        val reader = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val preferences = NotificationPreferences(PreferenceDataStoreFactory.create(scope = reader) { file })
            assertEquals(mapOf("test.first" to true, "test.second" to false), preferences.choices.first())
        } finally { reader.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }

    @Test fun blockingStartAndAutomaticEndArePostedWithoutKeepingDetailsOpen(): Unit = runBlocking {
        enabled()
        val target = "com.limitr.alert.fixture"
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        try {
            entry.repository().timer(target, "Alert fixture", 4000, false)
            entry.coordinator().refresh()
            await { manager.activeNotifications.any { it.tag == BlockingAlerts.tag(target) && it.notification.extras.getBoolean("limitr.blocking.active") } }
            // Do not call refresh here: the actual running coordinator must deliver expiry itself.
            await { manager.activeNotifications.any { it.tag == BlockingAlerts.tag(target) && !it.notification.extras.getBoolean("limitr.blocking.active") } }
            assertTrue(entry.repository().all().none { it.packageName == target })
            val end = manager.activeNotifications.single { it.tag == BlockingAlerts.tag(target) }
            assertEquals("Alert fixture blocking ended", end.notification.extras.getString("android.title"))
            manager.cancel(end.tag, end.id)
            entry.coordinator().refresh()
            assertTrue(manager.activeNotifications.none { it.tag == BlockingAlerts.tag(target) })
        } finally {
            entry.repository().all().filter { it.packageName == target }.forEach { entry.repository().cancel(it) }
            entry.coordinator().refresh()
            manager.cancel(BlockingAlerts.tag(target), BlockingAlerts.ID)
        }
    }

    @Test fun switchWorksBeforeBlockingPersistsAndControlsTheRealListener(): Unit = runBlocking {
        enabled()
        val component = ComponentName(context, NotificationListener::class.java).flattenToString()
        val oldListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val wasGranted = if (Build.VERSION.SDK_INT >= 27) manager.isNotificationListenerAccessGranted(ComponentName(context, NotificationListener::class.java))
            else component in oldListeners.orEmpty().split(':')
        val previousChoice = entry.notifications().choices.first()[fixture]
        fun access(grant: Boolean) {
            if (Build.VERSION.SDK_INT >= 27) shell("cmd notification ${if (grant) "allow_listener" else "disallow_listener"} $component")
            else {
                val listeners = oldListeners.orEmpty().split(':').filter { it.isNotBlank() && it != component }.toMutableList()
                if (grant) listeners.add(component)
                if (listeners.isEmpty()) shell("settings delete secure enabled_notification_listeners")
                else shell("settings put secure enabled_notification_listeners ${listeners.joinToString(":")}")
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                shell("pm grant $fixture android.permission.POST_NOTIFICATIONS")
                shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
            }
            entry.notifications().set(fixture, false)
            access(true)
            await { NotificationAccess.connected.value }
            entry.coordinator().refresh()
            val intent = Intent(context, BlockAppActivity::class.java).putExtra("packageName", fixture)
            ActivityScenario.launch<BlockAppActivity>(intent).use { scenario ->
                var ready = false
                await { scenario.onActivity { ready = it.findViewById<SwitchCompat>(R.id.blockNotification).isEnabled }; ready }
                scenario.onActivity {
                    val toggle = it.findViewById<SwitchCompat>(R.id.blockNotification)
                    assertFalse(toggle.isChecked)
                    toggle.performClick()
                }
                awaitChoice(true, "initial enable", scenario)
                scenario.recreate()
                await { scenario.onActivity { ready = it.findViewById<SwitchCompat>(R.id.blockNotification).isChecked }; ready }
                // With no block the preference alone must not suppress anything.
                broadcast("qa.CLEAR")
                broadcast("qa.POST", "--ei id 201")
                await { 201 in fixtureIds() }
                entry.repository().timer(fixture, "Notification fixture", 60000, false)
                entry.coordinator().refresh()
                await { 201 !in fixtureIds() }
                scenario.onActivity { it.findViewById<SwitchCompat>(R.id.blockNotification).performClick() }
                awaitChoice(false, "disable during blocking", scenario)
                broadcast("qa.POST", "--ei id 202")
                await { 202 in fixtureIds() }
                scenario.onActivity { it.findViewById<SwitchCompat>(R.id.blockNotification).performClick() }
                awaitChoice(true, "enable during blocking", scenario)
                await { 202 !in fixtureIds() }
                entry.repository().timer(fixture, "Notification fixture", 1000, false)
                entry.coordinator().refresh()
                await { entry.coordinator().state.value.sessions.none { it.packageName == fixture } }
                assertTrue(entry.notifications().choices.first().getValue(fixture))
                broadcast("qa.POST", "--ei id 203")
                await { 203 in fixtureIds() }
                access(false)
                await { !NotificationAccess.connected.value }
                await {
                    scenario.onActivity { ready = it.findViewById<TextView>(R.id.notificationHidingStatus).text.contains("access is missing") }
                    ready
                }
                // Turning OFF must remain possible even after access has been revoked.
                scenario.onActivity { it.findViewById<SwitchCompat>(R.id.blockNotification).performClick() }
                awaitChoice(false, "disable after revocation", scenario)
            }
        } finally {
            entry.repository().all().filter { it.packageName == fixture }.forEach { entry.repository().cancel(it) }
            entry.coordinator().refresh()
            entry.notifications().set(fixture, previousChoice)
            broadcast("qa.CLEAR")
            manager.cancel(BlockingAlerts.tag(fixture), BlockingAlerts.ID)
            access(wasGranted)
        }
    }
}
