package com.khadka.limitr.utils

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.khadka.limitr.services.blockerServices.AppLaunchDetector
import com.khadka.limitr.services.notifications.NotificationListener

object Permissions {
    fun isUsageStateManagerEnabled(context: Context): Boolean = context.getSystemService(AppOpsManager::class.java)
        .checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    fun Context.isAccessibilityEnabled(): Boolean = enabled(this, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, ComponentName(this, AppLaunchDetector::class.java))
    fun isNotificationServiceEnable(context: Context): Boolean = enabled(context, "enabled_notification_listeners", ComponentName(context, NotificationListener::class.java))
    private fun enabled(context: Context, setting: String, component: ComponentName): Boolean =
        Settings.Secure.getString(context.contentResolver, setting).orEmpty().split(':').any { ComponentName.unflattenFromString(it) == component }
}
