package com.khadka.limitr.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.MainThread
import com.khadka.limitr.blocking.EssentialApps
import com.khadka.limitr.ui.home.model.App
import com.khadka.limitr.usage.AppUsageRanking
import com.khadka.limitr.usage.AppUsageReader
import com.khadka.limitr.utils.Permissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppListState(val rows: List<App>? = null, val failed: Boolean = false)

/** Process-local cache. Finite background reads retain only application context, never a screen. */
@Singleton
class AppListRepository internal constructor(
    private val load: suspend () -> List<App>,
    private val usageAllowed: () -> Boolean,
    private val scope: CoroutineScope
) {
    @Inject constructor(@ApplicationContext context: Context) : this(
        { loadApps(context) }, { Permissions.isUsageStateManagerEnabled(context) },
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    )

    private val mutable = MutableStateFlow(AppListState())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    @MainThread fun preload() { if (mutable.value.rows == null) refresh() }

    @MainThread fun refresh() {
        // Never briefly display cached usage after access is revoked in Settings.
        if (!usageAllowed()) mutable.value = mutable.value.copy(rows = mutable.value.rows?.let { AppUsageRanking.rows(it, null) })
        if (job?.isActive == true) return
        mutable.value = mutable.value.copy(failed = false)
        job = scope.launch {
            try {
                val rows = load()
                mutable.value = AppListState(if (usageAllowed()) rows else AppUsageRanking.rows(rows, null))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(failed = true) }
        }
    }

    companion object {
        private suspend fun loadApps(context: Context): List<App> = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val essential = EssentialApps.packages(context)
            val installed = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            val apps = installed.distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName !in essential }.mapNotNull { info ->
                    ensureActive()
                    try {
                        val app = pm.getApplicationInfo(info.activityInfo.packageName, 0)
                        App(app.loadIcon(pm), app.loadLabel(pm).toString(), app.packageName)
                    } catch (_: PackageManager.NameNotFoundException) { null }
                }
            AppUsageRanking.rows(apps, AppUsageReader.last24Hours(context))
        }
    }
}
