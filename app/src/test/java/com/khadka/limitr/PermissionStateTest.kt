package com.khadka.limitr

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.blocking.*
import com.khadka.limitr.services.blockerServices.AppLaunchDetector
import com.khadka.limitr.utils.Permissions
import com.khadka.limitr.utils.SessionText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class PermissionStateTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun nullNotificationSettingsDoNotCrash() {
        Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners", null)
        assertFalse(Permissions.isNotificationServiceEnable(context))
    }
    @Test fun savedSessionsAreNotPresentedAsOperationalWithoutConsentOrConnection() {
        val state = SessionState(loaded = true)
        assertTrue(SessionText.availability(context, state).contains("disclosure"))
        EnforcementStatus.accept(context)
        assertTrue(SessionText.availability(context, state).contains("unavailable"))
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ComponentName(context, AppLaunchDetector::class.java).flattenToString())
        EnforcementStatus.set(Enforcement.DISCONNECTED)
        assertTrue(SessionText.availability(context, state).contains("disconnected"))
        EnforcementStatus.set(Enforcement.WAITING_FOR_APP)
        assertTrue(SessionText.availability(context, state).contains("waiting"))
        EnforcementStatus.set(Enforcement.CONNECTED)
        assertTrue(SessionText.availability(context, state).contains("service connected"))
        assertTrue(SessionText.availability(context, state.copy(error = true)).contains("paused"))
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
        assertTrue(SessionText.availability(context, state).contains("unavailable"))
    }
}
