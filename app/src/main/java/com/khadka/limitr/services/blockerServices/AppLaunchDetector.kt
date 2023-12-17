package com.khadka.limitr.services.blockerServices

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.khadka.limitr.R
import com.khadka.limitr.data.local.appdatabase.room.appblock.LimitrDao
import com.khadka.limitr.ui.blocker.activity.ActivityBlocked
import com.khadka.limitr.utils.Constants.OVERLAY_DISPLAYED
import com.khadka.limitr.utils.DateAndTime.getRemainingTime
import com.khadka.limitr.utils.DateAndTime.getTimer
import com.khadka.limitr.utils.OverlayScreen
import com.khadka.limitr.utils.ViewUtils.getAppNameByPackageName
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class AppLaunchDetector : AccessibilityService() {

    @Inject
    lateinit var overlayScreen: OverlayScreen

    @Inject
    lateinit var limitrDao: LimitrDao


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (!OVERLAY_DISPLAYED) {
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

                val launchedAppPackage = event.packageName as String
                val appName =
                    getAppNameByPackageName(context = this, packageName = launchedAppPackage)

                try {
                    checkApp(appName, launchedAppPackage, this)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        }

    }


    override fun onServiceConnected() {

        val info = AccessibilityServiceInfo()
        info.apply {
            eventTypes =
                AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
        }

        this.serviceInfo = info
    }

    override fun onInterrupt() {
        Timber.d("Some Sort of Interruption!")
    }


    private fun checkApp(appName: String, appPackage: String?, context: Context) {


        Timber.d("INSIDE CHECK APP")
        Timber.d("REMAINING TIME = ${limitrDao.getAppName(appName)?.remainingTime}")

        val currentTime = System.currentTimeMillis()
        val starTime = limitrDao.getAppName(appName)?.starTime
        val endTime = limitrDao.getAppName(appName)?.endTime

        Timber.d("Inside Interval")
        if (getAppName(appName) && starTime != null && endTime != null) {

            if (currentTime >= starTime && currentTime <= endTime) {

                showOverlayScreen(
                    appName = appName,
                    context = this@AppLaunchDetector,
                    appPackage = appPackage
                )
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

        } else if (getAppName(appName) && limitrDao.getAppName(appName)?.remainingTime != null) {

            val currentRemainingTime = getRemainingTime(
                limitrDao.getAppName(appName)!!.blockedTime,
                limitrDao.getAppName(appName)!!.remainingTime
            )

            Timber.d("CURRENT REMAINING TIME = $currentRemainingTime")

            if (currentRemainingTime != null) {
                if (currentRemainingTime > 0L) {

                    showOverlayScreen(
                        appName = appName,
                        context = this@AppLaunchDetector,
                        appPackage = appPackage
                    )

                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }

        }

    }

    private fun showOverlayScreen(appName: String, context: Context, appPackage: String?) {

        overlayScreen.showOverlayScreen(
            appName = appName,
            context = context,
            appPackage = appPackage,
            onButtonClicked = {

                launchBlockingActivity(
                    appName = appName,
                    appPackage = appPackage,
                    context = context
                )

                overlayScreen.removeOverlayView()

                OVERLAY_DISPLAYED = true

            },
            onExit = {
                overlayScreen.removeOverlayView()
                exitToHome()
            }

        )

        limitrDao.getRemainingTime(appName).observeForever {
            try {
                if (it.blockedTime != null && it.remainingTime != null) {
                    getTimer(it.blockedTime, it.remainingTime, overlayScreen.remainingTime)
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

        }
    }

    private fun launchBlockingActivity(appName: String, appPackage: String?, context: Context) {
        val blockedIntent = Intent(context, ActivityBlocked::class.java)
        blockedIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        blockedIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        blockedIntent.putExtra(context.getString(R.string.appName), appName)
        blockedIntent.putExtra(context.getString(R.string.packageName), appPackage)
        context.startActivity(blockedIntent)
    }

    private fun exitToHome() {
        OVERLAY_DISPLAYED = false
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        this.startActivity(intent)
    }

    private fun getAppName(appName: String): Boolean {
        return limitrDao.getAppName(appName)?.appName == appName
    }

}
