package com.example.limitr.services.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.limitr.MainActivity
import com.example.limitr.R
import com.example.limitr.utils.NotificationUtils.NOTIFICATIONCHANNEL

class TimerStartNotification : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notificationTitle = intent?.getStringExtra("title")
        val blockedTime = intent?.getStringExtra("text")
        val notificationId = intent?.getStringExtra("notificationId")?.toInt()
        val appIcon = intent?.getParcelableExtra<Bitmap>("appIcon")

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 101, tapIntent, PendingIntent.FLAG_MUTABLE)

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATIONCHANNEL)
            .setSmallIcon(R.drawable.logo)
            .setLargeIcon(appIcon)
            .setContentTitle(notificationTitle)
            .setContentText(blockedTime)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(Color.RED, 1000, 1000).build()

        val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
        if (notificationId != null) {
            notificationManager.notify(notificationId, notificationBuilder)
        }
    }

}