package com.example.limitr.ui.blocker.activity

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import com.example.limitr.R
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.databinding.ActivityBlockAppBinding
import com.example.limitr.ui.blocker.vm.BlockedAppVM
import com.example.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.example.limitr.utils.Constants
import com.example.limitr.utils.DateAndTime
import com.example.limitr.utils.NotificationUtils
import com.example.limitr.utils.Permissions
import com.example.limitr.utils.ViewUtils
import com.example.limitr.utils.dialogShow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class BlockAppActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks{

    private lateinit var binding: ActivityBlockAppBinding
    private lateinit var appPackage: String
    private val blockedAppVM: BlockedAppVM by viewModels()
    private lateinit var appName: String
    private lateinit var appIcon: Drawable
    private var startTime: Date? = null
    private var endTime: Date? = null
    private var unBlockAppStatus: Boolean = false
    private lateinit var dialog: Dialog
    private val mainViewModel: MainFragmentViewModel by viewModels()


    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var editor: SharedPreferences.Editor

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_block_app)
        setContentView(binding.root)

        dialog =
            dialogShow(
                this@BlockAppActivity,
                R.layout.notification_dialog
            )

        requestPostNotificationPermissionForAlarm()
        setView()

        binding.setTime.setOnClickListener {

            val dialog = dialogShow(this@BlockAppActivity, R.layout.select_time_layout)
            setNumberPicker(dialog, appName)
        }

        binding.unBlockApp.setOnClickListener {
            unBlockAppByCrypto()
        }

        binding.setInterval.setOnClickListener {

            ViewUtils.showTimePickerDialog(this@BlockAppActivity) { stTime ->
                startTime = stTime
                ViewUtils.showTimePickerDialog(this@BlockAppActivity) { edTime ->
                    endTime = edTime
                    val currentTime = System.currentTimeMillis()
                    checkConditionForTimeInterval(currentTime, startTime!!, endTime!!)
                }
            }
        }


        binding.blockNotification.setOnClickListener {

            if (!Permissions.isNotificationServiceEnable(this@BlockAppActivity)) {
                showNotificationDialog()
            } else {

                if (binding.blockNotification.isChecked) {
                    setNotificationStatus(true)

                } else if (!binding.blockNotification.isChecked) {
                    setNotificationStatus(false)
                }
            }
        }

    }


    override fun onResume() {
        super.onResume()

        if (Permissions.isNotificationServiceEnable(this@BlockAppActivity)) {
            binding.linearLayout2.isVisible = true

            val packageName = intent.getStringExtra(this.getString(R.string.packageName))
            val app = ViewUtils.getAppNameByPackageName(this@BlockAppActivity, packageName!!)
            if (dialog.isShowing) {
                editor.putBoolean(app, true)
                editor.apply()
                binding.blockNotification.isChecked = true
                dialog.dismiss()
            }
        }
    }

    private fun setView() {
        appPackage = intent.getStringExtra(this.getString(R.string.packageName)).toString()

        appIcon = ViewUtils.getAppIconByPackageName(this@BlockAppActivity, appPackage)!!

        appName = ViewUtils.getAppNameByPackageName(this@BlockAppActivity, appPackage)

        binding.appName.text =
            ViewUtils.getAppNameByPackageName(this@BlockAppActivity, appPackage)
        binding.appIcon.setImageDrawable(appIcon)


        blockedAppVM.getRemainingTime(appName).observe(this) {

            if (it != null) {
                binding.unBlockApp.isEnabled = true
                binding.setTime.isEnabled = false
                binding.setInterval.isEnabled = false

                unBlockAppStatus = if (it.starTime != null && it.endTime != null) {
                    binding.setTimerText.text = getString(R.string.duration)
                    binding.setIntervalText.text = getString(R.string.blocked_for)
                    binding.timerText.isVisible = true
                    binding.intervalText.isVisible = true
                    DateAndTime.getIntervalForBlocking(
                        it.starTime,
                        it.endTime,
                        it.remainingTime,
                        binding.timerText,
                        binding.intervalText
                    )
                } else {
                    binding.timerText.isVisible = true
                    binding.setTimerText.text = getString(R.string.duration)
                    DateAndTime.getTimer(
                        it.blockedTime!!,
                        it.remainingTime,
                        timerText = binding.timerText
                    )
                }

                val isNotificationOn = sharedPref.getBoolean(appName, false)

                if (isNotificationOn) {
                    binding.blockNotification.isChecked = true
                }

                if (it.blockedTime!! <= 0) {
                    removeNotificationStatus()
                }

                if(unBlockAppStatus){
                    unBlockApp(appName)
                }

            }

        }

    }

    private fun unBlockApp(appName: String) {

        blockedAppVM.deleteRemainingTime(appName)
            .observe(this) {
                ViewUtils.showToast(this@BlockAppActivity, "$appName is free now!")
                removeNotificationStatus()
                binding.setTime.isEnabled = true
                binding.setInterval.isEnabled = true
                binding.setTimerText.text = getString(R.string.set_timer)
                binding.setIntervalText.text = getString(R.string.set_interval)
            }
    }

    private fun removeNotificationStatus() {
        binding.blockNotification.isChecked = false
        editor.remove(appName)
        editor.apply()
    }

    private fun setNotificationStatus(status: Boolean) {
        editor.putBoolean(appName, status)
        editor.apply()
        binding.blockNotification.isChecked = status
    }


    private fun setIntervalForBlocking(startTime: Date?, endTime: Date?) {

        val interval = endTime?.time!! - startTime?.time!!

        saveRemainingTime(
            interval,
            appName,
            startTime.time,
            endTime.time,
        )

        NotificationUtils.startNotification(
            this@BlockAppActivity,
            appName,
            startTime.time,
            interval,
            appIcon.toBitmap()
        )
        NotificationUtils.endNotification(
            this@BlockAppActivity,
            appName,
            endTime.time,
            appIcon.toBitmap()

        )

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setNumberPicker(dialog: Dialog, appName: String?) {

        dialog.show()

        var hour: Int
        var minute: Int
        var second: Int

        val hourPicker: NumberPicker = dialog.findViewById(R.id.hour_picker)
        val minutePicker: NumberPicker = dialog.findViewById(R.id.minute_picker)
        val secondPicker: NumberPicker = dialog.findViewById(R.id.second_picker)
        val buttonStartTime: TextView = dialog.findViewById(R.id.buttonStartTime)
        val cancel: ImageView = dialog.findViewById(R.id.cancel)

        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        hourPicker.wrapSelectorWheel = true

        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.wrapSelectorWheel = true

        secondPicker.minValue = 0
        secondPicker.maxValue = 59
        secondPicker.wrapSelectorWheel = true

        hourPicker.setOnValueChangedListener { _, _, newVal ->
        }

        minutePicker.setOnValueChangedListener { _, _, newVal ->
        }

        secondPicker.setOnValueChangedListener { _, _, newVal ->

        }

        buttonStartTime.setOnClickListener {

            hour = hourPicker.value
            minute = minutePicker.value
            second = secondPicker.value

            setTimer(hour, minute, second)
            dialog.dismiss()

        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }

    }

    private fun setTimer(hour: Int, minute: Int, second: Int) {

        val interval = (hour * 60 * 60 + minute * 60 + second) * 1000L

        ViewUtils.startTimer(binding.timerText, interval)

        saveRemainingTime(interval, appName, null, null)

        NotificationUtils.startNotification(
            this@BlockAppActivity,
            appName,
            System.currentTimeMillis(),
            interval,
            appIcon.toBitmap()
        )
        NotificationUtils.endNotification(
            this@BlockAppActivity,
            appName,
            System.currentTimeMillis() + interval,
            appIcon.toBitmap()
        )

        dialog.dismiss()

    }


    private fun saveRemainingTime(
        time: Long,
        appName: String?,
        startTime: Long?,
        endTime: Long?,
    ) {

        val limitrEntities =
            LimitrEntities(
                appName!!,
                System.currentTimeMillis(),
                time,
                appPackage,
                startTime,
                endTime,
                false
            )

        blockedAppVM.insertRemainingTime(limitrEntities).observe(this) {
            ViewUtils.showToast(this@BlockAppActivity, "$appName is Blocked!")
            if (!Permissions.isNotificationServiceEnable(this@BlockAppActivity)) {
                showNotificationDialog()
            } else {
                setNotificationStatus(true)
            }

        }
    }

    private fun checkConditionForTimeInterval(currentTime: Long, startTime: Date, endTime: Date) {
        if (currentTime > startTime.time) {
            val newStarTime =
                Date(startTime.time.plus(86400000L))// 1 day in milliseconds
            val newEndTime =
                Date(endTime.time.plus(86400000L)) // 1 day in milliseconds
            setIntervalForBlocking(newStarTime, newEndTime)
        } else if (currentTime > endTime.time) {
            val newEndTime =
                Date(endTime.time.plus(86400000L))
            setIntervalForBlocking(startTime, newEndTime)
        } else {
            setIntervalForBlocking(startTime, endTime)
        }
    }


    private fun gotoSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.apply {
            Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun showNotificationDialog() {

        val grantPermission: TextView = dialog.findViewById(R.id.grantPermission)
        val cancel: ImageView = dialog.findViewById(R.id.cancel)

        grantPermission.setOnClickListener {
            gotoSettings()
        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun unBlockAppByCrypto() {

        val cryptoDialog = dialogShow(this@BlockAppActivity, R.layout.unblock_app_layout)

        cryptoDialog.show()

        val unBlockButton: Button = cryptoDialog.findViewById(R.id.unBlockApp)
        val cancel: ImageView = cryptoDialog.findViewById(R.id.cancel)

        cancel.setOnClickListener {
            cryptoDialog.dismiss()
        }

        unBlockButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val crypto = mainViewModel.getCrypto().first()
                if (crypto >= Constants.REQUIRED_CRYPTO_FOR_UNBLOCK) {
                    val deductCrypto = crypto - Constants.REQUIRED_CRYPTO_FOR_UNBLOCK
                    mainViewModel.upsertCrypto(deductCrypto)
                    unBlockApp(appName)
                    NotificationUtils.cancelNotification(this@BlockAppActivity, appName)
                    cryptoDialog.dismiss()
                    finish()
                } else {
                    ViewUtils.showToast(this@BlockAppActivity, getString(R.string.not_enough_crypto))
                    cryptoDialog.dismiss()
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasPostNotification(): Boolean {
        return EasyPermissions.hasPermissions(
            this@BlockAppActivity,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPostNotificationPermissionForAlarm() {
        if (!hasPostNotification()) {
            EasyPermissions.requestPermissions(
                this,
                "This app needs permission to post alarm notifications.",
                Constants.RC_POST_NOTIFICATION_PERMISSION,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        TODO("Not yet implemented")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        TODO("Not yet implemented")
    }
}