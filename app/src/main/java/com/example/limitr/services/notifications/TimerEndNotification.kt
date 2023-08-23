package com.example.limitr.services.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.limitr.ui.mainactivity.MainActivity
import com.example.limitr.R
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName

class TimerEndNotification : BroadcastReceiver() {

    private lateinit var notificationTitle: String
    private var notificationId: Int = 0
    private lateinit var appIcon: Bitmap
    private lateinit var pendingIntent: PendingIntent
    private lateinit var appPackage: String

    override fun onReceive(context: Context?, intent: Intent?) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        notificationTitle = intent?.getStringExtra("title").toString()
        notificationId = intent?.getIntExtra("notificationId", 0)!!
        appPackage = intent.getStringExtra("appPackage").toString()

        appIcon = getAppIconByPackageName(context = context!!, appPackage)?.toBitmap()!!

        pendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                tapIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        val notificationBuilder = NotificationCompat.Builder(
            context!!,
            notificationTitle
        )
            .setSmallIcon(R.drawable.logo_color)
            .setLargeIcon(appIcon)
            .setContentTitle("$notificationTitle is Free")
            .setContentText("Enroll now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true).build()

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, notificationBuilder)
            }

        }


    }

}