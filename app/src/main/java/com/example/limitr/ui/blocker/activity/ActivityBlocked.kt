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
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.limitr.R
import com.example.limitr.databinding.ActivityBlockedBinding
import com.example.limitr.ui.blocker.vm.RemainingTimeViewModel
import com.example.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.example.limitr.ui.mainactivity.MainActivity
import com.example.limitr.utils.Constants.REQUIREDCRYPTOFORUNBLOCK
import com.example.limitr.utils.DateAndTime.getIntervalForBlocking
import com.example.limitr.utils.DateAndTime.getTimer
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.NotificationUtils.cancelNotification
import com.example.limitr.utils.ViewUtils
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName
import com.example.limitr.utils.ViewUtils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class ActivityBlocked : AppCompatActivity() {
    private lateinit var appName: String
    private lateinit var appPackage: String
    private lateinit var activityBlockedBinding: ActivityBlockedBinding
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()
    private var unBlockAppStatus: Boolean = false
    private var appIcon: Drawable? = null
    private val mainViewModel: MainFragmentViewModel by viewModels()

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

        setView()

        activityBlockedBinding.appIcon.setImageDrawable(
            getAppIconByPackageName(
                this,
                appPackage
            )
        )

        activityBlockedBinding.unBlockApp.setOnClickListener {
            unBlockAppByCrypto()
        }

    }

    private fun setView() {


        loadProfilePhoto(activityBlockedBinding.profile, this)

        activityBlockedBinding.appName.text = appName

        lifecycleScope.launch(Dispatchers.Main) {
            activityBlockedBinding.totalCrypto.text = mainViewModel.getCrypto().first().toString()
        }

        remainingTimeViewModel.getRemainingTime(appName).observe(this) {

            if (it != null) {

                activityBlockedBinding.unBlockApp.isEnabled = true

                if (it.starTime != null && it.endTime != null) {
                    activityBlockedBinding.timerText.isVisible = true

                    activityBlockedBinding.setTimerText.text = getString(R.string.duration)
                    activityBlockedBinding.setIntervalText.text = getString(R.string.blocked_for)

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
                } else {
                    activityBlockedBinding.setTimerText.text = getString(R.string.duration)
                    activityBlockedBinding.intervalText.isVisible = false
                    unBlockAppStatus = getTimer(
                        it.blockedTime,
                        it.remainingTime,
                        activityBlockedBinding.timerText
                    )

                }

                if (unBlockAppStatus) {
                    unBlockApp(appName)
                }

                val isNotificationOn = sharedPref.getBoolean(appName, false)

                if (isNotificationOn) {
                    activityBlockedBinding.blockNotification.isChecked = true
                }

                if (it.blockedTime!! <= 0) {
                    removeNotificationStatus()
                }
            }
        }
    }

    private fun unBlockApp(appName: String) {
        remainingTimeViewModel.deleteRemainingTime(appName)
            .observe(this) {
                showToast(this@ActivityBlocked, "$appName is free now!")
                removeNotificationStatus()
                finishAffinity()
            }
    }

    private fun removeNotificationStatus() {
        editor.remove(appName)
        editor.apply()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finishAffinity()
        finish()

        super.onBackPressed()
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
            lifecycleScope.launch(Dispatchers.Main) {
                val crypto = mainViewModel.getCrypto().first()
                if (crypto >= REQUIREDCRYPTOFORUNBLOCK) {
                    val deductCrypto = crypto - REQUIREDCRYPTOFORUNBLOCK
                    mainViewModel.upsertCrypto(deductCrypto)
                    unBlockApp(appName)
                    cancelNotification(this@ActivityBlocked, appName)
                    cryptoDialog.dismiss()
                } else {
                    showToast(this@ActivityBlocked, getString(R.string.not_enough_crypto))
                    cryptoDialog.dismiss()
                }
            }

        }
    }
}