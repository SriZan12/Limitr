package com.khadka.limitr.ui.home.blockedApps.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.khadka.limitr.R
import com.khadka.limitr.blocking.SessionClock
import com.khadka.limitr.blocking.SessionCoordinator
import com.khadka.limitr.databinding.BlockedAppListLayoutBinding
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import com.khadka.limitr.ui.home.blockedApps.adapter.BlockedAppListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class BlockedApps : Fragment(R.layout.blocked_app_list_layout) {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var clock: SessionClock
    private var binding: BlockedAppListLayoutBinding? = null
    override fun onViewCreated(view: View, state: Bundle?) {
        val ui = BlockedAppListLayoutBinding.bind(view)
        binding = ui
        val adapter = BlockedAppListAdapter(clock) { pkg ->
            startActivity(Intent(requireContext(), BlockAppActivity::class.java).putExtra("packageName", pkg))
        }
        ui.blockedAppRecycler.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { coordinator.state.collect { value ->
                    adapter.submit(value.sessions)
                    ui.noApps.isVisible = value.sessions.isEmpty()
                    ui.noApps.text = if (value.error) "Saved timers and intervals unavailable. Data has been kept." else if (!value.loaded) "Loading…" else getString(R.string.no_apps_are_blocked)
                } }
                launch { while (isActive) { adapter.tick(); delay(1000) } }
            }
        }
    }
    override fun onDestroyView() { binding?.blockedAppRecycler?.adapter = null; binding = null; super.onDestroyView() }
}
