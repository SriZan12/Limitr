package com.khadka.limitr.blocking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.khadka.limitr.R
import com.khadka.limitr.ui.mainactivity.MainActivity

object SessionNotifications {
    private val phases = mutableMapOf<String, Phase>()
    fun transitions(context: Context, old: List<BlockingSession>, current: List<BlockingSession>, now: TimeSnapshot) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("sessions", "Blocking sessions", NotificationManager.IMPORTANCE_DEFAULT))
        val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        fun notify(session: BlockingSession, message: String) {
            if (!allowed) return
            val tap = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                manager.notify(session.packageName, 1, NotificationCompat.Builder(context, "sessions")
                    .setSmallIcon(R.drawable.logo_color).setContentTitle(session.appName).setContentText(message)
                    .setContentIntent(tap).setAutoCancel(true).build())
            } catch (_: SecurityException) {
                // Notification access can be revoked between checking permission and posting.
            }
        }
        current.forEach { session ->
            val phase = ScheduleEvaluator.evaluate(session, now).phase
            val before = phases.put(session.revision, phase)
            if (phase == Phase.ACTIVE && before != Phase.ACTIVE) notify(session, "Blocking session has started")
        }
        old.filter { previous -> current.none { it.revision == previous.revision } }.forEach {
            phases.remove(it.revision)
            if (ScheduleEvaluator.evaluate(it, now).phase == Phase.EXPIRED) notify(it, "Blocking session has ended")
        }
        phases.keys.retainAll(current.map { it.revision }.toSet())
    }
}
