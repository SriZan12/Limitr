package com.example.limitr.services.blockerServices

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.*
import com.example.limitr.data.room.LimitrDao
import com.example.limitr.ui.blocker.ActivityBlocked
import com.example.limitr.utils.ViewUtils.startTimer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class AccessibilityService : AccessibilityService(), LifecycleOwner {
    @Inject
    lateinit var limitrDao: LimitrDao
    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.markState(Lifecycle.State.CREATED)

    }

    override fun onInterrupt() {
        Timber.d("Some Sort of Interruption!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            try {
                val applicationInfo =
                    packageManager.getApplicationInfo(event.packageName.toString(), 0)
                val appName = packageManager.getApplicationLabel(applicationInfo) as String
//                accessLiveData(appName)
                checkApp(appName)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
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

    private fun launchBlockingActivity(appName: String, appPackage: String?) {
        val blockedIntent = Intent(this, ActivityBlocked::class.java)
        blockedIntent.flags = FLAG_ACTIVITY_NEW_TASK
        blockedIntent.putExtra("appName", appName)
        blockedIntent.putExtra("appPackage", appPackage)
        startActivity(blockedIntent)
    }

    private fun checkApp(appName: String) {
        lifecycleScope.launch {
            with(this@AccessibilityService) {
                val currentTime = System.currentTimeMillis()
                var timer = 0L
                limitrDao.getRemainingTime(appName).observeForever {
                    if (it != null) {
                        val getAppName = it.appName
                        if (getAppName == appName &&
                            currentTime >= it.starTime!! &&
                            currentTime <= it.endTime!!
                        ) {
                            launchBlockingActivity(appName, it.appPackage)
                        }
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
    }


    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }


}
