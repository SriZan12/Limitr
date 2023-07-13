package com.example.limitr.services.blockerServices

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
import com.example.limitr.R
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.mainactivity.MainActivity

class ShowBlockNotification : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val appName = intent?.getStringExtra("title").toString()
        val packageName = intent?.getStringExtra("packageName")
        val notificationId = intent?.getIntExtra("notificationId", 0)!!
        val appIcon = intent.getParcelableExtra<Bitmap>("appIcon")!!

        val tapIntent = Intent(context, BlockAppActivity::class.java)
        tapIntent.putExtra(context?.getString(R.string.appName), appName)
        tapIntent.putExtra(context?.getString(R.string.packageName), packageName)
        tapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent =
            PendingIntent.getActivity(context, 104, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        val notificationBuilder = NotificationCompat.Builder(
            context!!,
            appName
        )
            .setSmallIcon(R.drawable.logo_color)
            .setLargeIcon(appIcon)
            .setContentTitle("$appName is Blocked!")
            .setContentText("Tap here for more details..")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
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
