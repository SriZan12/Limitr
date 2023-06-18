package com.example.limitr.utils

import android.animation.ObjectAnimator
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import  android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.*
import javax.inject.Inject


object ViewUtils {

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

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val app: ApplicationInfo = pm.getApplicationInfo(
                packageName,
                ApplicationInfoFlags.of(0)
            )
            pm.getApplicationLabel(app) as String
        } else {
            val app: ApplicationInfo = pm
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(app) as String
        }
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


}