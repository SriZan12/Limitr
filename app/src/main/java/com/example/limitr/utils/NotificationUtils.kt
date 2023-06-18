package com.example.limitr.utils

import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import com.example.limitr.services.notifications.TimerEndNotification
import com.example.limitr.services.notifications.TimerStartNotification
import com.example.limitr.utils.DateAndTime.formatTimeInWords

object NotificationUtils {

    private fun createNotificationChannel(context: Context, appName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "LimitrNotification"
            val descriptionText = "LimitrNotifies"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(appName, name, importance).apply {
                description = descriptionText
            }
            channel.enableLights(true)
            channel.enableVibration(true)

            val notificationManager: NotificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startNotification(
        context: Context,
        appName: String,
        notificationStartTime: Long,
        blockedTime: Long,
        appIcon: Bitmap,
    ) {

        createNotificationChannel(context, appName)

        val notificationId = System.currentTimeMillis()

        val startNotificationIntent = Intent(context, TimerStartNotification::class.java)
        startNotificationIntent.putExtra("title", appName)
        startNotificationIntent.putExtra("text", " Blocked For ${formatTimeInWords(blockedTime)}")
        startNotificationIntent.putExtra("notificationId", notificationId.toInt())
        startNotificationIntent.putExtra("appIcon", appIcon)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                101,
                startNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        val startAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        startAlarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            notificationStartTime,
            pendingIntent
        )
    }


    fun endNotification(
        context: Context,
        appName: String,
        duration: Long,
        appIcon: Bitmap,
    ) {

        createNotificationChannel(context, appName)

        val notificationId = System.currentTimeMillis() + 1

        val endNotificationIntent = Intent(context, TimerEndNotification::class.java)
        endNotificationIntent.putExtra("title", appName)
        endNotificationIntent.putExtra("notificationId", notificationId)
        endNotificationIntent.putExtra("appIcon", appIcon)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                102,
                endNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        val startAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        startAlarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            duration,
            pendingIntent
        )

    }

    fun cancelNotification(context: Context, channelId: String) {
        // Get an instance of the NotificationManager
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

// Remove the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(channelId)
        }

    }
}