package com.example.limitr.ui.home

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Dialog
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.limitr.R
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.ui.home.model.App
import com.example.limitr.ui.home.model.AppInfoModel
import com.example.limitr.utils.ViewUtils.loadProfilePhoto
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class FragmentHome : Fragment(R.layout.fragment_home) {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private val homeFragment = "home"
    private lateinit var dialog: Dialog

    @Inject
    lateinit var appListAdapter: AppListAdapter
    private lateinit var appInfo: AppInfoModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        return fragmentHomeBinding.root

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dialog = Dialog(requireContext())
    }

    override fun onResume() {
        super.onResume()

        if (!Settings.canDrawOverlays(requireContext()) ||
            !checkAccessibilityPermission() || !isUsageStateManagerEnabled()
        ) {
            showPermissionDialog()
        } else {
            dialog.dismiss()
        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfilePhoto(fragmentHomeBinding.profile, requireContext())
        loadStatistics()

        fragmentHomeBinding.profile.setOnClickListener {
            val action = FragmentHomeDirections.actionFragmentHomeToFragmentEditProfile()
            findNavController().navigate(action)
        }

    }

    private fun goToDisplayOverOtherAppsSettings() {
        val packageName = requireContext().packageName
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun goToUsageStateManagerSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun goToAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPermissionDialog() {

        dialog.apply {
            window?.setContentView(R.layout.permission_layout)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(false)
        }

        val grantAccessiblePermission: Button = dialog.findViewById(R.id.grantAccessiblePerm)
        val grantDisplayOverPermission: Button = dialog.findViewById(R.id.grantDisplayOverPerm)
        val grantUsageState: Button = dialog.findViewById(R.id.grantUsageStateManager)

        if (checkAccessibilityPermission()) {
            grantAccessiblePermission.text = "Granted"
            grantAccessiblePermission.isEnabled = false
        }
        if (Settings.canDrawOverlays(requireContext())) {
            grantDisplayOverPermission.text = "Granted"
            grantDisplayOverPermission.isEnabled = false
        }

        if (isUsageStateManagerEnabled()) {
            grantUsageState.text = "Granted"
            grantUsageState.isEnabled = false
        }

        grantAccessiblePermission.setOnClickListener {
            goToAccessibilitySettings()
        }

        grantDisplayOverPermission.setOnClickListener {
            goToDisplayOverOtherAppsSettings()
        }

        grantUsageState.setOnClickListener {
            goToUsageStateManagerSettings()
        }

        dialog.show()
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
            appName: String,
            appIcon: Drawable,
            appPackageName: String
        ) {

            appInfo = AppInfoModel(appName, appIcon, appPackageName)

            val action = FragmentHomeDirections.actionFragmentHomeToFragmentBlockApp(appInfo)
            findNavController().navigate(action)
        }

    }

    private fun showAppsUsage(mySortedMap: Map<String?, UsageStats>) {
        val appsList = ArrayList<App?>()
        val usageStatsList: List<UsageStats> = ArrayList(mySortedMap.values)

        // sort the applications by time spent in foreground
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
        Timber.d("TotalTime = $totalTime")
        //fill the appsList
        for (usageStats in usageStatsList) {
            try {
                val packageName = usageStats.packageName
                var icon: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.logo)
                val packageNames = packageName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                var appName = packageNames[packageNames.size - 1].trim { it <= ' ' }
                if (isAppInfoAvailable(usageStats)) {
                    val ai: ApplicationInfo = requireContext().packageManager
                        .getApplicationInfo(packageName, 0)
                    icon = requireContext().packageManager.getApplicationIcon(ai)
                    appName = requireContext().packageManager.getApplicationLabel(ai)
                        .toString()
                }
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
        fragmentHomeBinding.appListRecyclerView.adapter = appListAdapter

    }

    /**
     * check if the application info is still existing in the device / otherwise it's not possible to show app detail
     * @return true if application info is available
     */
    private fun isAppInfoAvailable(usageStats: UsageStats): Boolean {
        return try {
            requireContext().packageManager
                .getApplicationInfo(usageStats.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * helper method to get string in format hh:mm:ss from miliseconds
     *
     * @param millis (application time in foreground)
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

    fun handleBackPressed(): Boolean {
        return true
    }

}
