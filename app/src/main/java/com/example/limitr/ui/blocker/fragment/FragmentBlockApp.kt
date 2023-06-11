package com.example.limitr.ui.blocker.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.Notification
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.limitr.R
import com.example.limitr.common.showDialog
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.databinding.BlockAppFragmentBinding

import com.example.limitr.ui.blocker.vm.RemainingTimeViewModel
import com.example.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.example.limitr.utils.Constants.RC_POST_NOTIFICATION_PERMISSION
import com.example.limitr.utils.Constants.REQUIREDCRYPTOFORUNBLOCK
import com.example.limitr.utils.DateAndTime.getIntervalForBlocking
import com.example.limitr.utils.DateAndTime.getTimer
import com.example.limitr.utils.NotificationUtils.cancelNotification
import com.example.limitr.utils.NotificationUtils.endNotification
import com.example.limitr.utils.NotificationUtils.startNotification
import com.example.limitr.utils.Permissions.isNotificationServiceEnable
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName
import com.example.limitr.utils.ViewUtils.getAppNameByPackageName
import com.example.limitr.utils.ViewUtils.showTimePickerDialog
import com.example.limitr.utils.ViewUtils.startTimer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import com.example.limitr.utils.ViewUtils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject


@AndroidEntryPoint
class FragmentBlockApp :
    Fragment(R.layout.block_app_fragment), EasyPermissions.PermissionCallbacks {

    private lateinit var fragmentAppBlockBinding: BlockAppFragmentBinding
    private val fragmentBlockAppArgs: FragmentBlockAppArgs by navArgs()
    private lateinit var appPackage: String
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentAppBlockBinding =
            DataBindingUtil.inflate(inflater, R.layout.block_app_fragment, container, false)
        return fragmentAppBlockBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dialog = showDialog(requireContext(), R.layout.notification_dialog)
    }

    override fun onResume() {
        super.onResume()

        if (isNotificationServiceEnable(requireContext())) {
            fragmentAppBlockBinding.linearLayout2.isVisible = true
            val pgName = fragmentBlockAppArgs.appInfo.toString()
            val app = getAppNameByPackageName(requireContext(), pgName)
            if (dialog.isShowing) {
                editor.putBoolean(app, true)
                editor.apply()
                fragmentAppBlockBinding.blockNotification.isChecked = true
                dialog.dismiss()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPostNotificationPermissionForAlarm()
        setView()

        fragmentAppBlockBinding.setTime.setOnClickListener {

            val dialog = showDialog(requireContext(), R.layout.select_time_layout)
            setNumberPicker(dialog, appName)
        }

        fragmentAppBlockBinding.unBlockApp.setOnClickListener {
            unBlockAppByCrypto()
        }

        fragmentAppBlockBinding.setInterval.setOnClickListener {

            showTimePickerDialog(requireContext()) { stTime ->
                startTime = stTime
                showTimePickerDialog(requireContext()) { edTime ->
                    endTime = edTime
                    val currentTime = System.currentTimeMillis()
                    checkConditionForTimeInterval(currentTime, startTime!!, endTime!!)
                }
            }
        }


        fragmentAppBlockBinding.blockNotification.setOnClickListener {

            if (!isNotificationServiceEnable(requireContext())) {
                showNotificationDialog()
            } else {

                if (fragmentAppBlockBinding.blockNotification.isChecked) {
                    setNotificationStatus(true)

                } else if (!fragmentAppBlockBinding.blockNotification.isChecked) {
                    setNotificationStatus(false)
                }
            }
        }

    }

    private fun setView() {
        appPackage = fragmentBlockAppArgs.appInfo.toString()

        appIcon = getAppIconByPackageName(requireContext(), appPackage)!!

        appName = getAppNameByPackageName(requireContext(), appPackage)

        fragmentAppBlockBinding.appName.text = getAppNameByPackageName(requireContext(), appPackage)
        fragmentAppBlockBinding.appIcon.setImageDrawable(appIcon)


        remainingTimeViewModel.getRemainingTime(appName).observe(viewLifecycleOwner) {

            if (it != null) {
                fragmentAppBlockBinding.unBlockApp.isEnabled = true

                unBlockAppStatus = if (it.starTime != null && it.endTime != null) {
                    fragmentAppBlockBinding.setTimerText.text = getString(R.string.duration)
                    fragmentAppBlockBinding.setIntervalText.text = getString(R.string.blocked_for)
                    fragmentAppBlockBinding.timerText.isVisible = true
                    fragmentAppBlockBinding.intervalText.isVisible = true
                    getIntervalForBlocking(
                        it.starTime,
                        it.endTime,
                        it.remainingTime,
                        fragmentAppBlockBinding.timerText,
                        fragmentAppBlockBinding.intervalText
                    )
                } else {
                    fragmentAppBlockBinding.timerText.isVisible = true
                    fragmentAppBlockBinding.setTimerText.text = getString(R.string.duration)
                    getTimer(
                        it.blockedTime!!,
                        it.remainingTime,
                        timerText = fragmentAppBlockBinding.timerText
                    )
                }

                val isNotificationOn = sharedPref.getBoolean(appName, false)

                if (isNotificationOn) {
                    fragmentAppBlockBinding.blockNotification.isChecked = true
                }

                fragmentAppBlockBinding.setTime.isEnabled = false
                fragmentAppBlockBinding.setInterval.isEnabled = false

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

        remainingTimeViewModel.deleteRemainingTime(appName)
            .observe(viewLifecycleOwner) {
                showToast(requireContext(), "$appName is free now!")
                removeNotificationStatus()
            }
    }

    private fun removeNotificationStatus() {
        fragmentAppBlockBinding.blockNotification.isChecked = false
        editor.remove(appName)
        editor.apply()
    }

    private fun setNotificationStatus(status: Boolean) {
        editor.putBoolean(appName, status)
        editor.apply()
        fragmentAppBlockBinding.blockNotification.isChecked = status
    }


    private fun setIntervalForBlocking(startTime: Date?, endTime: Date?) {

        val interval = endTime?.time!! - startTime?.time!!

        saveRemainingTime(
            interval,
            appName,
            startTime.time,
            endTime.time,
        )

        startNotification(
            requireContext(),
            appName,
            startTime.time,
            interval,
            appIcon.toBitmap()
        )
        endNotification(
            requireContext(),
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

        startTimer(fragmentAppBlockBinding.timerText, interval)

        saveRemainingTime(interval, appName, null, null)

        startNotification(
            requireContext(),
            appName,
            System.currentTimeMillis(),
            interval,
            appIcon.toBitmap()
        )
        endNotification(
            requireContext(),
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

        remainingTimeViewModel.insertRemainingTime(limitrEntities).observe(viewLifecycleOwner) {
            showToast(requireContext(), "$appName is Blocked!")
            if (!isNotificationServiceEnable(requireContext())) {
                showNotificationDialog()
            } else {
                setNotificationStatus(true)
            }
        }
    }

    private fun checkConditionForTimeInterval(currentTime: Long, startTime: Date, endTime: Date) {
        if (currentTime > startTime.time) {
            Timber.d("Inside If")
            val newStarTime =
                Date(startTime.time.plus(86400000L))// 1 day in milliseconds
            val newEndTime =
                Date(endTime.time.plus(86400000L)) // 1 day in milliseconds
            setIntervalForBlocking(newStarTime, newEndTime)
        } else if (currentTime > endTime.time) {
            Timber.d("Inside else If")
            val newEndTime =
                Date(endTime.time.plus(86400000L))
            setIntervalForBlocking(startTime, newEndTime)
        } else {
            Timber.d("Inside else")
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

        val cryptoDialog = showDialog(requireContext(), R.layout.unblock_app_layout)

        cryptoDialog.show()

        val unBlockButton: Button = cryptoDialog.findViewById(R.id.unBlockApp)
        val cancel: ImageView = cryptoDialog.findViewById(R.id.cancel)

        cancel.setOnClickListener {
            cryptoDialog.dismiss()
        }

        unBlockButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val crypto = mainViewModel.getCrypto().first()
                if (crypto >= REQUIREDCRYPTOFORUNBLOCK) {
                    val deductCrypto = crypto - REQUIREDCRYPTOFORUNBLOCK
                    mainViewModel.upsertCrypto(deductCrypto)
                    unBlockApp(appName)
                    cancelNotification(requireContext(), appName)
                    cryptoDialog.dismiss()
                    findNavController().popBackStack()
                } else {
                    showToast(requireContext(), getString(R.string.not_enough_crypto))
                    cryptoDialog.dismiss()
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasPostNotification(): Boolean {
        return EasyPermissions.hasPermissions(
            requireContext(),
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPostNotificationPermissionForAlarm() {
        if (!hasPostNotification()) {
            EasyPermissions.requestPermissions(
                this,
                "This app needs permission to post alarm notifications.",
                RC_POST_NOTIFICATION_PERMISSION,
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