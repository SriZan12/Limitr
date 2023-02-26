package com.example.limitr.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import com.example.limitr.services.notifications.TimerEndNotification
import com.example.limitr.services.notifications.TimerStartNotification
import java.util.*

object NotificationUtils {

    const val NOTIFICATIONCHANNEL = "LimitrAndroid"
    val NOTIFICATIONID = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "LimitrNotification"
            val descriptionText = "LimitrNotifies"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATIONCHANNEL, name, importance).apply {
                description = descriptionText
            }
            channel.enableLights(true)
            channel.enableVibration(true)

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startNotification(
        context: Context,
        notificationTitle: String,
        blockedTime: Long,
        appIcon: Bitmap
    ) {

        createNotificationChannel(context)

        val notificationId = System.currentTimeMillis().toInt()

        val startNotificationIntent = Intent(context, TimerStartNotification::class.java)
        startNotificationIntent.putExtra("title", notificationTitle)
        startNotificationIntent.putExtra("text", " Blocked For ${formatTime(blockedTime)}")
        startNotificationIntent.putExtra("notificationId", notificationId.toString())
        startNotificationIntent.putExtra("appIcon", appIcon)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                101,
                startNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val startAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        startAlarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun formatTime(timeInMillis: Long): String {
        val totalSeconds = timeInMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val formattedTime = StringBuilder()

        if (hours > 0) {
            formattedTime.append("$hours hour ")
        }

        if (minutes > 0) {
            formattedTime.append("$minutes minute ")
        }

        if (seconds > 0) {
            formattedTime.append("$seconds second")
        }

        return formattedTime.toString().trimEnd()
    }


    fun endNotification(
        context: Context,
        notificationTitle: String,
        duration: Long,
        appIcon: Bitmap
    ) {

        createNotificationChannel(context)

        val notificationId = System.currentTimeMillis().toInt() + 1

        val endNotificationIntent = Intent(context, TimerEndNotification::class.java)
        endNotificationIntent.putExtra("title", notificationTitle)
        endNotificationIntent.putExtra("notificationId", notificationId.toString())
        endNotificationIntent.putExtra("appIcon", appIcon)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                102,
                endNotificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val startAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        startAlarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            duration,
            pendingIntent
        )

    }

}