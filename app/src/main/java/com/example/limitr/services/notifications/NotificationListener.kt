package com.example.limitr.services.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.example.limitr.data.room.appdatabase.LimitrDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

import javax.inject.Inject

@AndroidEntryPoint
class NotificationListener : NotificationListenerService(), LifecycleOwner {

    @Inject
    lateinit var limitrDao: LimitrDao
    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.markState(Lifecycle.State.CREATED)

    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Timber.d("IsAppBlocked = ${sbn?.packageName}")
        sbn.let {
            lifecycleScope.launch {
                if (isAppBlocked(sbn?.packageName)) {
                    Timber.d("Notification is blocked = ${sbn?.packageName}")
                    cancelAllNotifications()
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {

    }

    private suspend fun isAppBlocked(packageName: String?): Boolean =
        suspendCoroutine { continuation ->
            limitrDao.getBlockedApps().observeForever { blockedApps ->
                val isAppBlocked = blockedApps.any { it.appPackage == packageName }
                if (isAppBlocked) {
                    val notificationStatus = blockedApps.any { it.notificationStatus == true }
                    if (notificationStatus) {
                        continuation.resume(notificationStatus)
                        limitrDao.getBlockedApps()
                            .removeObserver { } // To prevent the further memory leaks
                    }
                }
            }
            // Handling the configuration if the list is empty
            limitrDao.getBlockedApps().value?.let { blockedApps ->
                if (blockedApps.isEmpty()) {
                    continuation.resume(false)
                    limitrDao.getBlockedApps().removeObserver { }
                }
            }
        }


    override fun onDestroy() {
        super.onDestroy()

        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
    }


    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }


}