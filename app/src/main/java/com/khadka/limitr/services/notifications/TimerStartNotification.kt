package com.khadka.limitr.services.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.khadka.limitr.R
import com.khadka.limitr.ui.mainactivity.MainActivity
import com.khadka.limitr.utils.NotificationUtils.generateUniqueCode
import com.khadka.limitr.utils.ViewUtils

class TimerStartNotification : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val notificationTitle = intent?.getStringExtra("title")
        val blockedTime = intent?.getStringExtra("text")
        val notificationId = intent?.getIntExtra("notificationId", 0)
        val appPackage = intent?.getStringExtra("appPackage").toString()

        val appIcon = ViewUtils.getAppIconByPackageName(context = context, appPackage)?.toBitmap()!!
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId!!,
                tapIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT
            )

        val notificationBuilder = NotificationCompat.Builder(context, notificationTitle!!)
            .setSmallIcon(R.drawable.logo_color)
            .setLargeIcon(appIcon)
            .setContentTitle(notificationTitle)
            .setContentText(blockedTime)
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
                notify(generateUniqueCode(), notificationBuilder)
            }

        }
    }

}