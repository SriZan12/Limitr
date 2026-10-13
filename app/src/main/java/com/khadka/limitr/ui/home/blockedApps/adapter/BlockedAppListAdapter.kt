package com.khadka.limitr.ui.home.blockedApps.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible
import com.khadka.limitr.blocking.BlockingSession
import com.khadka.limitr.blocking.SessionClock
import com.khadka.limitr.databinding.ItemAppBlockedBinding
import com.khadka.limitr.utils.SessionText
import com.khadka.limitr.utils.ViewUtils

class BlockedAppListAdapter(private val clock: SessionClock, private val click: (String) -> Unit) : RecyclerView.Adapter<BlockedAppListAdapter.Holder>() {
    private var sessions = emptyList<BlockingSession>()
    fun submit(value: List<BlockingSession>) { sessions = value; notifyDataSetChanged() }
    fun tick() { if (itemCount > 0) notifyItemRangeChanged(0, itemCount, "time") }
    override fun getItemCount() = sessions.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(ItemAppBlockedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val session = sessions[position]
        holder.ui.appNameTv.text = session.appName
        holder.ui.iconImg.setImageDrawable(ViewUtils.getAppIconByPackageName(holder.itemView.context, session.packageName))
        holder.ui.root.setOnClickListener { click(session.packageName) }
        holder.time(session)
    }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position) else holder.time(sessions[position])
    }
    inner class Holder(val ui: ItemAppBlockedBinding) : RecyclerView.ViewHolder(ui.root) {
        fun time(session: BlockingSession) {
            ui.interval.text = SessionText.timing(session, clock.now())
            ui.timerText.isVisible = false
        }
    }
}
