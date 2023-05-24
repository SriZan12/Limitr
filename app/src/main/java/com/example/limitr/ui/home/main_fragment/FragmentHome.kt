package com.example.limitr.ui.home.main_fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.limitr.R
import com.example.limitr.common.showDialog
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.utils.DateAndTime.getTodayDate
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.Permissions.checkAccessibilityPermission
import com.example.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.example.limitr.utils.ViewUtils.getCrypto
import com.example.limitr.utils.ViewUtils.showToast
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject

enum class CryptoStatus { INSERT, UPDATE }

@AndroidEntryPoint
class FragmentHome :
    Fragment(R.layout.fragment_home) {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private lateinit var dialog: Dialog
    private var onBackPressed = 0L
    private lateinit var appListViewPagerAdapter: AppListViewPagerAdapter

    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var editor: SharedPreferences.Editor

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

        dialog = showDialog(requireContext(),R.layout.permission_layout)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (System.currentTimeMillis() < onBackPressed + 2000) {
                requireActivity().finish()
            } else {
                showToast(requireContext(), "Press again to exit")
                onBackPressed = System.currentTimeMillis()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!Settings.canDrawOverlays(requireContext()) ||
            !checkAccessibilityPermission(
                requireContext(),
                requireActivity()

            ) || !isUsageStateManagerEnabled(requireContext())
        ) {
            showPermissionDialog()
        } else {
            if (dialog.isShowing) {
                dialog.dismiss()
            }

            checkLastLoggedInDate()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reward = getCrypto(sharedPref, requireContext()).toString()

        loadProfilePhoto(fragmentHomeBinding.profile, requireContext())
        appListViewPagerAdapter = AppListViewPagerAdapter(requireActivity())
        fragmentHomeBinding.viewPager.adapter = appListViewPagerAdapter

        fragmentHomeBinding.totalCrypto.text = reward

        TabLayoutMediator(
            fragmentHomeBinding.tabLayout,
            fragmentHomeBinding.viewPager

        ) { tab, position ->
            when (position) {
                0 -> tab.text = "All Apps"
                1 -> tab.text = "Blocked Apps"
            }

        }.attach()

        fragmentHomeBinding.profile.setOnClickListener {
            val action =
                FragmentHomeDirections.actionFragmentHomeToFragmentEditProfile()
            findNavController().navigate(action)
        }

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

    private fun goToUsageStateManagerSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
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

    @SuppressLint("SetTextI18n")
    private fun showPermissionDialog() {

        dialog.show()

        val grantAccessiblePermission: Button = dialog.findViewById(R.id.grantAccessiblePerm)
        val grantDisplayOverPermission: Button = dialog.findViewById(R.id.grantDisplayOverPerm)
        val grantUsageState: Button = dialog.findViewById(R.id.grantUsageStateManager)

        if (checkAccessibilityPermission(requireContext(), requireActivity())) {
            grantAccessiblePermission.text = "Granted"
            grantAccessiblePermission.isEnabled = false
        }
        if (Settings.canDrawOverlays(requireContext())) {
            grantDisplayOverPermission.text = "Granted"
            grantDisplayOverPermission.isEnabled = false
        }

        if (isUsageStateManagerEnabled(requireContext())) {
            grantUsageState.text = "Granted"
            grantUsageState.isEnabled = false
        }

        grantAccessiblePermission.setOnClickListener {
            goToAccessibilitySettings()
        }

        grantDisplayOverPermission.setOnClickListener {
            goToDisplayOverOtherAppsSettings()
        }

        grantUsageState.setOnClickListener {
            goToUsageStateManagerSettings()
        }

        dialog.show()
    }


    private fun showEverydayReward(
        status: CryptoStatus,
        todayDate: String,
    ) {

        val rewardDialog = Dialog(requireContext())
        rewardDialog.apply {
            window?.setContentView(R.layout.claim_rewards)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(false)
        }.create()

        val claimRewardButton: Button = rewardDialog.findViewById(R.id.claimReward)

        claimRewardButton.setOnClickListener {

            if (status == CryptoStatus.INSERT) {
                editor.putString(getString(R.string.last_logged_date), todayDate)
                editor.putInt(getString(R.string.daily_Login_Reward), 1)
                editor.apply()
                rewardDialog.dismiss()

            } else {
                val updateReward = getCrypto(sharedPref, requireContext()) + 1
                editor.putString(getString(R.string.last_logged_date), todayDate)
                editor.putInt(getString(R.string.daily_Login_Reward), updateReward)
                editor.apply()
                rewardDialog.dismiss()
            }

            fragmentHomeBinding.totalCrypto.text =
                getCrypto(sharedPref, requireContext()).toString()
        }
        rewardDialog.show()

    }

    private fun checkLastLoggedInDate() {

        var defaultCryptoStatus = CryptoStatus.INSERT

        val todayDate = getTodayDate()
        val lastLoggedDate = sharedPref.getString(getString(R.string.last_logged_date), "")

        if (lastLoggedDate == null) {
            showEverydayReward(defaultCryptoStatus, todayDate)
        } else if (todayDate != lastLoggedDate) {

            defaultCryptoStatus = CryptoStatus.UPDATE
            showEverydayReward(
                defaultCryptoStatus,
                todayDate
            )
        }

    }

}
