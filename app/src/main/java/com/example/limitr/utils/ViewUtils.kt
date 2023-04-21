package com.example.limitr.utils

import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Context
import  android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TimePicker
import com.bumptech.glide.Glide
import com.example.limitr.BuildConfig
import com.example.limitr.R
import com.example.limitr.ui.blocker.TimerClass
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


object ViewUtils {

    var onBackPressed: Boolean = false

    fun showTimePickerDialog(requireContext: Context, onResponse: (time: Date) -> Unit) {

        val calendar: Calendar = Calendar.getInstance()

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

    private fun setIntervalText(startTime: Long, endTime: Long): String {
        val timeFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val formattedEndTime = timeFormat.format(endTime)
        val formattedStartTime = timeFormat.format(startTime)

        return "Blocked For : $formattedStartTime-$formattedEndTime"
    }

    private fun getRemainingTime(time: Long?, remainingTime: Long?): Long? {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - time!! // elapsed time means बितेको time
        return remainingTime?.minus(elapsedTime)
    }

    fun animateProgressBar(progressBar: ProgressBar, currentProgress: Int) {
        progressBar.max = 100
        ObjectAnimator.ofInt(progressBar, "Progress", currentProgress)
            .setDuration(2000)
            .start()
    }

    fun getAppIconByPackageName(context: Context, packageName: String): Drawable? {
        val pm = context.packageManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val app: ApplicationInfo = pm.getApplicationInfo(
                packageName,
                ApplicationInfoFlags.of(0)
            )
            app.loadIcon(pm)
        } else {
            val app: ApplicationInfo = pm
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)

            Timber.d("AppName = $pm.getApplicationLabel(app) as String")
            app.loadIcon(pm)
        }
    }


    fun getAppNameByPackageName(context: Context, packageName: String): String {
        val pm = context.packageManager

        val versionName: String = BuildConfig.VERSION_NAME
        Timber.d("Version = ${Build.VERSION_CODES.TIRAMISU}")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val app: ApplicationInfo = pm.getApplicationInfo(
                packageName,
                ApplicationInfoFlags.of(0)
            )
            pm.getApplicationLabel(app) as String
        } else {
            val app: ApplicationInfo = pm
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            Timber.d("AppName = $pm.getApplicationLabel(app) as String")
            pm.getApplicationLabel(app) as String
        }
    }

    fun getIntervalForBlocking(
        startTime: Long?,
        endTime: Long?,
        remainingTime: Long?,
        appName: String,
        timerText: TextView,
        intervalTextView: TextView?,
    ): Boolean {

        val currentRemainingTime = getRemainingTime(startTime, remainingTime)
        val currentTime = System.currentTimeMillis()

        if (currentRemainingTime != null && currentRemainingTime > 0) {
            intervalTextView?.text =
                setIntervalText(startTime!!, endTime!!)
            if (currentTime >= startTime &&
                currentTime <= endTime
            ) {
                timerText.visibility = View.VISIBLE
                startTimer(timerText, currentRemainingTime)
            }
        } else {
            return true
        }

        return false

    }

    fun getTimer(
        starTime: Long?,
        remainingTime: Long?,
        appName: String,
        timerText: TextView
    ): Boolean {
        val currentRemainingTime = getRemainingTime(starTime, remainingTime)
        if (currentRemainingTime != null && currentRemainingTime > 0) {
            startTimer(timerText, currentRemainingTime)
        } else {
            return true
        }
        return false
    }

}