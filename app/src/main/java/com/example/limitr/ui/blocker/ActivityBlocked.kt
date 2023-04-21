package com.example.limitr.ui.blocker

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.limitr.R
import com.example.limitr.databinding.ActivityBlockedBinding
import com.example.limitr.utils.ViewUtils
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName
import com.example.limitr.utils.ViewUtils.getIntervalForBlocking
import com.example.limitr.utils.ViewUtils.getTimer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ActivityBlocked : AppCompatActivity() {
    private lateinit var appName: String
    private lateinit var appPackage: String
    private lateinit var activityBlockedBinding: ActivityBlockedBinding
    private val appBlock = "appBlocker"
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()
    private var unBlockAppStatus: Boolean = false


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBlockedBinding = DataBindingUtil.setContentView(this, R.layout.activity_blocked)
        setContentView(activityBlockedBinding.root)

        appName = intent.getStringExtra("appName").toString()
        appPackage = intent.getStringExtra("appPackage").toString()

        ViewUtils.loadProfilePhoto(activityBlockedBinding.profile, this)

        activityBlockedBinding.appIcon.setImageDrawable(
            getAppIconByPackageName(
                this,
                appPackage
            )
        )
        activityBlockedBinding.appName.text = appName

        remainingTimeViewModel.getRemainingTime(appName).observe(this) {

            if (it != null) {
                if (it.starTime != null && it.endTime != null) {
                    activityBlockedBinding.intervalText.isVisible = true
                    unBlockAppStatus = getIntervalForBlocking(
                        it.starTime,
                        it.endTime,
                        it.remainingTime,
                        it.appName,
                        activityBlockedBinding.timerText,
                        activityBlockedBinding.intervalText
                    )
                    if (unBlockAppStatus) {
                        unBlockApp(appName)
                    }
                } else {
                    activityBlockedBinding.intervalText.isVisible = false
                    unBlockAppStatus = getTimer(
                        it.blockedTime,
                        it.remainingTime,
                        appName,
                        activityBlockedBinding.timerText
                    )
                    if (unBlockAppStatus) {
                        unBlockApp(appName)
                    }

                }
                appPackage = it.appPackage!!
                Timber.d("Unblock App = $unBlockAppStatus")
            }
        }

    }

    private fun unBlockApp(appName: String) {
        remainingTimeViewModel.deleteRemainingTime(appName)
            .observe(this) {
                Toast.makeText(this, "$appName is Free now!", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
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