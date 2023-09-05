package com.khadka.limitr.utils

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.khadka.limitr.services.blockerServices.AppLaunchDetector
import com.khadka.limitr.services.notifications.NotificationListener
import timber.log.Timber

object Permissions {

    fun isUsageStateManagerEnabled(requireContext: Context): Boolean {
        val appOpsManager =
            requireContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }


     fun Context.isAccessibilityEnabled(): Boolean {
        var enabled = 0
        try {
            enabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            Timber.e(e)
        }
        if (enabled == 1) {
            val name = ComponentName(applicationContext, AppLaunchDetector::class.java)
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains(name.flattenToString()) ?: false
        }
        return false
    }

    fun isNotificationServiceEnable(context: Context): Boolean {
        val myNotificationListenerComponentName =
            ComponentName(context, NotificationListener::class.java)
        val enabledListeners =
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")

        if (enabledListeners.isEmpty()) return false

        return enabledListeners.split(":").map {
            ComponentName.unflattenFromString(it)
        }.any { componentName ->
            myNotificationListenerComponentName == componentName
        }
    }
}