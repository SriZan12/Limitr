package com.example.limitr.ui.home.apps.fragment

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.example.limitr.R
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.model.App
import com.example.limitr.utils.Permissions.isAccessibilityEnabled
import com.example.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.example.limitr.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Collections
import java.util.TreeMap
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AppList : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AppListScreen(
                        onAppSelected = { appPackageName ->
                            val intent = Intent(requireContext(), BlockAppActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra(requireContext().getString(R.string.packageName), appPackageName)
                            }
                            startActivity(intent)
                        },
                        loadApps = { loadStatistics() }
                    )
                }
            }
        }
    }

    @Composable
    private fun AppListScreen(
        onAppSelected: (String) -> Unit,
        loadApps: () -> List<App>
    ) {
        var apps by remember { mutableStateOf(emptyList<App>()) }

        LaunchedEffect(Unit) {
            if (
                Settings.canDrawOverlays(requireContext()) &&
                requireContext().isAccessibilityEnabled() &&
                isUsageStateManagerEnabled(requireContext = requireContext())
            ) {
                apps = loadApps()
            }
        }

        if (apps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = getString(R.string.no_apps_blocked))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(apps) { app ->
                    AppRow(app = app, onClick = {
                        app.appPackageName?.let(onAppSelected)
                    })
                    Divider()
                }
            }
        }
    }

    @Composable
    private fun AppRow(app: App, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidIcon(icon = app.appIcon)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = app.appName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(text = app.usageDuration.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Text(text = "${app.usagePercentage}%", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun AndroidIcon(icon: Drawable?) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(96, 96)
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(icon)
            },
            modifier = Modifier.height(40.dp)
        )
    }

    private fun showAppsUsage(mySortedMap: Map<String?, UsageStats>): List<App> {
        val appsList = ArrayList<App>()
        val usageStatsList: List<UsageStats> = ArrayList(mySortedMap.values)

        Collections.sort(usageStatsList) { z1: UsageStats, z2: UsageStats ->
            z1.totalTimeInForeground.compareTo(z2.totalTimeInForeground)
        }

        var totalTime = 0L
        for (usageStats in usageStatsList) {
            totalTime += usageStats.totalTimeInForeground
        }

        for (usageStats in usageStatsList) {
            try {
                val packageName = usageStats.packageName
                Timber.d("packageName = $packageName")
                val icon: Drawable? = ViewUtils.getAppIconByPackageName(requireContext(), packageName)
                val appName: String = ViewUtils.getAppNameByPackageName(requireContext(), packageName)

                if (appName.trim() != context?.getString(R.string.app_name)) {
                    val usageDuration: String = getDurationBreakdown(usageStats.totalTimeInForeground)
                    val usagePercentage = if (totalTime == 0L) 0 else (usageStats.totalTimeInForeground * 100 / totalTime).toInt()
                    val usageStatDTO = App(icon, appName, packageName, usagePercentage, usageDuration)
                    appsList.add(usageStatDTO)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e)
            }
        }

        return appsList.reversed()
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

    private fun loadStatistics(): List<App> {
        val usageStateManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var appList = usageStateManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 3600 * 24,
            System.currentTimeMillis()
        )
        appList = appList.filter { app -> app.totalTimeInForeground > 0 }.toList()

        if (appList.isNotEmpty()) {
            val mySortedMap: MutableMap<String?, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.packageName] = usageStats
            }
            return showAppsUsage(mySortedMap)
        }
        return emptyList()
    }
}
