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
    private lateinit var systemLauncherPackage: String
    private lateinit var lifecycleRegistry: LifecycleRegistry

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.markState(Lifecycle.State.CREATED)

        systemLauncherPackage = getDefaultLauncherPackage()
        Timber.d("System = $systemLauncherPackage")
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

    private fun launchBlockingActivity() {
        val blockedIntent = Intent(this, ActivityBlocked::class.java)
        blockedIntent.flags = FLAG_ACTIVITY_NEW_TASK
        startActivity(blockedIntent)
    }

    private fun checkApp(appName: String) {
        Timber.d("AppName Inside Function = $appName")
        lifecycleScope.launch(Dispatchers.IO) {
            val exist = limitrDao.getAppName(appName)
            withContext(Dispatchers.Main) {
                Timber.d("returned appName = ${exist?.appName}")
                if (appName == exist?.appName) {
                    launchBlockingActivity()
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        }
    }


    private fun getDefaultLauncherPackage(): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return resolveInfo!!.activityInfo.packageName
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
    }


    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }


}
