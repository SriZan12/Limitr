package com.khadka.limitr

import android.Manifest
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.inspector.WindowInspector
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.khadka.limitr.blocking.Enforcement
import com.khadka.limitr.blocking.EnforcementStatus
import com.khadka.limitr.services.blockerServices.AppLaunchDetector
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt in on an isolated emulator: -e accessibilityChecks true. Restores secure settings. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class AccessibilityCompatibilityTest {
    @Test fun externalLaunchAndReconnectKeepOneOverlayAndExpireWithoutAnotherWindowEvent() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("accessibilityChecks") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Default UiAutomation suppresses other accessibility services, invalidating this test.
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val resolver = context.contentResolver
        val services = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val enabled = Settings.Secure.getString(resolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        val preferences = context.getSharedPreferences("accessibility_consent", Context.MODE_PRIVATE)
        val hadConsent = preferences.contains("package_events_v1")
        val consent = preferences.getBoolean("package_events_v1", false)
        val entry = EntryPointAccessors.fromApplication(context, TestSessions::class.java)
        val target = instrumentation.context.packageName
        val component = ComponentName(context, AppLaunchDetector::class.java).flattenToString()

        fun configure(components: String?, value: String?) {
            automation.adoptShellPermissionIdentity(Manifest.permission.WRITE_SECURE_SETTINGS)
            try {
                Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, components)
                Settings.Secure.putString(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, value)
            } finally { automation.dropShellPermissionIdentity() }
        }
        fun overlays(): List<String> {
            var result = emptyList<String>()
            instrumentation.runOnMainSync {
                result = WindowInspector.getGlobalWindowViews().mapNotNull {
                    it.findViewById<TextView>(R.id.remainingTime)?.text?.toString()
                }
            }
            return result
        }
        fun await(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 25_000
            while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
            assertTrue(message, condition())
        }

        try {
            assertTrue(preferences.edit().putBoolean("package_events_v1", true).commit())
            configure((services.orEmpty().split(':').filter(String::isNotBlank) + component).distinct().joinToString(":"), "1")
            await("Accessibility service did not connect") {
                EnforcementStatus.state.value == Enforcement.WAITING_FOR_APP || EnforcementStatus.state.value == Enforcement.CONNECTED
            }
            // Leave enough time for a cold external Activity launch on a slow emulator.
            entry.repository().timer(target, "Launch fixture", 180_000, false)
            entry.coordinator().refresh()
            context.startActivity(Intent().setComponent(ComponentName(target, LaunchFixtureActivity::class.java.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            await("External app launch was not blocked") { overlays().isNotEmpty() }
            assertEquals("Only one accessibility window should be attached", 1, overlays().size)
            val initial = overlays().single()
            await("Overlay countdown did not advance") { overlays().singleOrNull()?.let { it != initial } == true }
            val otherServices = services.orEmpty().split(':').filter {
                it.isNotBlank() && ComponentName.unflattenFromString(it) != ComponentName.unflattenFromString(component)
            }.joinToString(":")
            configure(otherServices, if (otherServices.isEmpty()) "0" else "1")
            await("Disconnect did not remove the accessibility window") {
                EnforcementStatus.state.value == Enforcement.DISCONNECTED && overlays().isEmpty()
            }
            configure((services.orEmpty().split(':').filter(String::isNotBlank) + component).distinct().joinToString(":"), "1")
            await("Accessibility service did not reconnect") {
                EnforcementStatus.state.value == Enforcement.WAITING_FOR_APP || EnforcementStatus.state.value == Enforcement.CONNECTED
            }
            context.startActivity(Intent().setComponent(ComponentName(target, LaunchFixtureActivity::class.java.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
            await("Persisted session was not enforced after reconnect") { overlays().size == 1 }
            // Shorten the persisted session while the same external window stays foregrounded.
            entry.repository().timer(target, "Launch fixture", 1_000, false)
            entry.coordinator().refresh()
            await("Overlay/session remained after expiry") {
                overlays().isEmpty() && entry.coordinator().state.value.sessions.none { it.packageName == target }
            }
            assertTrue(entry.repository().all().none { it.packageName == target })
        } finally {
            entry.repository().all().filter { it.packageName == target }.forEach { entry.repository().cancel(it) }
            entry.coordinator().refresh()
            configure(services, enabled)
            preferences.edit().apply {
                if (hadConsent) putBoolean("package_events_v1", consent) else remove("package_events_v1")
            }.commit()
        }
    }
}
