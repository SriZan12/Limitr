package com.khadka.limitr.services.notifications

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.khadka.limitr.blocking.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListener : NotificationListenerService() {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var clock: SessionClock
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collection: Job? = null
    private var essential = emptySet<String>()
    override fun onListenerConnected() {
        essential = EssentialApps.packages(this)
        collection?.cancel()
        collection = scope.launch {
            try { coordinator.state.collect { activeNotifications?.forEach(::consider) } }
            catch (_: SecurityException) { collection?.cancel() }
        }
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { sbn?.let(::consider) }
    private fun consider(sbn: StatusBarNotification) {
        if (sbn.packageName in essential || sbn.isOngoing) return
        val session = ScheduleEvaluator.activeFor(coordinator.state.value.sessions, sbn.packageName, clock.now()) ?: return
        if (session.blockNotifications) {
            try { cancelNotification(sbn.key) }
            catch (_: SecurityException) { collection?.cancel() }
        }
    }
    override fun onListenerDisconnected() { collection?.cancel() }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
