package com.khadka.limitr.ui.home.apps.fragment

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.khadka.limitr.R
import com.khadka.limitr.blocking.EssentialApps
import com.khadka.limitr.databinding.ApplistLayoutBinding
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import com.khadka.limitr.ui.home.apps.adapter.AppListAdapter
import com.khadka.limitr.ui.home.model.App
import com.khadka.limitr.utils.OnAppClickListener
import com.khadka.limitr.utils.Permissions
import kotlinx.coroutines.*

class AppList : Fragment(R.layout.applist_layout) {
    private var binding: ApplistLayoutBinding? = null
    private var job: Job? = null
    override fun onViewCreated(view: View, state: Bundle?) { binding = ApplistLayoutBinding.bind(view) }
    override fun onResume() {
        super.onResume()
        job?.cancel()
        val context = requireContext().applicationContext
        job = viewLifecycleOwner.lifecycleScope.launch {
            binding?.appLoadStatus?.text = "Loading installed apps…"
            binding?.appLoadStatus?.isVisible = true
            val rows = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val essential = EssentialApps.packages(context)
                val usage = if (Permissions.isUsageStateManagerEnabled(context)) {
                    val now = System.currentTimeMillis()
                    try { context.getSystemService(UsageStatsManager::class.java).queryAndAggregateUsageStats(now - 86_400_000, now).orEmpty() }
                    catch (_: SecurityException) { emptyMap() }
                } else emptyMap()
                val total = usage.values.sumOf { it.totalTimeInForeground }.coerceAtLeast(1)
                val installed = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                ArrayList<App?>(installed.distinctBy { it.activityInfo.packageName }.filter { it.activityInfo.packageName !in essential }.mapNotNull { info ->
                    ensureActive()
                    val pkg = info.activityInfo.packageName
                    try {
                        val app = pm.getApplicationInfo(pkg, 0)
                        val millis = usage[pkg]?.totalTimeInForeground ?: 0
                        App(app.loadIcon(pm), app.loadLabel(pm).toString(), pkg,
                            (millis * 100 / total).toInt().coerceIn(0, 100),
                            if (usage.isEmpty()) "—" else "${millis / 3_600_000} h ${millis / 60_000 % 60} m")
                    } catch (_: PackageManager.NameNotFoundException) { null }
                }.sortedBy { it.appName.orEmpty().lowercase() })
            }
            val ui = binding ?: return@launch
            ui.appLoadStatus.isVisible = rows.isEmpty()
            ui.appLoadStatus.text = "No launchable apps available."
            ui.appListRecycler.adapter = AppListAdapter().apply {
                setAppLists(rows, context, object : OnAppClickListener {
                    override fun onClick(appPackageName: String) {
                        startActivity(Intent(requireContext(), BlockAppActivity::class.java).putExtra("packageName", appPackageName))
                    }
                })
            }
        }
    }
    override fun onDestroyView() { job?.cancel(); binding?.appListRecycler?.adapter = null; binding = null; super.onDestroyView() }
}
