package com.example.limitr.utils

import android.app.TimePickerDialog
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import android.widget.TimePicker
import com.bumptech.glide.Glide
import com.example.limitr.R
import com.example.limitr.ui.blocker.TimerClass
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
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

    fun loadProfilePhoto(imageView: ImageView, context: Context) {
        Glide.with(context)
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .placeholder(R.drawable.user)
            .into(imageView)
    }

    fun setInterval(startTime: Long,endTime:Long): String {
        val timeFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val formattedEndTime = timeFormat.format(endTime)
        val formattedStartTime = timeFormat.format(startTime)

        return "Blocked For : $formattedStartTime-$formattedEndTime"
    }

}