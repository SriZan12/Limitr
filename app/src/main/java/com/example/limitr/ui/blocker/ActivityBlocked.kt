package com.example.limitr.ui.blocker

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.limitr.R
import com.example.limitr.databinding.ActivityBlockedBinding
import com.example.limitr.utils.ViewUtils
import com.example.limitr.utils.ViewUtils.setIntervalText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityBlocked : AppCompatActivity() {
    private lateinit var appName: String
    private lateinit var appPackage: String
    private lateinit var activityBlockedBinding: ActivityBlockedBinding
    private val appBlock = "appBlocker"
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBlockedBinding = DataBindingUtil.setContentView(this, R.layout.activity_blocked)
        setContentView(activityBlockedBinding.root)

        appName = intent.getStringExtra("appName").toString()
        appPackage = intent.getStringExtra("appPackage").toString()

        ViewUtils.loadProfilePhoto(activityBlockedBinding.profile, this)

        activityBlockedBinding.appIcon.setImageDrawable(getAppIconByPackageName(appPackage))

        remainingTimeViewModel.getRemainingTime(appName).observe(this) {

            if (it != null) {
                if (it.starTime != null && it.endTime != null) {
                    activityBlockedBinding.intervalText.isVisible = true
                    getIntervalForBlocking(it.starTime, it.endTime, it.remainingTime, it.appName)
                } else {
                    activityBlockedBinding.intervalText.isVisible = false
                    getTimer(it.blockedTime, it.remainingTime, appName)
                }
                appPackage = it.appPackage!!

            }

        }
    }

    private fun getIntervalForBlocking(
        startTime: Long?,
        endTime: Long?,
        remainingTime: Long?,
        appName: String
    ) {

        val currentRemainingTime = ViewUtils.getRemainingTime(startTime, remainingTime)
        val currentTime = System.currentTimeMillis()

        if (currentRemainingTime != null && currentRemainingTime > 0) {
            activityBlockedBinding.intervalText.text =
                setIntervalText(startTime!!, endTime!!)
            if (currentTime >= startTime &&
                currentTime <= endTime
            ) {
                activityBlockedBinding.timerText.visibility = View.VISIBLE
                ViewUtils.startTimer(activityBlockedBinding.timerText, currentRemainingTime)
            }
        } else {
            unBlockApp(appName)
            finish()
        }

    }

    private fun getTimer(starTime: Long?, remainingTime: Long?, appName: String) {
        val currentRemainingTime = ViewUtils.getRemainingTime(starTime, remainingTime)
        if (currentRemainingTime != null && currentRemainingTime > 0) {
            ViewUtils.startTimer(activityBlockedBinding.timerText, currentRemainingTime)
        } else {
            unBlockApp(appName)
        }
    }

    private fun unBlockApp(appName: String) {
        remainingTimeViewModel.deleteRemainingTime(appName)
            .observe(this) {
                Toast.makeText(this, "$appName is Free now!", Toast.LENGTH_SHORT).show()
            }
    }


    private fun getAppIconByPackageName(packageName: String): Drawable? {
        try {
            val pm = this.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            return appInfo.loadIcon(pm)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}