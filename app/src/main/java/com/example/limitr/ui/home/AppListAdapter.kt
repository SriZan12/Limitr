package com.example.limitr.ui.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.limitr.R
import com.example.limitr.databinding.AppsListLayoutBinding
import javax.inject.Inject

class AppListAdapter @Inject constructor() :
    RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {

    private var appsList: MutableList<ApplicationInfo> = mutableListOf()
    private lateinit var context: Context
    private lateinit var onclickListener: OnAppClickListener
    private val adapter = "Adapter"

    fun setAppLists(
        filteredAppList: MutableList<ApplicationInfo>,
        requireContext: Context,
        onclickListener: OnAppClickListener
    ) {
        this.appsList = filteredAppList
        this.context = requireContext
        this.onclickListener = onclickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val binding: AppsListLayoutBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.apps_list_layout,
                parent,
                false
            )
        return AppListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return appsList.size
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        holder.bind(appsList[position])
    }

    inner class AppListViewHolder(private val binding: AppsListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(applicationInfo: ApplicationInfo) {
            with(binding) {
                appName.text = applicationInfo.loadLabel(context.packageManager)
                appIcon.setImageDrawable(applicationInfo.loadIcon(context.packageManager))

                val appName = applicationInfo.loadLabel(context.packageManager).toString()
                val appIcon = applicationInfo.loadIcon(context.packageManager)

                navigateNext.setOnClickListener {
                   onclickListener.onClick(appName,appIcon)
                }
            }

        }
    }
}