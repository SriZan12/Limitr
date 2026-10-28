package com.khadka.limitr.ui.home.apps.fragment

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
import com.khadka.limitr.usage.AppUsageReader
import com.khadka.limitr.usage.AppUsageRanking
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
                val usage = AppUsageReader.last24Hours(context)
                val installed = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                val apps = installed.distinctBy { it.activityInfo.packageName }.filter { it.activityInfo.packageName !in essential }.mapNotNull { info ->
                    ensureActive()
                    val pkg = info.activityInfo.packageName
                    try {
                        val app = pm.getApplicationInfo(pkg, 0)
                        App(app.loadIcon(pm), app.loadLabel(pm).toString(), pkg)
                    } catch (_: PackageManager.NameNotFoundException) { null }
                }
                ArrayList<App?>(AppUsageRanking.rows(apps, usage))
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
    override fun onPause() { job?.cancel(); super.onPause() }
    override fun onDestroyView() { job?.cancel(); binding?.appListRecycler?.adapter = null; binding = null; super.onDestroyView() }
}
