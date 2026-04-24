package com.example.limitr.services.notifications

import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.example.limitr.data.local.appdatabase.LimitrDao
import com.example.limitr.utils.ViewUtils.getAppNameByPackageName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

import javax.inject.Inject

@AndroidEntryPoint
class NotificationListener : NotificationListenerService(), LifecycleOwner {

    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var editor: SharedPreferences.Editor

    @Inject
    lateinit var limitrDao: LimitrDao
    private lateinit var lifecycleRegistry: LifecycleRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

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
                val isAppBlocked = blockedApps.any {
                    it.appPackage == packageName

                }
                if (isAppBlocked) {
                    val appName = getAppNameByPackageName(this@NotificationListener, packageName!!)
                    val notificationStatus = checkNotificationStatus(appName)
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

    private fun checkNotificationStatus(appName: String): Boolean {
        return sharedPref.getBoolean(appName, false)
    }


    override fun onDestroy() {
        super.onDestroy()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

}
