package com.example.limitr.utils

import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Color
import android.os.Build
import com.example.limitr.services.notifications.TimerEndNotification
import com.example.limitr.services.notifications.TimerStartNotification
import com.example.limitr.utils.DateAndTime.formatTimeInWords
import kotlin.random.Random

object NotificationUtils {

    private fun createNotificationChannel(context: Context, appName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "LimitrNotification"
            val descriptionText = "LimitrNotifies"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(appName, name, importance).apply {
                description = descriptionText
                lightColor = Color.DKGRAY
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
                enableVibration(true)
            }


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
        appPackage: String,
    ) {

        createNotificationChannel(context, appName)

        val notificationId = generateUniqueCode()

        val startNotificationIntent = Intent(context, TimerStartNotification::class.java)
        startNotificationIntent.putExtra("title", appName)
        startNotificationIntent.putExtra("text", " Blocked For ${formatTimeInWords(blockedTime)}")
        startNotificationIntent.putExtra("notificationId", notificationId)
        startNotificationIntent.putExtra("appPackage", appPackage)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                startNotificationIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE

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
        appPackage: String,
    ) {

        createNotificationChannel(context, appName)

        val notificationId = generateUniqueCode()

        val endNotificationIntent = Intent(context, TimerEndNotification::class.java)
        endNotificationIntent.putExtra("title", appName)
        endNotificationIntent.putExtra("notificationId", notificationId)
        endNotificationIntent.putExtra("appPackage", appPackage)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                endNotificationIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            )

        val startAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        startAlarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            duration,
            pendingIntent
        )

    }

    fun cancelNotification(context: Context, channelId: String) {
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(channelId)
        }

    }

    fun generateUniqueCode(): Int {
        return Random.nextInt(100)
    }
}