package com.khadka.limitr.services.notifications

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import com.khadka.limitr.utils.Permissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationAccess {
    private val mutableConnected = MutableStateFlow(false)
    val connected = mutableConnected.asStateFlow()
    internal fun connectionChanged(connected: Boolean) { mutableConnected.value = connected }

    fun reconnectIfAllowed(context: Context) {
        if (Build.VERSION.SDK_INT >= 24 && !connected.value && Permissions.isNotificationServiceEnable(context)) {
            try { NotificationListenerService.requestRebind(ComponentName(context, NotificationListener::class.java)) }
            catch (_: SecurityException) { /* Access can be revoked between the check and the request. */ }
        }
    }
}
