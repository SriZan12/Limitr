package com.example.limitr.services.blockerServices

import android.app.Notification
import android.app.PendingIntent
import android.app.Service

import android.content.Intent
import android.os.IBinder


class ForegroundService : Service() {

//    private val notificationId = 1234 // Unique ID for the notification
//
//    override fun onCreate() {
//        super.onCreate()
//
//        createNotificationChannel(this@ForegroundService, appName)
//
//    }
//
//    interface OnAppLaunchListener {
//        fun onAppLaunch(packageName: String)
//    }
//
//    private var usageStatsManager: UsageStatsManager? = null
//    private var listener: OnAppLaunchListener? = null
//
//    fun setOnAppLaunchListener(listener: OnAppLaunchListener) {
//        this.listener = listener
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//
////        val appName = intent?.getStringExtra("appName")
////
////        Timber.d("In ForeGround Service = $appName")
//
//        Timber.d("LaunchedApp = ${startDetectingAppLaunch()}")
//
//        createNotification()
//        startForeground(notificationId, createNotification())
//
////        val accessibleIntent = Intent(this@ForegroundService, AccessibilityService::class.java)
////        accessibleIntent.putExtra("appName", appName)
////        startService(accessibleIntent)
//
//        return START_NOT_STICKY
//
//    }
//
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
//
//    private fun createNotification(): Notification {
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        return NotificationCompat.Builder(this, NOTIFICATIONCHANNEL)
//            .setContentTitle("Limitr")
//            .setContentText("App blocking is active")
//            .setSmallIcon(R.drawable.logo)
//            .setContentIntent(pendingIntent)
//            .build()
//    }
//
//    private fun startDetectingAppLaunch(): String? {
//        val usageStatsManager =
//            this.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
//        val endTime = System.currentTimeMillis()
//        val startTime = endTime - 1000 * 60 // look for events in the past minute
//        val usageStats =
//            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
//
//        // Sort the usage stats by last time used, so the most recent app launch is at the top
//        usageStats.sortByDescending { it.lastTimeUsed }
//
//        return usageStats.firstOrNull()?.packageName
//    }
}