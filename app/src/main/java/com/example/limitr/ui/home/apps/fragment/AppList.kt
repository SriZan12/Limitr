package com.example.limitr.ui.home.apps.fragment

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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.limitr.R
import com.example.limitr.databinding.ApplistLayoutBinding
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.apps.adapter.AppListAdapter
import com.example.limitr.ui.home.model.App
import com.example.limitr.utils.OnAppClickListener
import com.example.limitr.utils.Permissions.isAccessibilityEnabled
import com.example.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.example.limitr.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Collections
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class AppList : Fragment(R.layout.applist_layout) {

    private lateinit var binding: ApplistLayoutBinding

    @Inject
    lateinit var appListAdapter: AppListAdapter


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
            requireContext().isAccessibilityEnabled() || isUsageStateManagerEnabled(requireContext = requireContext())
        ) {
            loadStatistics()
        }
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

                if (appName.trim() != context?.getString(R.string.app_name)) {
                    val usageDuration: String =
                        getDurationBreakdown(usageStats.totalTimeInForeground)
                    val usagePercentage =
                        (usageStats.totalTimeInForeground * 100 / totalTime).toInt()
                    val usageStatDTO =
                        App(icon, appName, packageName, usagePercentage, usageDuration)
                    appsList.add(usageStatDTO)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        appsList.reverse()
        appListAdapter.setAppLists(appsList, requireContext(), onclickListener)
        binding.appListRecycler.adapter = appListAdapter

    }


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

        if (appList.size > 0) {
            val mySortedMap: MutableMap<String?, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.packageName] = usageStats
            }
            showAppsUsage(mySortedMap)
        }
    }

}