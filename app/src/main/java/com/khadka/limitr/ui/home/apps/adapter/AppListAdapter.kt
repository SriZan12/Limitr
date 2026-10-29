package com.khadka.limitr.ui.home.apps.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khadka.limitr.R
import com.khadka.limitr.databinding.ItemAppBinding
import com.khadka.limitr.ui.home.model.App
import com.khadka.limitr.usage.AppUsageRanking
import kotlin.math.roundToInt

class AppListAdapter(private val click: (String) -> Unit) : ListAdapter<App, AppListAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: App) = with(binding) {
            val context = root.context
            appNameTv.text = app.appName
            iconImg.setImageDrawable(app.appIcon)
            usageDurationTv.text = app.usageDuration
            val unavailable = app.usageDuration == "—"
            usageDurationTv.contentDescription = if (unavailable) context.getString(R.string.usage_unavailable)
                else context.getString(R.string.usage_duration_last_24_hours, app.usageDuration.orEmpty())
            usagePercTv.text = if (unavailable) "—" else AppUsageRanking.percentageLabel(app.usagePercentage)
            progressBar.max = 10_000
            progressBar.progress = (app.usagePercentage * 100).roundToInt().coerceIn(0, 10_000)
            progressBar.contentDescription = if (unavailable) context.getString(R.string.usage_unavailable)
                else context.getString(R.string.usage_share_last_24_hours, usagePercTv.text)
            progressBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, when {
                app.usagePercentage < 50 -> R.color.Normal
                app.usagePercentage < 80 -> R.color.Warning
                else -> R.color.Danger
            }))
            root.setOnClickListener { app.appPackageName?.let(click) }
        }
    }
    private object Diff : DiffUtil.ItemCallback<App>() {
        override fun areItemsTheSame(old: App, new: App) = old.appPackageName == new.appPackageName
        override fun areContentsTheSame(old: App, new: App) = old == new
    }
}
