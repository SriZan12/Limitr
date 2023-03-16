package com.example.limitr.utils

import android.app.TimePickerDialog
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import android.widget.TimePicker
import com.bumptech.glide.Glide
import com.example.limitr.R
import com.example.limitr.resource.PermissionState
import com.example.limitr.ui.blocker.TimerClass
import com.google.firebase.auth.FirebaseAuth
import java.security.Permission
import java.util.*

object ViewUtils {

    private val calendar: Calendar = Calendar.getInstance()
    var isAccessibilityServiceEnabled: Any = PermissionState.Denied


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

    fun accessibilityPermissionState(permissionState: Any): Any {
        return permissionState
    }


}