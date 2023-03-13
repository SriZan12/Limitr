package com.example.limitr.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.limitr.R
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.ui.home.model.AppInfoModel
import com.example.limitr.utils.ViewUtils.loadProfilePhoto
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class FragmentHome : Fragment(R.layout.fragment_home),
    EasyPermissions.PermissionCallbacks {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private val homeFragment = "home"
    private var filteredAppList: MutableList<ApplicationInfo> = mutableListOf()
    private lateinit var appInfo: AppInfoModel

    companion object {
        private const val RC_DISPLAY_OVERLAY_PERMISSION = 101
        private const val ACCESSIBILITY_SERVICE_PERMISSION = 102
    }


    @Inject
    lateinit var appListAdapter: AppListAdapter

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

        checkDisplayOverOtherAppsPermission()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentHomeBinding.profile.setOnClickListener {
            val action = FragmentHomeDirections.actionFragmentHomeToFragmentEditProfile()
            findNavController().navigate(action)
        }

        loadProfilePhoto(fragmentHomeBinding.profile, requireContext())

        val installedApps = getInstalledApps()
        for (app in installedApps) {
            if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                filteredAppList.add(app)
            }

            filteredAppList.apply {
                sortedBy {
                    it.name
                }
            }

            appListAdapter.setAppLists(filteredAppList, requireContext(), onclickListener)
        }

        val dividerItemDecoration = DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        )
        fragmentHomeBinding.appListRecyclerView.addItemDecoration(dividerItemDecoration) // Adding separating line below everyList in RecyclerView

        fragmentHomeBinding.appListRecyclerView.setHasFixedSize(true)
        fragmentHomeBinding.appListRecyclerView.adapter = appListAdapter
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): MutableList<ApplicationInfo> {
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

    private fun isDisplayOverOtherAppsEnabled(): Boolean {
        return EasyPermissions.hasPermissions(
            requireContext(),
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
    }

    @AfterPermissionGranted(RC_DISPLAY_OVERLAY_PERMISSION)
    private fun checkDisplayOverOtherAppsPermission() {

        Timber.d("Permission: ${!isDisplayOverOtherAppsEnabled()}")

        if (!Settings.canDrawOverlays(requireContext())) {
            Timber.d("Inside first if")
            if (!isDisplayOverOtherAppsEnabled()) {
                Timber.d("Inside second if")
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.DrawOverOtherApps),
                    RC_DISPLAY_OVERLAY_PERMISSION,
                    Manifest.permission.SYSTEM_ALERT_WINDOW
                )
            }
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        when (requestCode) {
            RC_DISPLAY_OVERLAY_PERMISSION -> {
                goToDisplayOverOtherAppsSettings()
            }
            ACCESSIBILITY_SERVICE_PERMISSION -> {
                goToAccessibilitySettings()
            }

        }
    }


    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

        val message = when (requestCode) {
            RC_DISPLAY_OVERLAY_PERMISSION -> getString(R.string.DrawOverOtherApps)
            ACCESSIBILITY_SERVICE_PERMISSION -> getString(R.string.AccessibilityService)

            else -> ""
        }

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Permission Required!")
            setMessage(message)
            setPositiveButton("Ok") { dialog, which ->
                when (requestCode) {
                    RC_DISPLAY_OVERLAY_PERMISSION -> goToDisplayOverOtherAppsSettings()
                    ACCESSIBILITY_SERVICE_PERMISSION -> goToAccessibilitySettings()
                }
            }
            setNegativeButton("Cancel") { dialog, which ->
                when (requestCode) {
                    RC_DISPLAY_OVERLAY_PERMISSION -> checkDisplayOverOtherAppsPermission()
                    ACCESSIBILITY_SERVICE_PERMISSION -> checkAccessibilityService()
                }
            }
        }.setCancelable(false)
            .create().show()
    }

    private fun goToDisplayOverOtherAppsSettings() {
        val packageName = requireContext().packageName
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        launcher.launch(intent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            checkAccessibilityService()
        }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return EasyPermissions.hasPermissions(
            requireContext(),
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE
        )
    }

    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            EasyPermissions.requestPermissions(
                this,
                "This apps need Accessibility Service enabled to function properly.",
                ACCESSIBILITY_SERVICE_PERMISSION,
                Manifest.permission.BIND_ACCESSIBILITY_SERVICE
            )
        }
    }

    private fun goToAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_SERVICE_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ACCESSIBILITY_SERVICE_PERMISSION && resultCode == Activity.RESULT_OK) {
            Toast.makeText(requireContext(), "Granted", Toast.LENGTH_SHORT).show()
        }
    }

}
