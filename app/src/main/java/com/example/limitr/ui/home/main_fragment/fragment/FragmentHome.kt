package com.example.limitr.ui.home.main_fragment.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.limitr.R
import com.example.limitr.common.Status
import com.example.limitr.common.showDialog
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.ui.home.main_fragment.adapter.AppListViewPagerAdapter
import com.example.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.example.limitr.utils.Constants.CRYPTO
import com.example.limitr.utils.Constants.DAILYCRYPTOREWARD
import com.example.limitr.utils.Constants.LASTLOGGEDDATE
import com.example.limitr.utils.DateAndTime.getTodayDate
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.Permissions.checkAccessibilityPermission
import com.example.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.example.limitr.utils.ViewUtils.showToast
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class FragmentHome :
    Fragment(R.layout.fragment_home) {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private lateinit var dialog: Dialog
    private var onBackPressed = 0L
    private lateinit var appListViewPagerAdapter: AppListViewPagerAdapter
    private val mainViewModel: MainFragmentViewModel by viewModels()

    @Inject
    lateinit var dataStore: DataStore<Preferences>

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

        dialog = showDialog(requireContext(), R.layout.permission_layout)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (System.currentTimeMillis() < onBackPressed + 2000) {
                requireActivity().finish()
            } else {
                showToast(requireContext(), getString(R.string.press_again_to_exit))
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

        setView()

        fragmentHomeBinding.profile.setOnClickListener {
            val action =
                FragmentHomeDirections.actionFragmentHomeToFragmentEditProfile()
            findNavController().navigate(action)
        }

    }

    private fun setView() {

        loadProfilePhoto(fragmentHomeBinding.profile, requireContext())
        appListViewPagerAdapter = AppListViewPagerAdapter(requireActivity())
        fragmentHomeBinding.viewPager.adapter = appListViewPagerAdapter

        lifecycleScope.launch(Dispatchers.Main) {
            mainViewModel.getCrypto().collect { crypto ->
                fragmentHomeBinding.totalCrypto.text = crypto.toString()
            }
        }

        TabLayoutMediator(
            fragmentHomeBinding.tabLayout,
            fragmentHomeBinding.viewPager

        ) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.all_apps)
                1 -> tab.text = getString(R.string.blocked_apps)
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
            grantAccessiblePermission.text = getText(R.string.Granted)
            grantAccessiblePermission.isEnabled = false
        }
        if (Settings.canDrawOverlays(requireContext())) {
            grantDisplayOverPermission.text = getText(R.string.Granted)
            grantDisplayOverPermission.isEnabled = false
        }

        if (isUsageStateManagerEnabled(requireContext())) {
            grantUsageState.text = getText(R.string.Granted)
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

    }


    private fun checkLastLoggedInDate() {
        var defaultCryptoStatus = Status.CryptoStatus.INSERT

        lifecycleScope.launch(Dispatchers.Main) {
            val lastLoggedDate = mainViewModel.getLastLoggedDate().first()
            if (lastLoggedDate == null || lastLoggedDate.isEmpty()) {
                Timber.d("INSIDE CHECKLOGGED IF")
                showEverydayReward(defaultCryptoStatus)
            } else if (getTodayDate() != lastLoggedDate) {
                Timber.d("INSIDE CHECKLOGGED ELSE")
                defaultCryptoStatus = Status.CryptoStatus.UPDATE
                showEverydayReward(defaultCryptoStatus)
            }

        }

    }

    private fun showEverydayReward(
        status: Status.CryptoStatus,
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

            if (status == Status.CryptoStatus.INSERT) {
                mainViewModel.upsertEverydayDate(getTodayDate())
                mainViewModel.upsertCrypto(DAILYCRYPTOREWARD)
                rewardDialog.dismiss()

            } else {

                lifecycleScope.launch(Dispatchers.Main) {
                    val currentCrypto = mainViewModel.getCrypto().first()
                    if (currentCrypto != null) {
                        mainViewModel.upsertEverydayDate(getTodayDate())
                        mainViewModel.upsertCrypto(currentCrypto + DAILYCRYPTOREWARD)
                        rewardDialog.dismiss()
                    }
                }
            }

        }

        rewardDialog.show()
    }


}
