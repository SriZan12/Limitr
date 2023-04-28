package com.example.limitr.utils

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.FragmentActivity
import com.example.limitr.services.notifications.NotificationListener

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

    fun checkAccessibilityPermission(
        requireContext: Context,
        requireActivity: FragmentActivity
    ): Boolean {
        var isAccessibilityEnabled = false
        (requireContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).apply {
            installedAccessibilityServiceList.forEach { installedService ->
                installedService.resolveInfo.serviceInfo.apply {
                    if (getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                            it.resolveInfo.serviceInfo.packageName == packageName
                                    && it.resolveInfo.serviceInfo.name == name && permission ==
                                    Manifest.permission.BIND_ACCESSIBILITY_SERVICE
                                    && it.resolveInfo.serviceInfo.packageName == requireActivity.packageName
                        })
                        isAccessibilityEnabled = true
                }
            }
        }
        return isAccessibilityEnabled
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