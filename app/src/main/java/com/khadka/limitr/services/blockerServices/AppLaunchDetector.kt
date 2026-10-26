package com.khadka.limitr.services.blockerServices

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.khadka.limitr.blocking.*
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import com.khadka.limitr.utils.OverlayScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AppLaunchDetector : AccessibilityService() {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var clock: SessionClock
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val overlay = OverlayScreen()
    private var renderJob: Job? = null
    private var stateJob: Job? = null
    private var foreground: String? = null
    private var foregroundWindow: Int? = null
    private var foregroundClass: String? = null
    // The blocked app is no longer foreground after Home; retain its session for the notice.
    private var blockedTarget: String? = null
    private var homePackage: String? = null
    private var essential = emptySet<String>()
    private var connected = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                foreground = null
                blockedTarget = null
                renderJob?.cancel()
                overlay.removeOverlayView()
            } else scope.launch { coordinator.refresh(); reconcile() }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (connected) return
        connected = true
        essential = EssentialApps.packages(this)
        ContextCompat.registerReceiver(this, screenReceiver,
            IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED) }, ContextCompat.RECEIVER_NOT_EXPORTED)
        EnforcementStatus.set(if (EnforcementStatus.consent(this)) Enforcement.WAITING_FOR_APP else Enforcement.NEEDS_CONSENT)
        stateJob = scope.launch {
            // A system rebind can follow a suspended process or a delayed inexact alarm.
            coordinator.refresh()
            coordinator.state.collect { reconcile() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!connected || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!EnforcementStatus.consent(this)) {
            EnforcementStatus.set(Enforcement.NEEDS_CONSENT)
            blockedTarget = null
            renderJob?.cancel()
            overlay.removeOverlayView()
            return
        }
        val target = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        // Our own overlay emits a window event. Do not mistake it for opening LIMITR.
        if (target == packageName && overlay.isShowing) return
        // A launch can open another Activity after Home was requested. Only suppress the same
        // window/class, not every event from that package, or that Activity can stay foreground.
        val windowClass = event.className?.toString()
        if (target == foreground && event.windowId == foregroundWindow && windowClass == foregroundClass &&
            EnforcementStatus.state.value != Enforcement.WINDOW_UNAVAILABLE) return
        foreground = target
        foregroundWindow = event.windowId
        foregroundClass = windowClass
        // Keep the notice over Home, but never over a newly opened app or system screen.
        if (target != homePackage) blockedTarget = null
        EnforcementStatus.set(Enforcement.CONNECTED)
        reconcile()
    }

    private fun reconcile() {
        renderJob?.cancel()
        renderJob = null
        val target = blockedTarget ?: foreground
        val state = coordinator.state.value
        val visible = getSystemService(PowerManager::class.java).isInteractive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val session = if (visible && !state.error && EnforcementStatus.consent(this) && target !in essential)
            ScheduleEvaluator.activeFor(state.sessions, target, clock.now()) else null
        if (session == null) { blockedTarget = null; overlay.removeOverlayView(); return }
        if (blockedTarget == null) {
            blockedTarget = session.packageName
            // Resolve the active launcher. Querying every Home handler also includes Settings'
            // boot fallback, which must never keep the notice over a real Settings screen.
            homePackage = packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
            )?.activityInfo?.packageName
            // Send the selected app to the background before attaching the blocking notice.
            // Do this once per launch, not on every countdown tick or Room publication.
            if (!performGlobalAction(GLOBAL_ACTION_HOME)) Timber.w("Home action unavailable while blocking")
        }
        val shown = overlay.show(this, session, onHome = {
            foreground = null
            blockedTarget = null
            overlay.removeOverlayView()
            renderJob?.cancel()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }, onDetails = {
            foreground = packageName
            blockedTarget = null
            overlay.removeOverlayView()
            renderJob?.cancel()
            // Direct response to a user tap, never an automatic background activity launch.
            startActivity(Intent(this, BlockAppActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("packageName", session.packageName))
        })
        if (!shown) { EnforcementStatus.set(Enforcement.WINDOW_UNAVAILABLE); return }
        EnforcementStatus.set(Enforcement.CONNECTED)
        renderJob = scope.launch {
            while (isActive) {
                val current = ScheduleEvaluator.evaluate(session, clock.now())
                if (current.phase != Phase.ACTIVE) {
                    blockedTarget = null
                    overlay.removeOverlayView()
                    coordinator.refresh()
                    break
                }
                overlay.remaining(current.remaining)
                delay(minOf(1000L, current.remaining))
            }
        }
    }

    override fun onInterrupt() {
        foreground = null
        blockedTarget = null
        renderJob?.cancel()
        overlay.removeOverlayView()
        EnforcementStatus.set(Enforcement.WAITING_FOR_APP)
    }

    override fun onUnbind(intent: Intent?): Boolean { disconnect(); return super.onUnbind(intent) }
    override fun onDestroy() { disconnect(); scope.cancel(); super.onDestroy() }
    private fun disconnect() {
        if (connected) { unregisterReceiver(screenReceiver); connected = false }
        stateJob?.cancel()
        renderJob?.cancel()
        overlay.removeOverlayView()
        foreground = null
        blockedTarget = null
        EnforcementStatus.set(Enforcement.DISCONNECTED)
    }
}
