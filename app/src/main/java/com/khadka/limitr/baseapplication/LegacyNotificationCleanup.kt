package com.khadka.limitr.baseapplication

import android.app.NotificationManager
import android.content.Context
import android.os.Build

internal object LegacyNotificationCleanup {
    fun removeBlockingAlerts(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Remove alerts posted by older versions without requesting notification permission.
        manager.activeNotifications.filter { posted ->
            if (Build.VERSION.SDK_INT >= 26) posted.notification.channelId == "sessions"
            else posted.id == 1 && posted.tag != null
        }.forEach { manager.cancel(it.tag, it.id) }
        if (Build.VERSION.SDK_INT >= 26) manager.deleteNotificationChannel("sessions")
    }
}
