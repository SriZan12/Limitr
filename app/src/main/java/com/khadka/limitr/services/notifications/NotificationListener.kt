package com.khadka.limitr.services.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.khadka.limitr.blocking.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListener : NotificationListenerService() {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var clock: SessionClock
    @Inject lateinit var preferences: NotificationPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collection: Job? = null
    private var essential = emptySet<String>()
    private var choices: Map<String, Boolean>? = null
    override fun onListenerConnected() {
        super.onListenerConnected()
        // Before API 24 these callbacks may arrive on binder threads. Confine mutable state to Main.
        scope.launch { collectNotifications() }
    }
    private fun collectNotifications() {
        NotificationAccess.connectionChanged(true)
        essential = EssentialApps.packages(this)
        choices = null
        collection?.cancel()
        collection = scope.launch {
            try {
                combine(coordinator.state, preferences.choices) { state, selected -> state to selected }
                    .collect { (_, selected) ->
                        choices = selected
                        activeNotifications?.forEach(::consider)
                    }
            } catch (_: SecurityException) { NotificationAccess.connectionChanged(false) }
              catch (_: IOException) { choices = null }
        }
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        scope.launch { consider(sbn) }
    }
    private fun consider(sbn: StatusBarNotification) {
        val selected = choices ?: return
        val state = coordinator.state.value
        if (!NotificationAccess.connected.value || !state.loaded || state.error ||
            sbn.packageName == packageName || sbn.packageName in essential || sbn.isOngoing) return
        val session = ScheduleEvaluator.activeFor(state.sessions, sbn.packageName, clock.now()) ?: return
        if (selected[sbn.packageName] ?: session.blockNotifications) {
            try { cancelNotification(sbn.key) }
            catch (_: SecurityException) { NotificationAccess.connectionChanged(false); collection?.cancel() }
        }
    }
    override fun onListenerDisconnected() {
        scope.launch {
            NotificationAccess.connectionChanged(false)
            choices = null
            collection?.cancel()
        }
        super.onListenerDisconnected()
    }
    override fun onDestroy() { NotificationAccess.connectionChanged(false); scope.cancel(); super.onDestroy() }
}
