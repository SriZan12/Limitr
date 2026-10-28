package com.khadka.limitr.ui.home.apps.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.khadka.limitr.R
import com.khadka.limitr.databinding.ItemAppBinding
import com.khadka.limitr.utils.OnAppClickListener
import com.khadka.limitr.ui.home.model.App
import com.khadka.limitr.usage.AppUsageRanking
import kotlin.math.roundToInt
import com.khadka.limitr.utils.ViewUtils.animateProgressBar
import com.khadka.limitr.utils.ViewUtils.setFadeInAnimation
import javax.inject.Inject

class AppListAdapter @Inject constructor() :
    RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {

    private var appsList: MutableList<App?> = mutableListOf()
    private lateinit var context: Context
    private lateinit var onclickListener: OnAppClickListener

    fun setAppLists(
        filteredAppList: ArrayList<App?>,
        requireContext: Context,
        onclickListener: OnAppClickListener
    ) {
        this.appsList.clear()
        this.appsList.addAll(filteredAppList)
        this.context = requireContext
        this.onclickListener = onclickListener
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val binding: ItemAppBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_app,
                parent,
                false
            )
        return AppListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        holder.bind(appsList[position], holder.itemView, position)
    }

    override fun getItemCount(): Int {
        return appsList.size
    }

    inner class AppListViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(appInfo: App?, itemView: View, position: Int) {
            with(binding) {

                setFadeInAnimation(view = itemView, position = position)

                appNameTv.text = appInfo?.appName
                iconImg.setImageDrawable(appInfo?.appIcon)
                usageDurationTv.text = appInfo?.usageDuration
                val unavailable = appInfo?.usageDuration == "—"
                usageDurationTv.contentDescription = if (unavailable) context.getString(R.string.usage_unavailable)
                    else context.getString(R.string.usage_duration_last_24_hours, appInfo?.usageDuration.orEmpty())
                usagePercTv.text = if (unavailable || appInfo == null) "—" else AppUsageRanking.percentageLabel(appInfo.usagePercentage)

//                slideInAnimation(view = itemView, position = position)

                if (appInfo != null) {
                    progressBar.max = 10_000
                    animateProgressBar(progressBar, (appInfo.usagePercentage * 100).roundToInt().coerceIn(0, 10_000))
                    progressBar.contentDescription = if (unavailable) context.getString(R.string.usage_unavailable)
                        else context.getString(R.string.usage_share_last_24_hours, usagePercTv.text)
                    setUsageColor(appInfo.usagePercentage, progressBar)
                }

                val appPackageName = appInfo?.appPackageName

                mainLinearLayout.setOnClickListener {
                    onclickListener.onClick(
                        appPackageName!!,
                    )
                }
            }

        }

        private fun setUsageColor(usagePercentage: Double, progressBar: ProgressBar) {
            if (usagePercentage < 50) {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Normal))
            } else if (usagePercentage < 80) {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Warning))
            } else {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Danger))

            }
        }

//        private fun slideInAnimation(view: View, position: Int) {
//            val animation = TranslateAnimation(view.width.toFloat(), 0f, 0f, 0f)
//            animation.duration = 500
//            animation.startOffset = (position * 5).toLong()
//
//            view.startAnimation(animation)
//        }

    }
}
