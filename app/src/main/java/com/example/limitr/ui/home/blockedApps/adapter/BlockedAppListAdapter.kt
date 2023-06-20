package com.example.limitr.ui.home.blockedApps.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.limitr.R
import com.example.limitr.utils.OnAppClickListener
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.databinding.ItemAppBlockedBinding
import com.example.limitr.utils.DateAndTime.getIntervalForBlocking
import com.example.limitr.utils.DateAndTime.getTimer
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName
import com.example.limitr.utils.ViewUtils.setFadeInAnimation
import javax.inject.Inject

class BlockedAppListAdapter @Inject constructor() :
    RecyclerView.Adapter<BlockedAppListAdapter.BlockedAppViewHolder>() {

    private var blockedAppsList: MutableList<LimitrEntities> = mutableListOf()
    private lateinit var context: Context
    private lateinit var onClickListener: OnAppClickListener


    fun setBlockedAppList(
        context: Context,
        blockedAppsList: MutableList<LimitrEntities>,
        onClickListener: OnAppClickListener
    ) {
        this.blockedAppsList.clear()
        this.blockedAppsList.addAll(blockedAppsList)
        this.context = context
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedAppViewHolder {
        val binding: ItemAppBlockedBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_app_blocked,
                parent,
                false
            )
        return BlockedAppViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return blockedAppsList.size
    }

    override fun onBindViewHolder(holder: BlockedAppViewHolder, position: Int) {
        holder.bind(
            appInfo = blockedAppsList[position],
            view = holder.itemView,
            position = position
        )
    }

    inner class BlockedAppViewHolder(val binding: ItemAppBlockedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(appInfo: LimitrEntities, view: View, position: Int) {
            with(binding) {
                appNameTv.text = appInfo.appName
                setFadeInAnimation(view = view, position = position)

                if (appInfo.starTime != null && appInfo.endTime != null) {
                    interval.isVisible = true
                    getIntervalForBlocking(
                        appInfo.starTime,
                        appInfo.endTime,
                        appInfo.remainingTime,
                        timerText,
                        interval
                    )
                } else {
                    timerText.isVisible = false
                    interval.isVisible = true
                    getTimer(
                        appInfo.blockedTime,
                        appInfo.remainingTime,
                        interval
                    )
                }
                iconImg.setImageDrawable(getAppIconByPackageName(context, appInfo.appPackage!!))

                binding.mainLinearLayout.setOnClickListener {
                    onClickListener.onClick(appInfo.appPackage!!)
                }
            }
        }
    }
}