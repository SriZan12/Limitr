package com.example.limitr.ui.home

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.limitr.R
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.ui.home.appcategory.AppCategoryAdapter
import com.example.limitr.utils.ViewUtils.loadProfilePhoto
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentHome : Fragment(R.layout.fragment_home) {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private val homeFragment = "home"
    private lateinit var dialog: Dialog
    private lateinit var appCategoryAdapter: AppCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        return fragmentHomeBinding.root

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dialog = Dialog(requireContext())
    }

    override fun onResume() {
        super.onResume()

        if (!Settings.canDrawOverlays(requireContext()) ||
            !checkAccessibilityPermission()
        ) {
            showPermissionDialog()
        } else {
            dialog.dismiss()
        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfilePhoto(fragmentHomeBinding.profile, requireContext())

        appCategoryAdapter = AppCategoryAdapter(requireActivity())
        fragmentHomeBinding.viewPager.adapter = appCategoryAdapter

        fragmentHomeBinding.profile.setOnClickListener {
            val action = FragmentHomeDirections.actionFragmentHomeToFragmentEditProfile()
            findNavController().navigate(action)
        }

        TabLayoutMediator(
            fragmentHomeBinding.tabLayout,
            fragmentHomeBinding.viewPager
        ) { tab, position ->
            when (position) {
                0 -> tab.text = "Installed Apps"
                1 -> tab.text = "System Apps"
            }

        }.attach()

    }

    private fun goToDisplayOverOtherAppsSettings() {
        val packageName = requireContext().packageName
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun goToAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun showPermissionDialog() {

        dialog.apply {
            window?.setContentView(R.layout.permission_layout)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(false)
        }

        val grantAccessiblePermission: Button = dialog.findViewById(R.id.grantAccessiblePerm)
        val grantDisplayOverPermission: Button = dialog.findViewById(R.id.grantDisplayOverPerm)

        if (checkAccessibilityPermission()) {
            grantAccessiblePermission.text = "Granted"
            grantAccessiblePermission.isEnabled = false
        }
        if (Settings.canDrawOverlays(requireContext())) {
            grantDisplayOverPermission.text = "Granted"
            grantDisplayOverPermission.isEnabled = false
        }

        grantAccessiblePermission.setOnClickListener {
            goToAccessibilitySettings()
        }

        grantDisplayOverPermission.setOnClickListener {
            goToDisplayOverOtherAppsSettings()
        }

        dialog.show()
    }

    private fun checkAccessibilityPermission(): Boolean {
        var isAccessibilityEnabled = false
        (requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).apply {
            installedAccessibilityServiceList.forEach { installedService ->
                installedService.resolveInfo.serviceInfo.apply {
                    if (getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { it.resolveInfo.serviceInfo.packageName == packageName && it.resolveInfo.serviceInfo.name == name && permission == Manifest.permission.BIND_ACCESSIBILITY_SERVICE && it.resolveInfo.serviceInfo.packageName == requireActivity().packageName })
                        isAccessibilityEnabled = true
                }
            }
        }
        return isAccessibilityEnabled
    }

}
