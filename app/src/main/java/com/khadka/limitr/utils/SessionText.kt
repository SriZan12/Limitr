package com.khadka.limitr.utils

import android.content.Context
import com.khadka.limitr.blocking.*
import com.khadka.limitr.utils.Permissions.isAccessibilityEnabled
import java.text.DateFormat
import java.util.Date

object SessionText {
    fun intervalRange(session: BlockingSession): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        return "${format.format(Date(session.startWall))}\n– ${format.format(Date(session.endWall))}"
    }
    fun timing(session: BlockingSession, now: TimeSnapshot): String {
        val state = ScheduleEvaluator.evaluate(session, now)
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        return when (state.phase) {
            Phase.SCHEDULED -> "Scheduled: ${format.format(Date(session.startWall))} – ${format.format(Date(session.endWall))}"
            Phase.ACTIVE -> "Active session • ${DateAndTime.formatTimeInNumbers(state.remaining)} remaining"
            Phase.EXPIRED -> "Session ended"
        }
    }
    fun availability(context: Context, state: SessionState): String = when {
        state.error -> "Storage unavailable. Your data has been kept. Blocking is paused."
        !state.loaded -> "Loading saved sessions…"
        !EnforcementStatus.consent(context) -> "Review accessibility disclosure to enable blocking."
        !context.isAccessibilityEnabled() -> "Blocking unavailable: enable LIMITR in Accessibility settings. Saved schedules are kept."
        EnforcementStatus.state.value == Enforcement.WINDOW_UNAVAILABLE -> "Blocking window unavailable. Reconnect LIMITR in Accessibility settings."
        EnforcementStatus.state.value == Enforcement.DISCONNECTED -> "Accessibility enabled, service disconnected. Reopen settings to reconnect."
        EnforcementStatus.state.value == Enforcement.NEEDS_CONSENT -> "Review accessibility disclosure to enable blocking."
        EnforcementStatus.state.value == Enforcement.WAITING_FOR_APP -> "Service connected; waiting for the next app window event."
        else -> "Blocking service connected. Android may interrupt enforcement."
    }
}
