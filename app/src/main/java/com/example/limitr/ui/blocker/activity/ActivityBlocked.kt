package com.example.limitr.ui.blocker.activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.limitr.R
import com.example.limitr.databinding.ActivityBlockedBinding
import com.example.limitr.ui.blocker.vm.RemainingTimeViewModel
import com.example.limitr.utils.DateAndTime.getIntervalForBlocking
import com.example.limitr.utils.DateAndTime.getTimer
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName
import com.example.limitr.utils.ViewUtils.showToast
import dagger.hilt.android.AndroidEntryPoint

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

        loadProfilePhoto(activityBlockedBinding.profile, this)

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
                        activityBlockedBinding.timerText
                    )
                    if (unBlockAppStatus) {
                        unBlockApp(appName)
                    }

                }
                appPackage = it.appPackage!!
            }
        }

    }

    private fun unBlockApp(appName: String) {
        remainingTimeViewModel.deleteRemainingTime(appName)
            .observe(this) {
                showToast(this@ActivityBlocked,"$appName is free now!")
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