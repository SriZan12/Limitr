package com.khadka.limitr.blocking

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionClock @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("device_clock", Context.MODE_PRIVATE)
    private val installationId: String = prefs.getString("installation", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("installation", it).apply()
    }

    fun now(): TimeSnapshot {
        val boot = if (Build.VERSION.SDK_INT >= 24) {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1).toString()
        } else prefs.getString("boot", "initial")!!
        return TimeSnapshot(System.currentTimeMillis(), SystemClock.elapsedRealtime(), "$installationId:$boot")
    }

    fun reboot() { prefs.edit().putString("boot", UUID.randomUUID().toString()).apply() }
}
