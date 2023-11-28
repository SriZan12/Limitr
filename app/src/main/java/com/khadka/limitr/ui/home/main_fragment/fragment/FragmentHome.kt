package com.khadka.limitr.ui.home.main_fragment.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.FirebaseDatabase
import com.khadka.limitr.R
import com.khadka.limitr.databinding.FragmentHomeBinding
import com.khadka.limitr.ui.home.activity.VideoActivity
import com.khadka.limitr.ui.home.main_fragment.adapter.AppListViewPagerAdapter
import com.khadka.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.khadka.limitr.utils.AppReferrer
import com.khadka.limitr.utils.Constants.APP_REFERRAL_LINK
import com.khadka.limitr.utils.Constants.DAILY_CRYPTO_REWARD
import com.khadka.limitr.utils.Constants.FIRST_LOGIN_REWARD
import com.khadka.limitr.utils.Constants.IS_NEW_USER
import com.khadka.limitr.utils.Constants.REFERRAL_CRYPTO_REWARD
import com.khadka.limitr.utils.DateAndTime.getTodayDate
import com.khadka.limitr.utils.FirebaseUtils.USER_UID
import com.khadka.limitr.utils.FirebaseUtils.getReferralStatus
import com.khadka.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.khadka.limitr.utils.FirebaseUtils.setReferralStatus
import com.khadka.limitr.utils.FirebaseUtils.updateReferralStatus
import com.khadka.limitr.utils.Permissions.isAccessibilityEnabled
import com.khadka.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.khadka.limitr.utils.Status
import com.khadka.limitr.utils.ViewUtils.showToast
import com.khadka.limitr.utils.dialogShow
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
    private var dialog: Dialog? = null
    private var onBackPressed = 0L
    private lateinit var appListViewPagerAdapter: AppListViewPagerAdapter
    private val mainViewModel: MainFragmentViewModel by viewModels()
    private var isReferralCodeStatusChecked = false


    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var editor: SharedPreferences.Editor


    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var appReferrer: AppReferrer

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

        isReferralCodeStatusChecked()


        if (!Settings.canDrawOverlays(requireContext()) ||
            !requireContext().isAccessibilityEnabled() || !isUsageStateManagerEnabled(
                requireContext()
            )
        ) {
            dialog = dialogShow(requireContext(), R.layout.permission_layout)
        }


        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (System.currentTimeMillis() < onBackPressed + 2000) {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                requireActivity().finish()
            } else {
                showToast(requireContext(), getString(R.string.press_again_to_exit))
                onBackPressed = System.currentTimeMillis()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!getAppConsent()) {
            showAccessibilityServiceInfo()
        } else {
            if (!Settings.canDrawOverlays(requireContext()) ||
                !requireContext().isAccessibilityEnabled() || !isUsageStateManagerEnabled(
                    requireContext()
                )
            ) {
                showPermissionDialog()
            } else if (IS_NEW_USER) {

                if (dialog?.isShowing == true) {
                    dialog?.dismiss()
                }

                showFirstLoginRewardDialog()

            } else {

                if (dialog?.isShowing == true) {
                    dialog?.dismiss()
                }

                checkLastLoggedInDate()
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setView()
        setPopUpMenu()

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

        getReferralStatus { status ->
            Timber.d("REFERAL STATUS = $status")
            if (status) {
//                Show Referral Reward Dialog.
                showReferralRewardDialog()
            }
        }

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

    private fun setPopUpMenu() {
        val popupMenu = PopupMenu(requireContext(), fragmentHomeBinding.options)
        popupMenu.menuInflater.inflate(R.menu.home_menu, popupMenu.menu)

        fragmentHomeBinding.options.setOnClickListener {
            popupMenu.show()
        }

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.action_refer_app -> {

                    setReferralStatus()

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, APP_REFERRAL_LINK)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share your referral link"))
                    true
                }

                R.id.action_accessibility_disclosure -> {
                    showAccessibilityServiceInfo()
                    true
                }

                else -> false
            }
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

        dialog?.show()

        val grantAccessiblePermission: Button = dialog?.findViewById(R.id.grantAccessiblePerm)!!
        val grantDisplayOverPermission: Button = dialog?.findViewById(R.id.grantDisplayOverPerm)!!
        val grantUsageState: Button = dialog?.findViewById(R.id.grantUsageStateManager)!!
        val helpButton: Button = dialog?.findViewById(R.id.helpButton)!!

        helpButton.setOnClickListener {
            showHelpDialog()
        }

        if (requireContext().isAccessibilityEnabled()) {
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
            if (lastLoggedDate.isEmpty()) {
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
            lifecycleScope.launch(Dispatchers.Main) {
                val currentCrypto = mainViewModel.getCrypto().first()
                mainViewModel.upsertEverydayDate(getTodayDate())
                mainViewModel.upsertCrypto(currentCrypto + DAILY_CRYPTO_REWARD)
                rewardDialog.dismiss()

                if(!isReferralCodeStatusChecked && IS_NEW_USER) {
                    appReferrer.startReferralClientConnection(context = requireContext())
                }
            }

        }

        rewardDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showFirstLoginRewardDialog() {

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
        val firsLoginText: TextView = rewardDialog.findViewById(R.id.firstLoginText)
        val cryptoText: TextView = rewardDialog.findViewById(R.id.cryptoText)

        firsLoginText.isVisible = true
        firsLoginText.text = getString(R.string.first_login_reward)
        cryptoText.text = FIRST_LOGIN_REWARD.toString()

        claimRewardButton.setOnClickListener {
            mainViewModel.upsertCrypto(FIRST_LOGIN_REWARD)
            IS_NEW_USER = false
            rewardDialog.dismiss()

            checkLastLoggedInDate()
        }


        rewardDialog.show()
    }

    private fun showHelpDialog() {
        val helpDialog = Dialog(requireContext())
        helpDialog.apply {
            window?.setContentView(R.layout.help_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(true)
        }.create()

        val grantPermission: Button = helpDialog.findViewById(R.id.grantAccessiblePerm)
        val watchVideo: Button = helpDialog.findViewById(R.id.watch_video_1)
        val watchVideo2: Button = helpDialog.findViewById(R.id.watch_video_2)

        grantPermission.setOnClickListener {
            goToAccessibilitySettings()
            helpDialog.dismiss()
        }

        watchVideo.setOnClickListener {
            val intent = Intent(requireContext(), VideoActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("androidVersion", Status.AndroidVersion.ANDROID_13_LESS)
            requireContext().startActivity(intent)
        }

        watchVideo2.setOnClickListener {
            val intent = Intent(requireContext(), VideoActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("androidVersion", Status.AndroidVersion.ANDROID_13_PLUS)
            requireContext().startActivity(intent)
        }

        helpDialog.show()
    }

    private fun showAccessibilityServiceInfo() {
        val accessibilityServiceInfoDialog = Dialog(requireContext())
        accessibilityServiceInfoDialog.apply {
            window?.setContentView(R.layout.accessibility_service_info_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1000

            )
            setCancelable(false)
        }.create()

        val acceptButton: Button =
            accessibilityServiceInfoDialog.findViewById(R.id.acceptButton)
        val acceptCheckBox: CheckBox =
            accessibilityServiceInfoDialog.findViewById(R.id.acceptCheckbox)

        val denyButton: Button =
            accessibilityServiceInfoDialog.findViewById(R.id.denyButton)

        acceptCheckBox.isChecked = getAppConsent()

        acceptButton.setOnClickListener {
            if (acceptCheckBox.isChecked) {
                if (!Settings.canDrawOverlays(requireContext()) ||
                    !requireContext().isAccessibilityEnabled() || !isUsageStateManagerEnabled(
                        requireContext()
                    )
                ) {
                    showPermissionDialog()
                    accessibilityServiceInfoDialog.dismiss()

                } else {
                    accessibilityServiceInfoDialog.dismiss()
                }

            } else {
                showToast(requireContext(), "First Agree to Grant Limitr Accessibility Service")
            }
        }

        denyButton.setOnClickListener {
            editor.putBoolean("checkboxStatus", false)
            editor.apply()
            requireActivity().finishAffinity()
        }

        acceptCheckBox.setOnClickListener {

            if (acceptCheckBox.isChecked) {
                editor.putBoolean("checkboxStatus", true)
                editor.apply()
                acceptCheckBox.isChecked = true
            } else {
                editor.putBoolean("checkboxStatus", false)
                editor.apply()
                acceptCheckBox.isChecked = false
            }
        }

        accessibilityServiceInfoDialog.show()
    }

    private fun getAppConsent(): Boolean {
        return sharedPref.getBoolean("checkboxStatus", false)
    }

    private fun isReferralCodeStatusChecked() {
        lifecycleScope.launch(Dispatchers.Main) {
            mainViewModel.isReferralCodeStatusChecked().collect { status ->
                Timber.d("INSIDE isReferralCodeStatusChecked = $status")
                isReferralCodeStatusChecked = status
            }

        }
    }

    private fun showReferralRewardDialog() {
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
        claimRewardButton.text = requireContext().getString(R.string.claim_referral_reward)

        claimRewardButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val currentCrypto = mainViewModel.getCrypto().first()
                mainViewModel.upsertEverydayDate(getTodayDate())
                mainViewModel.upsertCrypto(currentCrypto + REFERRAL_CRYPTO_REWARD)
                updateReferralStatus(userUID = USER_UID, status = false)
                rewardDialog.dismiss()
            }

        }

        rewardDialog.show()
    }
}
