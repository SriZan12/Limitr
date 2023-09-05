package com.khadka.limitr.ui.blocker.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.khadka.limitr.R
import com.khadka.limitr.databinding.ActivityBlockedBinding
import com.khadka.limitr.ui.blocker.vm.BlockedAppVM
import com.khadka.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.khadka.limitr.ui.mainactivity.MainActivity
import com.khadka.limitr.utils.Constants.OVERLAY_DISPLAYED
import com.khadka.limitr.utils.Constants.REQUIRED_CRYPTO_FOR_UNBLOCK
import com.khadka.limitr.utils.DateAndTime.getIntervalForBlocking
import com.khadka.limitr.utils.DateAndTime.getTimer
import com.khadka.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.khadka.limitr.utils.NotificationUtils.cancelNotification
import com.khadka.limitr.utils.OverlayScreen
import com.khadka.limitr.utils.Permissions
import com.khadka.limitr.utils.ViewUtils.getAppIconByPackageName
import com.khadka.limitr.utils.ViewUtils.showToast
import com.khadka.limitr.utils.dialogShow
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
    private val blockedAppVM: BlockedAppVM by viewModels()
    private var unBlockAppStatus: Boolean = false
    private var appIcon: Drawable? = null
    private lateinit var dialog: Dialog
    private val mainViewModel: MainFragmentViewModel by viewModels()

    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var editor: SharedPreferences.Editor

    @Inject
    lateinit var overlayScreen: OverlayScreen



    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBlockedBinding = DataBindingUtil.setContentView(this, R.layout.activity_blocked)
        setContentView(activityBlockedBinding.root)

        dialog =
            dialogShow(
                this@ActivityBlocked,
                R.layout.notification_dialog
            )

        appName = intent.getStringExtra(this.getString(R.string.appName)).toString()
        appPackage = intent.getStringExtra(this.getString(R.string.packageName)).toString()
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

        activityBlockedBinding.blockNotification.setOnClickListener {

            if (!Permissions.isNotificationServiceEnable(this@ActivityBlocked)) {
                showNotificationDialog()
            } else {

                if (activityBlockedBinding.blockNotification.isChecked) {
                    setNotificationStatus(true)

                } else if (!activityBlockedBinding.blockNotification.isChecked) {
                    setNotificationStatus(false)
                }
            }
        }

    }

    private fun setView() {


        loadProfilePhoto(activityBlockedBinding.profile, this)

        activityBlockedBinding.appName.text = appName

        lifecycleScope.launch(Dispatchers.Main) {
            activityBlockedBinding.totalCrypto.text = mainViewModel.getCrypto().first().toString()
        }

        blockedAppVM.getRemainingTime(appName).observe(this) {

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
        blockedAppVM.deleteRemainingTime(appName)
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

    private fun setNotificationStatus(status: Boolean) {
        editor.putBoolean(appName, status)
        editor.apply()
        activityBlockedBinding.blockNotification.isChecked = status
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()

        OVERLAY_DISPLAYED = false

        val intent = Intent(this@ActivityBlocked, MainActivity::class.java)
        intent.flags.apply {
            Intent.FLAG_ACTIVITY_CLEAR_TASK
            Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        launcher.launch(intent)
        finish()

    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        val cancel: ImageView = cryptoDialog.findViewById(R.id.cancel)

        unBlockButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val crypto = mainViewModel.getCrypto().first()
                if (crypto >= REQUIRED_CRYPTO_FOR_UNBLOCK) {
                    val deductCrypto = crypto - REQUIRED_CRYPTO_FOR_UNBLOCK
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

        cancel.setOnClickListener {
            cryptoDialog.dismiss()
        }
    }

    private fun showNotificationDialog() {

        val grantPermission: TextView = dialog.findViewById(R.id.grantPermission)
        val cancel: ImageView = dialog.findViewById(R.id.cancel)

        grantPermission.setOnClickListener {
            gotoSettings()
        }

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun gotoSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.apply {
            Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        OVERLAY_DISPLAYED = false
    }
}