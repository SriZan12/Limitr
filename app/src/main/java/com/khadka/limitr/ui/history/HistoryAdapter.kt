package com.khadka.limitr.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.khadka.limitr.R
import com.khadka.limitr.data.local.appdatabase.model.AppHistoryEntities
import com.khadka.limitr.databinding.ItemAppHistoryBinding
import com.khadka.limitr.utils.DateAndTime
import com.khadka.limitr.utils.ViewUtils
import java.text.DateFormat
import java.util.Date

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.Holder>() {
    private var rows = emptyList<AppHistoryEntities>()
    fun submit(value: List<AppHistoryEntities>) { rows = value; notifyDataSetChanged() }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAppHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        val context = holder.itemView.context
        with(holder.ui) {
            appNameTv.text = row.appName
            iconImg.setImageDrawable(ViewUtils.getAppIconByPackageName(context, row.appPackage))
            noOfTimesBlocked.text = context.getString(R.string.history_count, row.noOfTimesBlocked ?: 0)
            blockedAt.text = context.getString(R.string.history_date, DateAndTime.formatDate(row.blockedDate))
            val start = row.starTime
            val end = row.endTime
            startEndTime.isVisible = start != null && end != null
            startEndTime.text = if (start != null && end != null) {
                val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                context.getString(R.string.history_interval, "${format.format(Date(start))} – ${format.format(Date(end))}")
            } else null
        }
    }
    class Holder(val ui: ItemAppHistoryBinding) : RecyclerView.ViewHolder(ui.root)
}
