package com.khadka.limitr.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

object EssentialApps {
    fun packages(context: Context): Set<String> = buildSet {
        add(context.packageName)
        addAll(listOf("android", "com.android.systemui", "com.android.settings", "com.android.phone",
            "com.android.server.telecom", "com.android.emergency", "com.google.android.apps.safetyhub",
            "com.android.permissioncontroller", "com.google.android.permissioncontroller",
            "com.android.packageinstaller", "com.google.android.packageinstaller"))
        val pm = context.packageManager
        listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), Intent(Settings.ACTION_SETTINGS),
            Intent(Intent.ACTION_DIAL), Intent("android.intent.action.EMERGENCY_DIAL")).forEach { intent ->
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).forEach { add(it.activityInfo.packageName) }
        }
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { add(it) }
        context.getSystemService(InputMethodManager::class.java)?.inputMethodList?.forEach { add(it.packageName) }
    }
}
