package com.khadka.limitr.usage

import com.khadka.limitr.ui.home.model.App
import java.util.Locale

object AppUsageRanking {
    fun rows(apps: List<App>, usage: Map<String, Long>?): List<App> {
        fun duration(app: App) = usage?.get(app.appPackageName)?.coerceIn(0, AppUsageReader.DAY_MILLIS) ?: 0L
        // The percentage describes the apps displayed in this list, excluding system/essential apps.
        val total = apps.sumOf { duration(it) }.toDouble()
        return apps.sortedWith(compareByDescending<App> { duration(it) }
            .thenBy { it.appName.orEmpty().lowercase(Locale.ROOT) }.thenBy { it.appPackageName.orEmpty() })
            .map { app ->
                val millis = duration(app)
                app.copy(usagePercentage = if (total > 0) millis / total * 100 else 0.0,
                    usageDuration = when {
                        usage == null -> "—"
                        millis in 1..59_999 -> "<1 m"
                        else -> "${millis / 3_600_000} h ${millis / 60_000 % 60} m"
                    })
            }
    }

    fun percentageLabel(percentage: Double) = if (percentage > 0 && percentage < 1) "<1%" else "${percentage.toInt()}%"
}
