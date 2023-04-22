package com.example.limitr.ui.home.blockedApps

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.limitr.R
import com.example.limitr.data.room.model.LimitrEntities
import com.example.limitr.databinding.ItemAppBlockedBinding
import com.example.limitr.utils.ViewUtils.getAppIconByPackageName
import com.example.limitr.utils.ViewUtils.getIntervalForBlocking
import com.example.limitr.utils.ViewUtils.getTimer
import javax.inject.Inject

class BlockedAppListAdapter @Inject constructor() :
    RecyclerView.Adapter<BlockedAppListAdapter.BlockedAppViewHolder>() {

    private var blockedAppsList: MutableList<LimitrEntities> = mutableListOf()
    private lateinit var context: Context


    fun setBlockedAppList(context: Context, blockedAppsList: MutableList<LimitrEntities>) {
        this.blockedAppsList.clear()
        this.blockedAppsList.addAll(blockedAppsList)
        this.context = context
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
        holder.bind(blockedAppsList[position])
    }

    inner class BlockedAppViewHolder(val binding: ItemAppBlockedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(data: LimitrEntities) {
            with(binding) {
                appNameTv.text = data.appName
                if (data.starTime != null && data.endTime != null) {
                    interval.isVisible = true
                    getIntervalForBlocking(
                        data.starTime,
                        data.endTime,
                        data.remainingTime,
                        timerText,
                        interval
                    )
                } else {
                    timerText.isVisible = false
                    interval.isVisible = true
                    getTimer(
                        data.blockedTime,
                        data.remainingTime,
                        interval
                    )
                }
                iconImg.setImageDrawable(getAppIconByPackageName(context, data.appPackage!!))
            }
        }
    }
}