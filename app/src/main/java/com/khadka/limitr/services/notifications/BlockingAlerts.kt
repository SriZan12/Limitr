package com.khadka.limitr.services.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.annotation.SuppressLint
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.khadka.limitr.R
import com.khadka.limitr.blocking.BlockingSession
import com.khadka.limitr.blocking.Phase
import com.khadka.limitr.blocking.ScheduleEvaluator
import com.khadka.limitr.blocking.TimeSnapshot
import com.khadka.limitr.blocking.EnforcementStatus
import com.khadka.limitr.blocking.Enforcement
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingAlerts @Inject constructor(@param:ApplicationContext private val context: Context) {
    companion object {
        const val CHANNEL = "blocking_alerts"
        const val ID = 1001
        fun tag(packageName: String) = "blocking:$packageName"
    }
    private val manager = context.getSystemService(NotificationManager::class.java)
    // Delivery bookkeeping is intentionally excluded from backup; never contains notification content.
    private val delivery = context.getSharedPreferences("blocking_alert_delivery", Context.MODE_PRIVATE)
    var permissionPrompted: Boolean
        get() = delivery.getBoolean("permission_prompted", false)
        set(value) { delivery.edit { putBoolean("permission_prompted", value) } }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.blocking_alert_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun enabled(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    /** Called by the serialized coordinator, after Room commits and schedules the next boundary. */
    @SuppressLint("UseKtx") // commit()'s Boolean result is needed; the KTX edit extension discards it.
    suspend fun reconcile(sessions: List<BlockingSession>, expired: List<BlockingSession>, now: TimeSnapshot) = withContext(Dispatchers.IO) {
        createChannel()
        val active = sessions.filter { ScheduleEvaluator.evaluate(it, now).phase == Phase.ACTIVE }
        val activePackages = active.map { it.packageName }.toSet()
        // A cancelled/replaced block must not leave a stale "blocked" alert or emit a timer-ended alert.
        manager.activeNotifications.filter { it.id == ID && it.tag?.startsWith("blocking:") == true &&
            it.notification.extras.getBoolean("limitr.blocking.active") && it.tag.removePrefix("blocking:") !in activePackages
        }.forEach { manager.cancel(it.tag, it.id) }
        if (!enabled()) return@withContext
        active.forEach { session ->
            if (delivery.getString(session.packageName, null) != session.revision && post(session, ended = false)) {
                if (!delivery.edit().putString(session.packageName, session.revision).commit()) Timber.w("Could not save blocking alert delivery state")
            }
        }
        // Only rows that actually expired in this reconciliation qualify, never disappeared/cancelled rows.
        expired.filter { it.packageName !in activePackages }.forEach { post(it, ended = true) }
    }

    private fun post(session: BlockingSession, ended: Boolean): Boolean {
        val intent = Intent(context, BlockAppActivity::class.java)
            .setData("limitr://blocking/${Uri.encode(session.packageName)}".toUri())
            .putExtra("packageName", session.packageName)
        val content = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_blocking_notification)
            .setContentTitle(context.getString(if (ended) R.string.blocking_ended_title else R.string.blocking_started_title, session.appName))
            .setContentText(context.getString(when {
                ended -> R.string.blocking_ended_message
                !EnforcementStatus.consent(context) || EnforcementStatus.state.value != Enforcement.CONNECTED -> R.string.blocking_started_unavailable
                else -> R.string.blocking_started_message
            }))
            .setContentIntent(content).setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addExtras(android.os.Bundle().apply { putBoolean("limitr.blocking.active", !ended) }).build()
        return try { manager.notify(tag(session.packageName), ID, notification); true }
        catch (_: SecurityException) { false }
    }
}
