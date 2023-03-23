package com.example.limitr.ui.home.appcategory

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.limitr.R
import com.example.limitr.databinding.SystemAppsBinding
import com.example.limitr.ui.home.AppListAdapter
import com.example.limitr.ui.home.FragmentHomeDirections
import com.example.limitr.ui.home.OnAppClickListener
import com.example.limitr.ui.home.model.AppInfoModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SystemAppFragment : Fragment(R.layout.system_apps) {

    private lateinit var systemAppFragmentBinding: SystemAppsBinding
    private val filteredAppList: MutableList<ApplicationInfo> = mutableListOf()

    @Inject
    lateinit var appListAdapter: AppListAdapter
    private lateinit var appInfo: AppInfoModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        systemAppFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.system_apps, container, false)
        return systemAppFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val systemApps = systemApps()
        for (app in systemApps) {
            if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                filteredAppList.add(app)
            }
        }

        appListAdapter.setAppLists(filteredAppList, requireContext(), onclickListener)

        val dividerItemDecoration = DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        )
        systemAppFragmentBinding.systemAppsRecycler.addItemDecoration(dividerItemDecoration) // Adding separating line below everyList in RecyclerView

        systemAppFragmentBinding.systemAppsRecycler.setHasFixedSize(true)
        systemAppFragmentBinding.systemAppsRecycler.adapter = appListAdapter
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun systemApps(): MutableList<ApplicationInfo> {
        val packageManager = requireContext().packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    }

    private val onclickListener: OnAppClickListener = object : OnAppClickListener {
        override fun onClick(appName: String, appIcon: Drawable, appPackageName: String) {

            appInfo = AppInfoModel(appName, appIcon, appPackageName)

            val action = FragmentHomeDirections.actionFragmentHomeToFragmentBlockApp(appInfo)
            findNavController().navigate(action)
        }

    }
}