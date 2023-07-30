package com.example.limitr.services.blockerServices

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.limitr.R
import com.example.limitr.utils.ViewUtils.getAppNameByPackageName
import timber.log.Timber


class AppLaunchDetector : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val launchedAppPackage = event.packageName as String
            val appName = getAppNameByPackageName(context = this, packageName = launchedAppPackage)

            try {
                sendInterceptIntent(
                    context = this,
                    appName = appName,
                    appPackage = launchedAppPackage
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }

    }


    override fun onServiceConnected() {

        val info = AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
        }

        this.serviceInfo = info
    }

    override fun onInterrupt() {
        Timber.d("Some Sort of Interruption!")
    }


    private fun sendInterceptIntent(context: Context, appName: String, appPackage: String?) {
        Timber.d("INSIDE INTERCEPTION FUNCTION")
        val intent = Intent(this, AppFoundReceiver::class.java)
        intent.putExtra(this.getString(R.string.appName), appName)
        intent.putExtra(this.getString(R.string.packageName), appPackage)
        context.sendBroadcast(intent)
    }

}
