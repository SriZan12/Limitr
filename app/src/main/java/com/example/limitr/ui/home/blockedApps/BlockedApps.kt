package com.example.limitr.ui.home.blockedApps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limitr.R
import com.example.limitr.data.room.model.LimitrEntities
import com.example.limitr.databinding.BlockedAppListLayoutBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BlockedApps : Fragment(R.layout.blocked_app_list_layout) {

    private lateinit var binding: BlockedAppListLayoutBinding
    private val viewModel: BlockedAppViewModels by viewModels()
    private var blockedAppsList: MutableList<LimitrEntities> = mutableListOf()

    @Inject
    lateinit var blockedAppListAdapter: BlockedAppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.blocked_app_list_layout, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getBlockedApps().observe(viewLifecycleOwner) {

            if (it != null) {
                blockedAppsList.clear()
                for (apps in it) {
                    blockedAppsList.add(apps)
                }
                blockedAppListAdapter.setBlockedAppList(requireContext(), blockedAppsList)
                binding.blockedAppRecycler.adapter = blockedAppListAdapter
            } else {
                binding.noApps.isVisible = true
            }
        }
    }

}