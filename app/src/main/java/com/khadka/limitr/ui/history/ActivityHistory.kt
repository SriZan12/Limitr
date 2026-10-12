package com.khadka.limitr.ui.history

import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.khadka.limitr.data.local.appdatabase.LimitrDao
import com.khadka.limitr.databinding.ActivityHistoryBinding
import com.khadka.limitr.utils.Insets
import com.khadka.limitr.utils.DateAndTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityHistory : AppCompatActivity() {
    @Inject lateinit var dao: LimitrDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Insets.apply(binding.root)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val unresolved = dao.unresolvedLegacy()
                    binding.historyStatus.text = if (unresolved.isEmpty()) "Most recent session per app; existing history is preserved."
                        else "${unresolved.size} older selections have no package identity. They are preserved, but need to be selected again in All Apps."
                    dao.history().collect { rows ->
                        binding.historyList.adapter = ArrayAdapter(this@ActivityHistory, android.R.layout.simple_list_item_1,
                            rows.map { "${it.appName} • ${it.noOfTimesBlocked ?: 0} sessions\n${DateAndTime.formatDate(it.blockedDate)} • ${it.appPackage}" })
                    }
                } catch (_: SQLiteException) { binding.historyStatus.text = "History storage unavailable. Data has been kept." }
                  catch (_: IllegalStateException) { binding.historyStatus.text = "History storage unavailable. Data has been kept." }
            }
        }
    }
}
