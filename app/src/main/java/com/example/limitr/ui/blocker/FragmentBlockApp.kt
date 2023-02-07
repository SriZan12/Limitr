package com.example.limitr.ui.blocker

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.limitr.R
import com.example.limitr.data.room.model.LimitrEntities
import com.example.limitr.databinding.FragmentAppBlockBinding
import com.example.limitr.ui.home.model.AppInfoModel
import com.example.limitr.utils.ViewUtils.startTimer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentBlockApp : Fragment(R.layout.fragment_app_block) {

    private lateinit var fragmentAppBlockBinding: FragmentAppBlockBinding
    private val fragmentBlockAppArgs: FragmentBlockAppArgs by navArgs()
    private lateinit var appInfoModel: AppInfoModel
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()
    private val appBlock = "appBlocker"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentAppBlockBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_app_block, container, false)
        return fragmentAppBlockBinding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appInfoModel = fragmentBlockAppArgs.appInfo

        fragmentAppBlockBinding.appName.text = appInfoModel.appName
        fragmentAppBlockBinding.appIcon.setImageDrawable(appInfoModel.appIcon)


        fragmentAppBlockBinding.startTimer.setOnClickListener {
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
                Log.d(appBlock, "onViewCreated: ${it.appName}")
                fragmentAppBlockBinding.timerText.visibility = View.VISIBLE
                val elapsedTime = System.currentTimeMillis() - it.startTime!!
                val currentRemainingTime = it.remainingTime?.minus(elapsedTime)
                val hours = currentRemainingTime?.div(3600)
                val minutes = (currentRemainingTime?.rem(3600))?.div(60)
                val seconds = currentRemainingTime?.rem(60)

                val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                if (currentRemainingTime != null && currentRemainingTime > 0) {
                    startTimer(fragmentAppBlockBinding.timerText, currentRemainingTime)
                }
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setNumberPicker(dialog: Dialog, appName: String?) {

        var hour: Int = 0
        var minute: Int = 0
        var second: Int = 0

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
            Log.d(appBlock, "Time in milliseconds: $timeInMillis")

//            val time = Time(timeInMillis)

            startTimer(fragmentAppBlockBinding.timerText, timeInMillis)

            saveRemainingTime(timeInMillis, appName)

            dialog.dismiss()

        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveRemainingTime(time: Long, appName: String?) {

        val limitrEntities = LimitrEntities(appName!!, System.currentTimeMillis(), time)
//        limitrEntities.startTime = System.currentTimeMillis()
//        limitrEntities.remainingTime = time
//        limitrEntities.appName = appName

        remainingTimeViewModel.insertRemainingTime(limitrEntities).observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "Inserted Successfully", Toast.LENGTH_SHORT).show()
        }
    }
}