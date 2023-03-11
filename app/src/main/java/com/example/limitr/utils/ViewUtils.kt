package com.example.limitr.utils

import android.app.TimePickerDialog
import android.content.Context
import android.widget.TextView
import android.widget.TimePicker
import com.example.limitr.ui.blocker.TimerClass
import java.util.*

object ViewUtils {

    private val calendar: Calendar = Calendar.getInstance()


    fun showTimePickerDialog(requireContext: Context, onResponse: (time: Date) -> Unit) {

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext,
            { _: TimePicker, HourOfDay, Minute ->
                calendar.set(Calendar.HOUR_OF_DAY, HourOfDay)
                calendar.set(Calendar.MINUTE, Minute)
                onResponse(calendar.time)
            }, hour, minute, false
        )
        timePickerDialog.show()
    }

    fun startTimer(timerText: TextView, givenTime: Long) {
        TimerClass(timerText, givenTime).start()
    }


}