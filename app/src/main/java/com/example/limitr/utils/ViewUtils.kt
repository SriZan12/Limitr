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

    fun isAppBlocked(packageName: String?, context: Context): Boolean {
        // Retrieve the block list from shared preferences
        val prefs = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
        val blockList = prefs.getStringSet("block_list", setOf()) ?: setOf()

        // Check if the package name is in the block list
        return blockList.contains(packageName)
    }

    fun addAppToBlockList(packageName: String, duration: Long, context: Context) {
        // Retrieve the block list from shared preferences
        val prefs = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
        val blockList = prefs.getStringSet("block_list", mutableSetOf()) ?: mutableSetOf()

        // Add the package name to the block list
        blockList.add(packageName)

        // Store the updated block list and duration in shared preferences
        val editor = prefs.edit()
        editor.putStringSet("block_list", blockList)
        editor.putLong("$packageName:duration", System.currentTimeMillis())
    }


}