package com.khadka.limitr.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.khadka.limitr.R
import com.khadka.limitr.blocking.BlockingSession
import timber.log.Timber

/** Owned exclusively by the accessibility service, without singleton/Activity references. */
class OverlayScreen {
    private var manager: WindowManager? = null
    private var view: View? = null
    private var revision: String? = null
    val isShowing get() = view != null

    fun show(service: AccessibilityService, session: BlockingSession, onHome: () -> Unit, onDetails: () -> Unit): Boolean {
        if (revision == session.revision && view != null) return true
        removeOverlayView()
        val candidate = LayoutInflater.from(service).inflate(R.layout.overlay, null)
        candidate.findViewById<TextView>(R.id.appBlock).text = session.appName
        candidate.findViewById<ImageView>(R.id.appIcon).setImageDrawable(ViewUtils.getAppIconByPackageName(service, session.packageName))
        candidate.findViewById<Button>(R.id.exit).setOnClickListener { onHome() }
        candidate.findViewById<Button>(R.id.goToActivityBlocked).setOnClickListener { onDetails() }
        val windowManager = service.getSystemService(WindowManager::class.java)
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        try { windowManager.addView(candidate, params) }
        catch (error: WindowManager.BadTokenException) { Timber.w("Accessibility window token unavailable"); return false }
        catch (error: SecurityException) { Timber.w("Accessibility window access revoked"); return false }
        manager = windowManager
        view = candidate
        revision = session.revision
        Insets.apply(candidate)
        return true
    }

    fun remaining(millis: Long) { view?.findViewById<TextView>(R.id.remainingTime)?.text = DateAndTime.formatTimeInNumbers(millis) }
    fun removeOverlayView() {
        val old = view
        if (old != null) {
            try { manager?.removeViewImmediate(old) }
            catch (_: IllegalArgumentException) { Timber.d("Accessibility window already detached") }
        }
        view = null
        manager = null
        revision = null
    }
}
