package com.example.limitr.ui.home.apps

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.limitr.R
import com.example.limitr.databinding.ItemAppBinding
import com.example.limitr.common.OnAppClickListener
import com.example.limitr.ui.home.model.App
import com.example.limitr.utils.ViewUtils.animateProgressBar
import javax.inject.Inject

class AppListAdapter @Inject constructor() :
    RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {

    private var appsList: MutableList<App?> = mutableListOf()
    private lateinit var context: Context
    private lateinit var onclickListener: OnAppClickListener
    private val adapter = "Adapter"

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
        return position
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
        holder.bind(appsList[position])
    }

    override fun getItemCount(): Int {
        return appsList.size
    }

    inner class AppListViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(appInfo: App?) {
            with(binding) {
                appNameTv.text = appInfo?.appName
                iconImg.setImageDrawable(appInfo?.appIcon)
                usageDurationTv.text = appInfo?.usageDuration
                usagePercTv.text = appInfo?.usagePercentage.toString() + "%"

                if (appInfo != null) {
                    animateProgressBar(progressBar, appInfo.usagePercentage)
                    setUsageColor(appInfo.usagePercentage, progressBar)
                }

                val appName = appInfo?.appName
                val appIcon = appInfo?.appIcon
                val appPackageName = appInfo?.appPackageName

                mainLinearLayout.setOnClickListener {
                    onclickListener.onClick(
                        appPackageName!!,
                    )
                }
            }

        }

        private fun setUsageColor(usagePercentage: Int, progressBar: ProgressBar) {
            if (usagePercentage < 50) {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Normal))
            } else if (usagePercentage in 50..79) {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Warning))
            } else {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Danger))

            }
        }
    }
}