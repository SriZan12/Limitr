package com.example.limitr.services.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.limitr.mainactivity.MainActivity
import com.example.limitr.R
import com.example.limitr.utils.NotificationUtils.NOTIFICATIONCHANNEL
import java.util.*

class TimerEndNotification : BroadcastReceiver() {

    private lateinit var notificationTitle: String
    private var notificationId: Int = 0
    private lateinit var appIcon: Bitmap
    private lateinit var pendingIntent: PendingIntent

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        notificationTitle = intent?.getStringExtra("title").toString()
        notificationId = intent?.getIntExtra("notificationId", 0)!!
        appIcon = intent.getParcelableExtra<Bitmap>("appIcon")!!

        pendingIntent =
            PendingIntent.getActivity(context, 102, tapIntent, PendingIntent.FLAG_MUTABLE)
        val notificationBuilder = NotificationCompat.Builder(
            context,
            notificationTitle
        )
            .setSmallIcon(R.drawable.logo)
            .setLargeIcon(appIcon)
            .setContentTitle("$notificationTitle is Free")
            .setContentText("Enroll now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(Color.RED, 1000, 1000).build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notificationBuilder)
    }

}