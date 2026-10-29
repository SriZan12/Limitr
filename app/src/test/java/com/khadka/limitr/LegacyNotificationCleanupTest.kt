package com.khadka.limitr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.baseapplication.LegacyNotificationCleanup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [23, 28])
class LegacyNotificationCleanupTest {
    @Test fun upgradeRemovesOldAlertsAndChannelAndCanRunAgain() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("sessions", "Blocking sessions", NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel("other", "Other", NotificationManager.IMPORTANCE_DEFAULT))
        }
        fun post(tag: String, id: Int, channel: String) {
            manager.notify(tag, id, NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.logo_color).setContentTitle(tag).build())
        }
        post("old.timer", 1, "sessions")
        post("old.interval", 1, "sessions")
        post("unrelated", 2, "other")
        assertEquals(3, manager.activeNotifications.size)

        repeat(2) { LegacyNotificationCleanup.removeBlockingAlerts(context) }

        assertEquals(listOf("unrelated"), manager.activeNotifications.map { it.tag })
        if (Build.VERSION.SDK_INT >= 26) {
            assertNull(manager.getNotificationChannel("sessions"))
            assertNotNull(manager.getNotificationChannel("other"))
        }
    }
}
