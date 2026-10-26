package com.khadka.limitr.ui.blocker.activity

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.NumberPicker
import android.widget.TextView
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.khadka.limitr.R
import com.khadka.limitr.blocking.*
import com.khadka.limitr.databinding.ActivityBlockAppBinding
import com.khadka.limitr.ui.blocker.vm.SessionViewModel
import com.khadka.limitr.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import com.khadka.limitr.utils.Permissions.isAccessibilityEnabled
import com.khadka.limitr.ui.history.ActivityHistory

@AndroidEntryPoint
class BlockAppActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlockAppBinding
    private val model: SessionViewModel by viewModels()
    private lateinit var target: String
    private lateinit var label: String
    private var essential = false
    private var dialog: android.app.Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra("packageName")?.takeIf { it.isNotBlank() } ?: run { finish(); return }
        binding = ActivityBlockAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Insets.apply(binding.root)
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        label = ViewUtils.getAppNameByPackageName(this, target)
        essential = target in EssentialApps.packages(this)
        binding.appName.text = label
        binding.appIcon.setImageDrawable(ViewUtils.getAppIconByPackageName(this, target))
        binding.setTime.setOnClickListener { timerDialog() }
        binding.setInterval.setOnClickListener { intervalDialog() }
        binding.unBlockApp.setOnClickListener { unblockDialog() }
        binding.showHistory.setOnClickListener { startActivity(Intent(this, ActivityHistory::class.java)) }
        binding.blockNotification.setOnClickListener {
            val session = current() ?: return@setOnClickListener
            if (!Permissions.isNotificationServiceEnable(this)) {
                binding.blockNotification.isChecked = false
                dialog = AlertDialog.Builder(this).setTitle("Hide selected app notifications?")
                    .setMessage("LIMITR uses notification access to dismiss notifications only from apps you have blocked and selected for notification hiding. It does not save or transmit notification contents. Ongoing and essential system notifications are left available.")
                    .setNegativeButton("Not now", null).setPositiveButton("Agree and open settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }.show()
            } else model.act { notifications(session, binding.blockNotification.isChecked) }
        }
        binding.enforcementStatus.setOnClickListener { PermissionOnboarding.show(this) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.coordinator.state.collect { render() } }
                launch { model.busy.collect { render() } }
                launch { model.error.collect { render() } }
                launch { model.unblocked.collect { render() } }
                launch { EnforcementStatus.state.collect { render() } }
                launch { while (isActive) { render(); delay(1000) } }
            }
        }
    }
    override fun onResume() { super.onResume(); lifecycleScope.launch { model.coordinator.refresh() } }
    private fun current() = model.coordinator.state.value.sessions.firstOrNull { it.packageName == target }
    private fun render() {
        if (!::binding.isInitialized) return
        // Wait for persistence/reconciliation to finish before destroying the ViewModel.
        if (model.unblocked.value && !model.busy.value && model.error.value == null) {
            dialog?.dismiss()
            finish()
            return
        }
        val state = model.coordinator.state.value
        val session = current()
        val canEdit = state.loaded && !state.error && !model.busy.value && !essential
        binding.setTime.isEnabled = canEdit
        binding.setInterval.isEnabled = canEdit
        binding.unBlockApp.isEnabled = session != null && !model.busy.value
        binding.blockNotification.isEnabled = session != null && !model.busy.value
        binding.blockNotification.isChecked = session?.blockNotifications == true
        val now = model.clock.now()
        val evaluation = session?.let { ScheduleEvaluator.evaluate(it, now) }
        binding.timerText.isVisible = session != null
        binding.intervalText.isVisible = session?.kind == "interval"
        binding.setTimerText.setText(when {
            session == null -> R.string.set_timer
            evaluation?.phase == Phase.SCHEDULED -> R.string.starts_in
            else -> R.string.duration
        })
        binding.setIntervalText.setText(if (session?.kind == "interval") R.string.blocked_for else R.string.set_interval)
        binding.timerText.text = session?.let {
            DateAndTime.formatTimeInNumbers(if (evaluation?.phase == Phase.SCHEDULED) it.startWall - now.wall else evaluation?.remaining ?: 0)
        }
        binding.intervalText.text = session?.takeIf { it.kind == "interval" }?.let { SessionText.intervalRange(it) }
        binding.sessionError.text = model.error.value ?: if (essential) getString(R.string.essential_apps_available) else null
        binding.sessionError.isVisible = !binding.sessionError.text.isNullOrEmpty()
        binding.enforcementStatus.isVisible = state.error || !state.loaded ||
            !EnforcementStatus.consent(this) || !isAccessibilityEnabled() || EnforcementStatus.state.value != Enforcement.CONNECTED
        binding.enforcementStatus.text = SessionText.availability(this, state)
        dialog?.findViewById<TextView>(R.id.unblockError)?.let {
            it.text = model.error.value
            it.isVisible = model.error.value != null
            dialog?.findViewById<View>(R.id.unBlockApp)?.isEnabled = session != null && !model.busy.value
            if (session == null) dialog?.dismiss()
        }
    }
    private fun unblockDialog() {
        if (current() == null) return
        val view = layoutInflater.inflate(R.layout.unblock_app_layout, null)
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<View>(R.id.cancel).setOnClickListener { dialog?.dismiss() }
        view.findViewById<View>(R.id.unBlockApp).setOnClickListener { current()?.let(model::unblockWithCrypto) }
        dialog?.show()
        render()
    }
    private fun timerDialog() {
        val view = layoutInflater.inflate(R.layout.select_time_layout, null)
        val hours = view.findViewById<NumberPicker>(R.id.hour_picker).apply { minValue = 0; maxValue = 23 }
        val minutes = view.findViewById<NumberPicker>(R.id.minute_picker).apply { minValue = 0; maxValue = 59 }
        val seconds = view.findViewById<NumberPicker>(R.id.second_picker).apply { minValue = 0; maxValue = 59 }
        dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<android.view.View>(R.id.cancel).setOnClickListener { dialog?.dismiss() }
        view.findViewById<android.view.View>(R.id.buttonStartTime).setOnClickListener {
            // NumberPicker commits keyboard edits when its input loses focus.
            hours.clearFocus()
            minutes.clearFocus()
            seconds.clearFocus()
            val duration = (hours.value * 3600L + minutes.value * 60L + seconds.value) * 1000
            if (duration == 0L) { ViewUtils.showToast(this, "Choose a duration greater than zero."); return@setOnClickListener }
            model.act { timer(target, label, duration, current()?.blockNotifications ?: false) }
            dialog?.dismiss()
        }
        dialog?.show()
    }
    private fun intervalDialog() {
        pickTime("Start time") { start -> pickTime("End time") { end ->
            val (from, until) = ScheduleEvaluator.interval(model.clock.now().wall, start, end, ZoneId.systemDefault())
            val format = java.text.DateFormat.getDateTimeInstance()
            dialog = AlertDialog.Builder(this).setTitle("Save one-time interval?")
                .setMessage("${format.format(java.util.Date(from))} – ${format.format(java.util.Date(until))}\nThe chosen instants stay fixed if the time zone changes.")
                .setNegativeButton(R.string.cancel, null).setPositiveButton("Save") { _, _ ->
                    model.act { interval(target, label, from, until, current()?.blockNotifications ?: false) }
                }.show()
        } }
    }
    private fun pickTime(title: String, selected: (LocalTime) -> Unit) {
        val now = LocalTime.now()
        dialog = TimePickerDialog(this, { _, hour, minute -> selected(LocalTime.of(hour, minute)) }, now.hour, now.minute,
            android.text.format.DateFormat.is24HourFormat(this)).apply { setTitle(title); show() }
    }
    override fun onDestroy() { dialog?.dismiss(); super.onDestroy() }
}
