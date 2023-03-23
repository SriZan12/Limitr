package com.example.limitr.ui.blocker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.limitr.R
import com.example.limitr.databinding.ActivityBlockedBinding
import com.example.limitr.ui.home.model.AppInfoModel
import com.example.limitr.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityBlocked : AppCompatActivity() {
    private lateinit var appName: String
    private lateinit var appPackage: String
    private lateinit var activityBlockedBinding: ActivityBlockedBinding
    private lateinit var appInfoModel: AppInfoModel
    private val appBlock = "appBlocker"
    private lateinit var appIcon: Bitmap
    private val remainingTimeViewModel: RemainingTimeViewModel by viewModels()


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
                val elapsedTime = System.currentTimeMillis() - it.startTime!!
                val currentRemainingTime = it.remainingTime?.minus(elapsedTime)

                if (currentRemainingTime != null && currentRemainingTime > 0) {
                    ViewUtils.startTimer(activityBlockedBinding.timerText, currentRemainingTime)
                } else if (currentRemainingTime != null && currentRemainingTime <= 0) {
                    remainingTimeViewModel.deleteRemainingTime(appName).observe(this) {
                        finishAffinity()
                        Toast.makeText(this, "$appName is Free now!", Toast.LENGTH_SHORT).show()
                    }
                }
                appPackage = it.appPackage!!

            }

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