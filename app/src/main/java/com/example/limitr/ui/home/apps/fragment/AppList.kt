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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.example.limitr.R
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.model.App
import com.example.limitr.ui.theme.Danger
import com.example.limitr.ui.theme.Normal
import com.example.limitr.ui.theme.UiColor
import com.example.limitr.ui.theme.Warning
import com.example.limitr.utils.Permissions.isAccessibilityEnabled
import com.example.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.example.limitr.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.TreeMap
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AppList : Fragment() {

    private var apps by mutableStateOf(listOf<App>())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppListScreen(
                    apps = apps,
                    onClick = { appPackageName ->
                        val intent = Intent(requireContext(), BlockAppActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra(requireContext().getString(R.string.packageName), appPackageName)
                        startActivity(intent)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (Settings.canDrawOverlays(requireContext()) ||
            requireContext().isAccessibilityEnabled() ||
            isUsageStateManagerEnabled(requireContext = requireContext())
        ) {
            loadStatistics()
        }
    }

    private fun loadStatistics() {
        val usageStateManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var appList = usageStateManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 3600 * 24,
            System.currentTimeMillis(),
        )
        appList = appList.filter { app -> app.totalTimeInForeground > 0 }.toList()

        if (appList.isNotEmpty()) {
            val mySortedMap: MutableMap<String?, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.packageName] = usageStats
            }
            showAppsUsage(mySortedMap)
        } else {
            apps = emptyList()
        }
    }

    private fun showAppsUsage(mySortedMap: Map<String?, UsageStats>) {
        val appsList = ArrayList<App>()
        val usageStatsList: List<UsageStats> = ArrayList(mySortedMap.values)
        val sorted = usageStatsList.sortedBy { it.totalTimeInForeground }

        var totalTime = 0L
        for (usageStats in sorted) {
            totalTime += usageStats.totalTimeInForeground
        }

        for (usageStats in sorted) {
            try {
                val packageName = usageStats.packageName
                val icon: Drawable? =
                    ViewUtils.getAppIconByPackageName(requireContext(), packageName)
                val appName: String =
                    ViewUtils.getAppNameByPackageName(requireContext(), packageName)

                if (appName.trim() != context?.getString(R.string.app_name)) {
                    val usageDuration: String = getDurationBreakdown(usageStats.totalTimeInForeground)
                    val usagePercentage =
                        (usageStats.totalTimeInForeground * 100 / totalTime).toInt()
                    appsList.add(App(icon, appName, packageName, usagePercentage, usageDuration))
                }
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }

        apps = appsList.reversed()
    }

    private fun getDurationBreakdown(millis: Long): String {
        var timeInMillis = millis
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        timeInMillis -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        timeInMillis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis)
        return "$hours h $minutes m $seconds s"
    }
}

@Composable
private fun AppListScreen(
    apps: List<App>,
    onClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UiColor)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        items(items = apps, key = { it.appPackageName }) { app ->
            AppUsageItem(
                app = app,
                onClick = { onClick(app.appPackageName) },
            )
        }
    }
}

@Composable
private fun AppUsageItem(
    app: App,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        app.appIcon?.let {
            val appIconBitmap = remember(it) { it.toBitmap().asImageBitmap() }
            Image(
                bitmap = appIconBitmap,
                contentDescription = app.appName,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = app.appName, color = Color.White)
                Text(text = app.usageDuration, color = Color.White)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = app.usagePercentage / 100f,
                    modifier = Modifier.weight(1f),
                    color = when {
                        app.usagePercentage < 50 -> Normal
                        app.usagePercentage in 50..79 -> Warning
                        else -> Danger
                    },
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(text = "${app.usagePercentage}%", color = Color.White)
            }
        }
    }
}
