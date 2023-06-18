package com.example.limitr.ui.home.blockedApps.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limitr.R
import com.example.limitr.utils.OnAppClickListener
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.databinding.BlockedAppListLayoutBinding
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.blockedApps.adapter.BlockedAppListAdapter
import com.example.limitr.ui.home.blockedApps.vm.BlockedAppViewModels
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
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
                    Timber.d("BLOCKEDAPPS = ${apps.appName}")
                }

                blockedAppListAdapter.setBlockedAppList(
                    requireContext(),
                    blockedAppsList,
                    onclickListener
                )
                binding.blockedAppRecycler.adapter = blockedAppListAdapter
            } else {
                binding.noApps.isVisible = true
            }
        }
    }

    private val onclickListener: OnAppClickListener = object : OnAppClickListener {
        override fun onClick(
            appPackageName: String
        ) {
            val intent = Intent(requireContext(), BlockAppActivity::class.java)
            intent.putExtra(requireContext().getString(R.string.packageName), appPackageName)

            startActivity(intent)


        }

    }

}