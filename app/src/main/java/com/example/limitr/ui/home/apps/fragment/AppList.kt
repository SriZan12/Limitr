package com.example.limitr.ui.home.apps.fragment

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.limitr.R
import com.example.limitr.databinding.ApplistLayoutBinding
import com.example.limitr.utils.OnAppClickListener
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.apps.adapter.AppListAdapter
import com.example.limitr.ui.home.model.App
import com.example.limitr.ui.home.model.AppInfoModel
import com.example.limitr.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class AppList : Fragment(R.layout.applist_layout) {

    private lateinit var binding: ApplistLayoutBinding

    @Inject
    lateinit var appListAdapter: AppListAdapter
    private lateinit var appInfo: AppInfoModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.applist_layout, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if (Settings.canDrawOverlays(requireContext()) ||
            checkAccessibilityPermission() || isUsageStateManagerEnabled()
        ) {
            loadStatistics()
        }
    }

    private fun checkAccessibilityPermission(): Boolean {
        var isAccessibilityEnabled = false
        (requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).apply {
            installedAccessibilityServiceList.forEach { installedService ->
                installedService.resolveInfo.serviceInfo.apply {
                    if (getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { it.resolveInfo.serviceInfo.packageName == packageName && it.resolveInfo.serviceInfo.name == name && permission == Manifest.permission.BIND_ACCESSIBILITY_SERVICE && it.resolveInfo.serviceInfo.packageName == requireActivity().packageName })
                        isAccessibilityEnabled = true
                }
            }
        }
        return isAccessibilityEnabled
    }

    private fun isUsageStateManagerEnabled(): Boolean {
        val appOpsManager =
            requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private val onclickListener: OnAppClickListener = object : OnAppClickListener {
        override fun onClick(
            appPackageName: String
        ) {
            val intent = Intent(requireContext(), BlockAppActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(requireContext().getString(R.string.packageName), appPackageName)
            startActivity(intent)
        }

    }


    private fun showAppsUsage(mySortedMap: Map<String?, UsageStats>) {
        val appsList = ArrayList<App?>()
        val usageStatsList: List<UsageStats> = ArrayList(mySortedMap.values)

//         sort the applications by time spent in foreground
        Collections.sort(
            usageStatsList
        ) { z1: UsageStats, z2: UsageStats ->
            z1.totalTimeInForeground.compareTo(z2.totalTimeInForeground)
        }

        // get total time of apps usage to calculate the usagePercentage for each app
        var totalTime = 0L
        for (usageStats in usageStatsList) {
            totalTime += usageStats.totalTimeInForeground
        }
        //fill the appsList
        for (usageStats in usageStatsList) {
            try {
                val packageName = usageStats.packageName
                Timber.d("packageName = $packageName")
                val icon: Drawable? =
                    ViewUtils.getAppIconByPackageName(requireContext(), packageName)
                val appName: String =
                    ViewUtils.getAppNameByPackageName(requireContext(), packageName)

                Timber.d("appName = $appName")


                val usageDuration: String = getDurationBreakdown(usageStats.totalTimeInForeground)
                val usagePercentage = (usageStats.totalTimeInForeground * 100 / totalTime).toInt()
                val usageStatDTO = App(icon, appName, packageName, usagePercentage, usageDuration)
                appsList.add(usageStatDTO)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }


        // reverse the list to get most usage first
        appsList.reverse()
        appListAdapter.setAppLists(appsList, requireContext(), onclickListener)
        binding.appListRecycler.adapter = appListAdapter

    }

    /* @param millis (application time in foreground)
     * @return string in format hh:mm:ss from milliseconds
     */
    private fun getDurationBreakdown(millis: Long): String {
        var timeInMillis = millis
        require(timeInMillis >= 0) { "Duration must be greater than zero!" }
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        timeInMillis -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        timeInMillis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis)
        return "$hours h $minutes m $seconds s"
    }

    /**
     * load the usage stats for last 24h
     */
    private fun loadStatistics() {
        val usageStateManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var appList = usageStateManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 3600 * 24,
            System.currentTimeMillis()
        )
        appList = appList.filter { app -> app.totalTimeInForeground > 0 }
            .toList() // filtering the app which has been used

        // Group the usageStats by application and sort them by total time in foreground
        if (appList.size > 0) {
            val mySortedMap: MutableMap<String?, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.packageName] = usageStats
            }
            showAppsUsage(mySortedMap)
        }
    }

}