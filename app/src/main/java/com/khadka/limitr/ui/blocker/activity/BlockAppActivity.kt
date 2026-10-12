package com.khadka.limitr.ui.blocker.activity

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.NumberPicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

@AndroidEntryPoint
class BlockAppActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlockAppBinding
    private val model: SessionViewModel by viewModels()
    private lateinit var target: String
    private lateinit var label: String
    private var essential = false
    private var dialog: android.app.Dialog? = null
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra("packageName")?.takeIf { it.isNotBlank() } ?: run { finish(); return }
        binding = ActivityBlockAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Insets.apply(binding.root)
        label = ViewUtils.getAppNameByPackageName(this, target)
        essential = target in EssentialApps.packages(this)
        binding.appName.text = label
        binding.appIcon.setImageDrawable(ViewUtils.getAppIconByPackageName(this, target))
        binding.setTime.setOnClickListener { timerDialog() }
        binding.setInterval.setOnClickListener { intervalDialog() }
        binding.unblockCrypto.setOnClickListener { current()?.let(model::unblockWithCrypto) }
        binding.unBlockApp.setOnClickListener {
            val session = current() ?: return@setOnClickListener
            dialog = AlertDialog.Builder(this).setTitle("Stop this session?")
                .setMessage("This will stop the saved timer or interval for $label.")
                .setNegativeButton(R.string.cancel, null).setPositiveButton("Stop session") { _, _ -> model.act { cancel(session) } }.show()
        }
        binding.blockNotification.setOnClickListener {
            val session = current() ?: return@setOnClickListener
            if (!Permissions.isNotificationServiceEnable(this)) {
                binding.blockNotification.isChecked = false
                dialog = AlertDialog.Builder(this).setTitle("Hide selected app notifications?")
                    .setMessage("LIMITR uses notification access to dismiss notifications only from apps with active sessions that you select. It does not save or transmit notification contents. Ongoing and essential system notifications are left available.")
                    .setNegativeButton("Not now", null).setPositiveButton("Agree and open settings") { _, _ ->
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }.show()
            } else model.act { notifications(session, binding.blockNotification.isChecked) }
        }
        binding.permissionSettings.setOnClickListener { PermissionOnboarding.show(this) }
        binding.sessionNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else ViewUtils.showToast(this, "Session notifications are available in Android notification settings.")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.coordinator.state.collect { render() } }
                launch { model.busy.collect { render() } }
                launch { model.error.collect { render() } }
                launch { EnforcementStatus.state.collect { render() } }
                launch { while (isActive) { render(); delay(1000) } }
            }
        }
    }
    override fun onResume() { super.onResume(); lifecycleScope.launch { model.coordinator.refresh() } }
    private fun current() = model.coordinator.state.value.sessions.firstOrNull { it.packageName == target }
    private fun render() {
        if (!::binding.isInitialized) return
        val state = model.coordinator.state.value
        val session = current()
        val canEdit = state.loaded && !state.error && !model.busy.value && !essential
        binding.setTime.isEnabled = canEdit
        binding.setInterval.isEnabled = canEdit
        binding.unBlockApp.isEnabled = session != null && !model.busy.value
        binding.unblockCrypto.isEnabled = session != null && !model.busy.value
        binding.blockNotification.isEnabled = session != null && !model.busy.value
        binding.blockNotification.isChecked = session?.blockNotifications == true
        binding.timerText.text = session?.let { SessionText.timing(it, model.clock.now()) } ?: "No saved session"
        binding.intervalText.text = model.error.value ?: if (essential) "Essential system apps remain accessible." else "Saving a new timer or interval replaces this app’s current session."
        binding.enforcementStatus.text = SessionText.availability(this, state)
    }
    private fun timerDialog() {
        val view = layoutInflater.inflate(R.layout.select_time_layout, null)
        val hours = view.findViewById<NumberPicker>(R.id.hour_picker).apply { minValue = 0; maxValue = 23 }
        val minutes = view.findViewById<NumberPicker>(R.id.minute_picker).apply { minValue = 0; maxValue = 59 }
        val seconds = view.findViewById<NumberPicker>(R.id.second_picker).apply { minValue = 0; maxValue = 59 }
        dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<android.view.View>(R.id.cancel).setOnClickListener { dialog?.dismiss() }
        view.findViewById<android.view.View>(R.id.buttonStartTime).setOnClickListener {
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
