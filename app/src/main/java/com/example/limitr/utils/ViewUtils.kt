package com.example.limitr.utils

import android.app.TimePickerDialog
import android.app.usage.UsageStatsManager
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import android.widget.TimePicker
import com.bumptech.glide.Glide
import com.example.limitr.R
import com.example.limitr.ui.blocker.TimerClass
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
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

    fun setIntervalText(startTime: Long, endTime: Long): String {
        val timeFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val formattedEndTime = timeFormat.format(endTime)
        val formattedStartTime = timeFormat.format(startTime)

        return "Blocked For : $formattedStartTime-$formattedEndTime"
    }

    fun getRemainingTime(time: Long?, remainingTime: Long?): Long? {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - time!! // elapsed time means बितेको time
        return remainingTime?.minus(elapsedTime)
    }

    fun loadAppStatistic(context: Context, appPackage: String) {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val interval =
            UsageStatsManager.INTERVAL_DAILY // or INTERVAL_WEEKLY, INTERVAL_MONTHLY, etc.
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 24 // Show usage stats for the last 24 hours
        val usageStats = usageStatsManager.queryUsageStats(interval, startTime, endTime)

        if (usageStats.isNotEmpty()) {
            for (stat in usageStats) {
                if (stat.packageName == appPackage) { // Replace with your app's package name
                    val usageTime = stat.totalTimeInForeground / 1000 // In seconds
                    Timber.d("usageTime = $usageTime")
                    // Show the usage time in your app's UI
                }
            }
        } else {
            Timber.d("usageTime = No usage Found")
            // Show a message to the user that no usage stats are available
        }

    }


}