package com.khadka.limitr.ui.home.apps.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.khadka.limitr.R
import com.khadka.limitr.databinding.ApplistLayoutBinding
import com.khadka.limitr.repository.AppListRepository
import com.khadka.limitr.repository.AppListState
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import com.khadka.limitr.ui.home.apps.adapter.AppListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppList : Fragment(R.layout.applist_layout) {
    @Inject lateinit var apps: AppListRepository
    private var binding: ApplistLayoutBinding? = null
    private var adapter: AppListAdapter? = null

    override fun onViewCreated(view: View, state: Bundle?) {
        binding = ApplistLayoutBinding.bind(view)
        adapter = AppListAdapter { pkg ->
            startActivity(Intent(requireContext(), BlockAppActivity::class.java).putExtra("packageName", pkg))
        }
        binding?.appListRecycler?.adapter = adapter
        binding?.appListRecycler?.itemAnimator = null // Usage refreshes should not flash or animate every row.
        binding?.appLoadStatus?.setOnClickListener { apps.refresh() }
        render(apps.state.value)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { apps.state.collect(::render) }
        }
    }

    private fun render(state: AppListState) {
        val ui = binding ?: return
        ui.appListSkeleton.isVisible = state.rows == null && !state.failed
        ui.appListRecycler.isVisible = state.rows != null
        ui.appLoadStatus.isVisible = state.failed || state.rows?.isEmpty() == true
        ui.appLoadStatus.setText(if (state.failed) R.string.apps_load_failed else R.string.apps_empty)
        ui.appLoadStatus.isClickable = state.failed
        ui.appLoadStatus.isFocusable = state.failed
        state.rows?.let { adapter?.submitList(it) }
    }

    override fun onResume() {
        super.onResume()
        apps.refresh()
        render(apps.state.value)
        binding?.appListSkeleton?.setActive(true)
    }
    override fun onPause() { binding?.appListSkeleton?.setActive(false); super.onPause() }
    override fun onDestroyView() {
        binding?.appListRecycler?.adapter = null
        adapter = null
        binding = null
        super.onDestroyView()
    }
}
