package com.khadka.limitr.ui.history

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.khadka.limitr.R
import com.khadka.limitr.data.local.appdatabase.model.historyentities.AppHistoryEntities
import com.khadka.limitr.databinding.ItemAppHistoryBinding
import com.khadka.limitr.utils.DateAndTime.formatDate
import com.khadka.limitr.utils.DateAndTime.formatTime
import com.khadka.limitr.utils.DateAndTime.formatTimeInNumbers
import com.khadka.limitr.utils.OnAppClickListener
import com.khadka.limitr.utils.ViewUtils
import javax.inject.Inject


class AppHistoryListAdapter @Inject constructor() :
    RecyclerView.Adapter<AppHistoryListAdapter.AppHistoryViewHolder>() {

    private var appHistoryList: MutableList<AppHistoryEntities> = mutableListOf()
    private lateinit var context: Context
//    private lateinit var onClickListener: OnAppClickListener


    fun setAppHistoryList(
        context: Context,
        appHistoryList: MutableList<AppHistoryEntities>,
//        onClickListener: OnAppClickListener
    ) {
        this.appHistoryList.clear()
        this.appHistoryList.addAll(appHistoryList)
        this.context = context
//        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppHistoryViewHolder {
        val binding: ItemAppHistoryBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_app_history,
                parent,
                false
            )
        return AppHistoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return appHistoryList.size
    }

    override fun onBindViewHolder(holder: AppHistoryViewHolder, position: Int) {
        holder.bind(
            appInfo = appHistoryList[position],
            view = holder.itemView,
            position = position
        )
    }

    inner class AppHistoryViewHolder(val binding: ItemAppHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(appInfo: AppHistoryEntities, view: View, position: Int) {
            with(binding) {
                appNameTv.text = appInfo.appName
                blockedAt.text = formatDate(date = appInfo.blockedDate) ?: ""
                noOfTimesBlocked.text = "10"
                iconImg.setImageDrawable(
                    ViewUtils.getAppIconByPackageName(
                        context = context,
                        appInfo.appPackage
                    )!!
                )

                if (appInfo.starTime != null && appInfo.endTime != null) {
                    textFrom.isVisible = true
                    startEndTime.isVisible = true
                    startEndTime.text =
                        "${formatTime(time = appInfo.starTime!!)} - ${
                            formatTime(time = appInfo.endTime!!)
                        }"
                }

                ViewUtils.setFadeInAnimation(view = view, position = position)

            }
        }
    }
}