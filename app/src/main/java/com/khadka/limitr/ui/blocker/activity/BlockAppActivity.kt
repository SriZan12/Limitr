package com.khadka.limitr.ui.blocker.activity

import android.app.TimePickerDialog
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.khadka.limitr.services.notifications.BlockingAlerts
import com.khadka.limitr.services.notifications.NotificationAccess
import javax.inject.Inject
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.NumberPicker
import android.widget.EditText
import android.widget.TextView
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.allViews
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
import com.khadka.limitr.play.*

@AndroidEntryPoint
class BlockAppActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlockAppBinding
    private val model: SessionViewModel by viewModels()
    private lateinit var target: String
    private lateinit var label: String
    private var essential = false
    private var dialog: android.app.Dialog? = null
    private var editor: String? = null
    private var intervalStart: LocalTime? = null
    private var intervalBounds: LongArray? = null
    @Inject lateinit var alerts: BlockingAlerts
    @Inject lateinit var updates: PlayUpdateRepository
    @Inject lateinit var reviews: PlayReviewRepository
    @Inject lateinit var playGuard: PlayUiGuard
    private var playHost: PlayEngagementHost? = null
    private var pendingNotificationEnable = false
    private val notificationAccessSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (pendingNotificationEnable) {
            pendingNotificationEnable = false
            if (Permissions.isNotificationServiceEnable(this)) model.setNotificationHiding(target, true)
            else ViewUtils.showToast(this, getString(R.string.notification_access_denied))
        }
        NotificationAccess.reconnectIfAllowed(this)
        render()
    }
    private val alertPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        lifecycleScope.launch { model.coordinator.refresh(); render() }
    }

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
        pendingNotificationEnable = savedInstanceState?.getBoolean("pending_notification_enable") ?: false
        alerts.createChannel()
        model.observeNotificationChoice(target)
        binding.appName.text = label
        binding.appIcon.setImageDrawable(ViewUtils.getAppIconByPackageName(this, target))
        binding.setTime.setOnClickListener { timerDialog() }
        binding.setInterval.setOnClickListener { intervalDialog() }
        binding.unBlockApp.setOnClickListener { unblockDialog() }
        binding.showHistory.setOnClickListener { startActivity(Intent(this, ActivityHistory::class.java)) }
        binding.blockNotification.setOnClickListener {
            // Capture the user's choice before busy/state emissions render the previously saved value.
            val enabled = binding.blockNotification.isChecked
            if (enabled && !Permissions.isNotificationServiceEnable(this)) {
                render()
                explainNotificationAccess(enableAfterGrant = true)
            } else model.setNotificationHiding(target, enabled)
        }
        binding.notificationAccessAction.setOnClickListener {
            if (!Permissions.isNotificationServiceEnable(this)) explainNotificationAccess(enableAfterGrant = true)
            else openNotificationAccess(enableAfterGrant = false)
        }
        binding.blockingAlertStatus.setOnClickListener { requestBlockingAlerts() }
        binding.enforcementStatus.setOnClickListener { PermissionOnboarding.show(this) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.coordinator.state.collect { render() } }
                launch { model.busy.collect { render() } }
                launch { model.notificationChoices.collect { render() } }
                launch { NotificationAccess.connected.collect { render() } }
                launch { model.error.collect { render() } }
                launch { model.unblocked.collect { render() } }
                launch { EnforcementStatus.state.collect { render() } }
                launch { while (isActive) { render(); delay(1000) } }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.alertSetup.collect { requested ->
                    if (requested) {
                        model.consumeAlertSetup()
                        if (Build.VERSION.SDK_INT >= 33 && !alerts.permissionPrompted &&
                            ContextCompat.checkSelfPermission(this@BlockAppActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestBlockingAlerts()
                        }
                    }
                }
            }
        }
        restoreEditor(savedInstanceState)
        playHost = attachPlayEngagement(binding.root, updates, reviews, playGuard, allowNewPrompts = false) {
            dialog?.isShowing != true && !model.busy.value &&
                supportFragmentManager.fragments.none { it is androidx.fragment.app.DialogFragment }
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        playHost?.refreshReadiness()
    }
    override fun onResume() {
        super.onResume()
        NotificationAccess.reconnectIfAllowed(this)
        render()
        lifecycleScope.launch { model.coordinator.refresh() }
    }
    private fun wantsNotificationHiding() = model.notificationChoices.value?.get(target) ?: current()?.blockNotifications ?: false

    private fun explainNotificationAccess(enableAfterGrant: Boolean) {
        dialog?.dismiss()
        editor = null
        dialog = AlertDialog.Builder(this).setTitle(R.string.notification_access_title)
            .setMessage(R.string.notification_access_message)
            .setNegativeButton(R.string.notification_access_not_now, null)
            .setPositiveButton(R.string.notification_access_agree) { _, _ -> openNotificationAccess(enableAfterGrant) }.show()
    }
    private fun openNotificationAccess(enableAfterGrant: Boolean) {
        pendingNotificationEnable = enableAfterGrant
        try { notificationAccessSettings.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        catch (_: ActivityNotFoundException) {
            pendingNotificationEnable = false
            ViewUtils.showToast(this, getString(R.string.notification_settings_unavailable))
        }
    }
    private fun requestBlockingAlerts() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            (!alerts.permissionPrompted || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            alerts.permissionPrompted = true
            alertPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val settings = when {
                Build.VERSION.SDK_INT >= 26 && !NotificationManagerCompat.from(this).areNotificationsEnabled() ->
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                Build.VERSION.SDK_INT >= 26 -> Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName).putExtra(Settings.EXTRA_CHANNEL_ID, BlockingAlerts.CHANNEL)
                else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            }
            try { startActivity(settings) }
            catch (_: ActivityNotFoundException) { ViewUtils.showToast(this, getString(R.string.notification_settings_unavailable)) }
        }
    }
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
        binding.blockNotification.isEnabled = canEdit && model.notificationChoices.value != null
        val hideNotifications = wantsNotificationHiding()
        binding.blockNotification.isChecked = hideNotifications
        val access = Permissions.isNotificationServiceEnable(this)
        val connected = NotificationAccess.connected.value
        val active = session?.let { ScheduleEvaluator.evaluate(it, model.clock.now()).phase == Phase.ACTIVE } == true
        val status = when {
            model.notificationChoices.value == null -> R.string.notification_hiding_loading
            !hideNotifications -> R.string.notification_hiding_off
            !access -> R.string.notification_hiding_access
            !connected -> R.string.notification_hiding_disconnected
            active -> R.string.notification_hiding_active
            else -> R.string.notification_hiding_ready
        }
        binding.notificationHidingStatus.setText(status)
        binding.notificationHidingExceptions.isVisible = hideNotifications
        binding.notificationAccessAction.isVisible = !essential && (!access || (hideNotifications && !connected))
        binding.notificationAccessAction.isEnabled = !model.busy.value
        binding.notificationAccessAction.setText(if (access) R.string.notification_access_reconnect else R.string.notification_access_setup)
        binding.blockingAlertStatus.isVisible = !alerts.enabled()
        binding.blockingAlertStatus.setText(R.string.blocking_alerts_off)
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
        editor = null
        val view = layoutInflater.inflate(R.layout.unblock_app_layout, null)
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<View>(R.id.cancel).setOnClickListener { dialog?.dismiss() }
        view.findViewById<View>(R.id.unBlockApp).setOnClickListener { current()?.let(model::unblockWithCrypto) }
        dialog?.show()
        render()
    }
    private fun timerDialog(parts: IntArray? = null) {
        dialog?.dismiss()
        editor = "timer"
        val view = layoutInflater.inflate(R.layout.select_time_layout, null)
        val hours = view.findViewById<NumberPicker>(R.id.hour_picker).apply { minValue = 0; maxValue = 23 }
        val minutes = view.findViewById<NumberPicker>(R.id.minute_picker).apply { minValue = 0; maxValue = 59 }
        val seconds = view.findViewById<NumberPicker>(R.id.second_picker).apply { minValue = 0; maxValue = 59 }
        if (parts?.size == 3) {
            hours.value = parts[0].coerceIn(0, 23)
            minutes.value = parts[1].coerceIn(0, 59)
            seconds.value = parts[2].coerceIn(0, 59)
        }
        dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<android.view.View>(R.id.cancel).setOnClickListener { dialog?.dismiss() }
        view.findViewById<android.view.View>(R.id.buttonStartTime).setOnClickListener {
            // NumberPicker commits keyboard edits when its input loses focus.
            hours.clearFocus()
            minutes.clearFocus()
            seconds.clearFocus()
            val duration = (hours.value * 3600L + minutes.value * 60L + seconds.value) * 1000
            if (duration == 0L) { ViewUtils.showToast(this, "Choose a duration greater than zero.", view); return@setOnClickListener }
            model.saveTimer(target, label, duration, wantsNotificationHiding())
            dialog?.dismiss()
        }
        dialog?.show()
    }
    private fun intervalDialog() {
        intervalStart = null
        pickTime("Start time", "interval_start") { start -> intervalEndDialog(start) }
    }
    private fun intervalEndDialog(start: LocalTime) {
        intervalStart = start
        pickTime("End time", "interval_end") { end ->
            val (from, until) = ScheduleEvaluator.interval(model.clock.now().wall, start, end, ZoneId.systemDefault())
            confirmInterval(from, until)
        }
    }
    private fun confirmInterval(from: Long, until: Long) {
        dialog?.dismiss()
        editor = "interval_confirm"
        intervalBounds = longArrayOf(from, until)
        val format = java.text.DateFormat.getDateTimeInstance()
        dialog = AlertDialog.Builder(this).setTitle("Save one-time interval?")
            .setMessage("${format.format(java.util.Date(from))} – ${format.format(java.util.Date(until))}\nThe chosen instants stay fixed if the time zone changes.")
            .setNegativeButton(R.string.cancel, null).setPositiveButton("Save") { _, _ ->
                model.saveInterval(target, label, from, until, wantsNotificationHiding())
            }.show()
    }
    private fun pickTime(title: String, kind: String, selected: (LocalTime) -> Unit) {
        dialog?.dismiss()
        editor = kind
        val now = LocalTime.now()
        dialog = TimePickerDialog(this, { _, hour, minute -> selected(LocalTime.of(hour, minute)) }, now.hour, now.minute,
            android.text.format.DateFormat.is24HourFormat(this)).apply {
                setTitle(title)
                show()
                window?.decorView?.let(AcmeTypography::applyTree)
            }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_notification_enable", pendingNotificationEnable)
        dialog?.takeIf { it.isShowing && editor != null }?.let { shown ->
            outState.putString("blocking_editor", editor)
            if (editor == "timer") {
                val parts = listOf(R.id.hour_picker, R.id.minute_picker, R.id.second_picker).map { id ->
                    val picker = shown.findViewById<NumberPicker>(id)
                    // Read the visible draft; value can still hold the previous
                    // committed number while the keyboard input has focus.
                    picker.allViews.filterIsInstance<EditText>().firstOrNull()?.text?.toString()
                        ?.toIntOrNull()?.coerceIn(picker.minValue, picker.maxValue) ?: picker.value
                }
                outState.putIntArray("timer_parts", parts.toIntArray())
            } else {
                outState.putBundle("time_picker", shown.onSaveInstanceState())
                intervalStart?.let { outState.putIntArray("interval_start", intArrayOf(it.hour, it.minute)) }
                outState.putLongArray("interval_bounds", intervalBounds)
            }
        }
        super.onSaveInstanceState(outState)
    }
    private fun restoreEditor(saved: Bundle?) {
        when (saved?.getString("blocking_editor")) {
            "timer" -> timerDialog(saved.getIntArray("timer_parts"))
            "interval_start" -> intervalDialog()
            "interval_end" -> saved.getIntArray("interval_start")?.takeIf { it.size == 2 }?.let {
                intervalEndDialog(LocalTime.of(it[0].coerceIn(0, 23), it[1].coerceIn(0, 59)))
            }
            "interval_confirm" -> saved.getLongArray("interval_bounds")?.takeIf { it.size == 2 && it[1] > it[0] }?.let {
                confirmInterval(it[0], it[1])
            }
        }
        saved?.getBundle("time_picker")?.let { (dialog as? TimePickerDialog)?.onRestoreInstanceState(it) }
    }
    override fun onDestroy() { dialog?.dismiss(); super.onDestroy() }
}
