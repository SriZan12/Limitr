package com.khadka.limitr.utils

import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.khadka.limitr.blocking.EnforcementStatus

object PermissionOnboarding {
    fun show(activity: FragmentActivity) {
        val dialog = AlertDialog.Builder(activity).setTitle("Accessibility access for app blocking")
            .setMessage("LIMITR uses Android AccessibilityService to read the package name of the app window you open and show a blocking screen during your saved sessions. Processing stays on this device. LIMITR does not read screen text, collect screen contents, or send app-window events to a server.\n\nThis access is optional. You can disable it in Android Accessibility settings at any time. Timers remain saved while access is off, but blocking is unavailable. LIMITR cannot guarantee continuous blocking or suspend other apps.")
            .setNegativeButton("Not now", null)
            .setPositiveButton("Agree and open settings") { _, _ ->
                EnforcementStatus.accept(activity)
                activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }.create()
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { dialog.dismiss() }
        }
        activity.lifecycle.addObserver(observer)
        dialog.setOnDismissListener { activity.lifecycle.removeObserver(observer) }
        dialog.show()
    }
}
