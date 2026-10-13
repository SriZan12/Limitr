package com.khadka.limitr.ui.history

import android.database.sqlite.SQLiteException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.khadka.limitr.data.local.appdatabase.LimitrDao
import com.khadka.limitr.databinding.ActivityHistoryBinding
import com.khadka.limitr.utils.Insets
import com.khadka.limitr.R
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
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        val adapter = HistoryAdapter()
        binding.rvHistory.adapter = adapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val unresolved = dao.unresolvedLegacy()
                    dao.history().collect { rows ->
                        binding.historyStatus.isVisible = unresolved.isNotEmpty() || rows.isEmpty()
                        binding.historyStatus.text = if (unresolved.isNotEmpty())
                            "${unresolved.size} older selections have no package identity. They are preserved, but need to be selected again in All Apps."
                            else getString(R.string.no_history)
                        adapter.submit(rows)
                    }
                } catch (_: SQLiteException) { binding.historyStatus.isVisible = true; binding.historyStatus.text = "History storage unavailable. Data has been kept." }
                  catch (_: IllegalStateException) { binding.historyStatus.isVisible = true; binding.historyStatus.text = "History storage unavailable. Data has been kept." }
            }
        }
    }
}
