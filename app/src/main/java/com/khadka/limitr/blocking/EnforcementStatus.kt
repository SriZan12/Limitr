package com.khadka.limitr.blocking

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Enforcement { DISCONNECTED, WAITING_FOR_APP, CONNECTED, WINDOW_UNAVAILABLE, NEEDS_CONSENT }
object EnforcementStatus {
    private val mutable = MutableStateFlow(Enforcement.DISCONNECTED)
    val state = mutable.asStateFlow()
    fun set(value: Enforcement) { mutable.value = value }
    fun consent(context: Context) = context.getSharedPreferences("accessibility_consent", Context.MODE_PRIVATE).getBoolean("package_events_v1", false)
    fun accept(context: Context) { context.getSharedPreferences("accessibility_consent", Context.MODE_PRIVATE).edit().putBoolean("package_events_v1", true).apply() }
}
