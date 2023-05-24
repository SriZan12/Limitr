package com.example.limitr.ui.blocker.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.limitr.R
import com.example.limitr.data.room.appdatabase.model.LimitrEntities
import com.example.limitr.databinding.ActivityBlockedBinding
import com.example.limitr.ui.blocker.vm.RemainingTimeViewModel
import com.example.limitr.utils.DateAndTime.getIntervalForBlocking
import com.example.limitr.utils.DateAndTime.getRemainingTime
import com.example.limitr.utils.DateAndTime.getTimer
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.NotificationUtils
import com.example.limitr.utils.NotificationUtils.cancelNotification
import com.example.limitr.utils.ViewUtils
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName
import com.example.limitr.utils.ViewUtils.showToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

const val twoHoursValue = 7200000

@AndroidEntryPoint
class ActivityBlocked : AppCompatActivity() {
    private lateinit var appName: String
    private lateinit var appPackage: String
    private lateinit var activityBlockedBinding: ActivityBlockedBinding
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()
    private var unBlockAppStatus: Boolean = false
    private var appIcon: Drawable? = null

    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var editor: SharedPreferences.Editor


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBlockedBinding = DataBindingUtil.setContentView(this, R.layout.activity_blocked)
        setContentView(activityBlockedBinding.root)

        appName = intent.getStringExtra("appName").toString()
        appPackage = intent.getStringExtra("appPackage").toString()
        appIcon = getAppIconByPackageName(this, appPackage)


        loadProfilePhoto(activityBlockedBinding.profile, this)

        activityBlockedBinding.appIcon.setImageDrawable(
            getAppIconByPackageName(
                this,
                appPackage
            )
        )

        activityBlockedBinding.unBlockApp.setOnClickListener {
            unBlockAppByCrypto()
        }

        activityBlockedBinding.appName.text = appName

        remainingTimeViewModel.getRemainingTime(appName).observe(this) {

            if (it != null) {
                if (it.starTime != null && it.endTime != null && it.isAppBlockedOrLimited == getString(
                        R.string.blocked
                    )
                ) {
                    activityBlockedBinding.intervalText.isVisible = true
                    unBlockAppStatus = getIntervalForBlocking(
                        it.starTime,
                        it.endTime,
                        it.remainingTime,
                        activityBlockedBinding.timerText,
                        activityBlockedBinding.intervalText
                    )
                    Timber.d("UnblockStatus = $unBlockAppStatus")
                    if (unBlockAppStatus) {
                        unBlockApp(appName)
                    }
                } else if (it.starTime != null && it.endTime != null && it.isAppBlockedOrLimited == getString(
                        R.string.limited
                    )
                ) {
                    getTimer(
                        it.blockedTime!!,
                        it.remainingTime,
                        timerText = activityBlockedBinding.timerText
                    )
                    val getRemainingTime = getRemainingTime(it.blockedTime, it.remainingTime)
                    Timber.d("remainingTime = ${getRemainingTime}")
                    if (getRemainingTime!! < 0L) {
                        blockLimitedApp(it.appName, it.appPackage!!)
                    }
                } else {
                    activityBlockedBinding.intervalText.isVisible = false
                    unBlockAppStatus = getTimer(
                        it.blockedTime,
                        it.remainingTime,
                        activityBlockedBinding.timerText
                    )
                    Timber.d("UnblockStatus = $unBlockAppStatus")

                    if (unBlockAppStatus) {
                        unBlockApp(appName)
                    }

                }
            }
        }

    }

    private fun unBlockApp(appName: String) {
        remainingTimeViewModel.deleteRemainingTime(appName)
            .observe(this) {
                showToast(this@ActivityBlocked, "$appName is free now!")
                finishAffinity()
            }
    }

    private fun blockLimitedApp(appName: String, appPackage: String) {
        val blockedStatus = getString(R.string.blocked)
        val limitrEntities = LimitrEntities(
            appName,
            System.currentTimeMillis(),
            twoHoursValue.toLong(),
            appPackage,
            null,
            null,
            false,
            blockedStatus
        )
        remainingTimeViewModel.insertRemainingTime(limitrEntities).observe(this) {
            NotificationUtils.startNotification(
                this,
                appName,
                System.currentTimeMillis(),
                twoHoursValue.toLong(),
                appIcon!!.toBitmap()
            )
            NotificationUtils.endNotification(
                this,
                appName,
                System.currentTimeMillis() + twoHoursValue.toLong(),
                appIcon!!.toBitmap()
            )

            ViewUtils.startTimer(activityBlockedBinding.timerText, twoHoursValue.toLong())
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

    private fun unBlockAppByCrypto() {
        val cryptoDialog = Dialog(this@ActivityBlocked)

        cryptoDialog.apply {
            window?.setContentView(R.layout.unblock_app_layout)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }.show()

        val unBlockButton: Button = cryptoDialog.findViewById(R.id.unBlockApp)

        unBlockButton.setOnClickListener {
            val crypto = ViewUtils.getCrypto(sharedPref, this@ActivityBlocked)
            if (crypto >= 2) {
                val deductCrypto = crypto - 2
                editor.putInt(getString(R.string.daily_Login_Reward), deductCrypto)
                editor.apply()
                unBlockApp(appName)
                cancelNotification(this@ActivityBlocked, appName)
                cryptoDialog.dismiss()
            } else {
                showToast(this@ActivityBlocked, "Not enough Crypto")
                cryptoDialog.dismiss()
            }
        }
    }
}