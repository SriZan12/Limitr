package com.example.limitr.services.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.limitr.ui.mainactivity.MainActivity
import com.example.limitr.R

class TimerStartNotification : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notificationTitle = intent?.getStringExtra("title")
        val blockedTime = intent?.getStringExtra("text")
        val notificationId = intent?.getIntExtra("notificationId", 0)
        val appIcon = intent?.getParcelableExtra<Bitmap>("appIcon")

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 101, tapIntent, PendingIntent.FLAG_MUTABLE)

        val notificationBuilder = NotificationCompat.Builder(context, notificationTitle!!)
            .setSmallIcon(R.drawable.logo_color)
            .setLargeIcon(appIcon)
            .setContentTitle(notificationTitle)
            .setContentText(blockedTime)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true).build()

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId!!, notificationBuilder)
            }

        }
    }

}