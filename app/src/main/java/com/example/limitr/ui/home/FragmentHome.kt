package com.example.limitr.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.bumptech.glide.Glide
import com.example.limitr.R
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.ui.home.model.AppInfoModel
import com.example.limitr.utils.ViewUtils.loadProfilePhoto
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
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
                    "This app needs the 'Display over other apps' permission to function properly.",
                    RC_DISPLAY_OVERLAY_PERMISSION,
                    Manifest.permission.SYSTEM_ALERT_WINDOW
                )
            }
        }


    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        gotoSettings()
        Toast.makeText(requireContext(), "Granted", Toast.LENGTH_SHORT).show()
    }


    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun gotoSettings() {
        val packageName = requireContext().packageName
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivityForResult(intent, RC_DISPLAY_OVERLAY_PERMISSION)
    }
}
