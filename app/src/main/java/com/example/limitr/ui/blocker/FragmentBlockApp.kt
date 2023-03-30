package com.example.limitr.ui.blocker

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.limitr.R
import com.example.limitr.data.room.model.LimitrEntities
import com.example.limitr.databinding.FragmentAppBlockBinding
import com.example.limitr.ui.home.model.AppInfoModel
import com.example.limitr.utils.NotificationUtils.endNotification
import com.example.limitr.utils.NotificationUtils.startNotification
import com.example.limitr.utils.ViewUtils.getRemainingTime
import com.example.limitr.utils.ViewUtils.setIntervalText
import com.example.limitr.utils.ViewUtils.showTimePickerDialog
import com.example.limitr.utils.ViewUtils.startTimer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class FragmentBlockApp : Fragment(R.layout.fragment_app_block) {

    private lateinit var fragmentAppBlockBinding: FragmentAppBlockBinding
    private val fragmentBlockAppArgs: FragmentBlockAppArgs by navArgs()
    private lateinit var appInfoModel: AppInfoModel
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()
    private val appBlock = "appBlocker"
    private lateinit var appIcon: Bitmap
    private var startTime: Date? = null
    private var endTime: Date? = null
    private var remainingTime: Long = 0
    private var diff: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentAppBlockBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_app_block, container, false)
        return fragmentAppBlockBinding.root
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appInfoModel = fragmentBlockAppArgs.appInfo

        appIcon = appInfoModel.appIcon.toBitmap()

        fragmentAppBlockBinding.appName.text = appInfoModel.appName
        fragmentAppBlockBinding.appIcon.setImageDrawable(appInfoModel.appIcon)


        fragmentAppBlockBinding.setTime.setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.window?.setContentView(R.layout.select_time_layout)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.show()

            setNumberPicker(dialog, appInfoModel.appName)
        }

        remainingTimeViewModel.getRemainingTime(appInfoModel.appName).observe(viewLifecycleOwner) {

            if (it != null) {

                if (it.starTime != null && it.endTime != null) {
                    getIntervalForBlocking(it.starTime, it.endTime, it.remainingTime)
                } else {
                    getTimer(it.blockedTime!!, it.remainingTime)
                }

                fragmentAppBlockBinding.setTime.isEnabled = false
                fragmentAppBlockBinding.startTime.isEnabled = false

            }
        }

        fragmentAppBlockBinding.startTime.setOnClickListener {
            showTimePickerDialog(requireContext()) { stTime ->
                startTime = stTime
                showTimePickerDialog(requireContext()) { edTime ->
                    endTime = edTime
                    setIntervalForBlocking(startTime, endTime)
                }
            }
        }
    }

    private fun getIntervalForBlocking(startTime: Long?, endTime: Long?, remainingTime: Long?) {

        val currentRemainingTime = getRemainingTime(startTime, remainingTime)
        val currentTime = System.currentTimeMillis()

        if (currentRemainingTime != null && currentRemainingTime > 0) {
            fragmentAppBlockBinding.textInterval.text =
                setIntervalText(startTime!!, endTime!!)
            if (currentTime >= startTime &&
                currentTime <= endTime
            ) {
                fragmentAppBlockBinding.timerText.visibility = View.VISIBLE
                startTimer(fragmentAppBlockBinding.timerText, currentRemainingTime!!)
            }
        } else {
            unBlockApp(appInfoModel.appName!!)
        }

    }

    private fun getTimer(starTime: Long?, remainingTime: Long?) {
        val currentRemainingTime = getRemainingTime(starTime, remainingTime)
        if (currentRemainingTime != null && currentRemainingTime > 0) {
            startTimer(fragmentAppBlockBinding.timerText, currentRemainingTime)
        } else {
            unBlockApp(appInfoModel.appName!!)
        }
    }

    private fun unBlockApp(appName: String) {
        fragmentAppBlockBinding.startTime.isEnabled = true
        fragmentAppBlockBinding.setTime.isEnabled = true
        remainingTimeViewModel.deleteRemainingTime(appName)
            .observe(viewLifecycleOwner) {
                Toast.makeText(requireContext(), "$appName is free now!", Toast.LENGTH_SHORT).show()
            }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setIntervalForBlocking(startTime: Date?, endTime: Date?) {
        if (endTime == null) {
            Toast.makeText(requireContext(), "Please Set End Time", Toast.LENGTH_SHORT).show()
            return
        } else if (startTime == null) {
            Toast.makeText(requireContext(), "Please Set Start Time", Toast.LENGTH_SHORT).show()
            return
        } else {
            val timeInMillis = endTime.time - startTime.time
            saveRemainingTime(
                timeInMillis,
                appInfoModel.appName,
                startTime,
                endTime
            )

            startNotification(
                requireContext(),
                appInfoModel.appName!!,
                startTime.time,
                timeInMillis,
                appIcon
            )
            endNotification(
                requireContext(),
                appInfoModel.appName!!,
                endTime.time,
                appIcon

            )

        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setNumberPicker(dialog: Dialog, appName: String?) {

        var hour = 0
        var minute = 0
        var second = 0

        val hourPicker: NumberPicker = dialog.findViewById(R.id.hour_picker)
        val minutePicker: NumberPicker = dialog.findViewById(R.id.minute_picker)
        val secondPicker: NumberPicker = dialog.findViewById(R.id.second_picker)
        val buttonStartTime: TextView = dialog.findViewById(R.id.buttonStartTime)

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

            val timeInMillis = (hour * 60 * 60 + minute * 60 + second) * 1000L
            Timber.d("timeInMillis = $timeInMillis")

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, second)

            startTimer(fragmentAppBlockBinding.timerText, timeInMillis)

            saveRemainingTime(timeInMillis, appName, null, null)
            startNotification(
                requireContext(),
                appInfoModel.appName!!,
                System.currentTimeMillis(),
                timeInMillis,
                appIcon
            )
            endNotification(
                requireContext(),
                appInfoModel.appName!!,
                System.currentTimeMillis() + timeInMillis,
                appIcon
            )

            dialog.dismiss()

        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveRemainingTime(
        time: Long,
        appName: String?,
        startTime: Date?,
        endTime: Date?
    ) {

        val limitrEntities =
            LimitrEntities(
                appName!!,
                System.currentTimeMillis(),
                time,
                appInfoModel.appPackage,
                startTime?.time,
                endTime?.time
            )

        remainingTimeViewModel.insertRemainingTime(limitrEntities).observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "Inserted Successfully", Toast.LENGTH_SHORT).show()
        }
    }

}