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
import com.example.limitr.R
import com.example.limitr.utils.Status
import com.example.limitr.utils.dialogShow
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.ui.home.main_fragment.adapter.AppListViewPagerAdapter
import com.example.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.example.limitr.utils.Constants.DAILYCRYPTOREWARD
import com.example.limitr.utils.Constants.FIRST_LOGIN_REWARD
import com.example.limitr.utils.Constants.IS_NEW_USER
import com.example.limitr.utils.DateAndTime.getTodayDate
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.Permissions.checkAccessibilityPermission
import com.example.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.example.limitr.utils.ViewUtils.showToast
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()


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

        if (!Settings.canDrawOverlays(requireContext()) ||
            !checkAccessibilityPermission(
                requireContext(),
                requireActivity()

            ) || !isUsageStateManagerEnabled(requireContext())
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

        if (!Settings.canDrawOverlays(requireContext()) ||
            !checkAccessibilityPermission(
                requireContext(),
                requireActivity()

            ) || !isUsageStateManagerEnabled(requireContext())
        ) {
            showPermissionDialog()
        } else {
            if (dialog?.isShowing == true) {
                dialog?.dismiss()
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

        dialog?.show()

        val grantAccessiblePermission: Button = dialog?.findViewById(R.id.grantAccessiblePerm)!!
        val grantDisplayOverPermission: Button = dialog?.findViewById(R.id.grantDisplayOverPerm)!!
        val grantUsageState: Button = dialog?.findViewById(R.id.grantUsageStateManager)!!
        val helpText: TextView = dialog?.findViewById(R.id.helpText)!!

        helpText.setOnClickListener {
            showHelpDialog()
        }

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

            if (status == Status.CryptoStatus.INSERT) {
                mainViewModel.upsertEverydayDate(getTodayDate())
                mainViewModel.upsertCrypto(DAILYCRYPTOREWARD)
                rewardDialog.dismiss()

            } else {

                lifecycleScope.launch(Dispatchers.Main) {
                    val currentCrypto = mainViewModel.getCrypto().first()
                    mainViewModel.upsertEverydayDate(getTodayDate())
                    mainViewModel.upsertCrypto(currentCrypto + DAILYCRYPTOREWARD)
                    rewardDialog.dismiss()
                }
            }

        }

        rewardDialog.show()
    }

    /*
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

            firsLoginText.isVisible = true

            claimRewardButton.setOnClickListener {
                mainViewModel.upsertCrypto(FIRST_LOGIN_REWARD)
                firsLoginText.isVisible = false
                rewardDialog.dismiss()
            }

            rewardDialog.show()
        }
    */

    private fun showHelpDialog() {
        val helpDialog = Dialog(requireContext())
        helpDialog.apply {
            window?.setContentView(R.layout.help_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(false)
        }.create()

        val grantPermission: Button = helpDialog.findViewById(R.id.grantAccessiblePerm)

        grantPermission.setOnClickListener {
            goToAccessibilitySettings()
            helpDialog.dismiss()
        }

        helpDialog.show()
    }

    private fun isFirstAuthentication(auth: FirebaseAuth): Boolean {
        var isFirstTime: Boolean = false
        val authStateListener = FirebaseAuth.AuthStateListener { listener ->
            val user: FirebaseUser? = firebaseAuth.currentUser

            if (user != null) {
                val isFirstTimeSignIn =
                    user.metadata?.creationTimestamp == user.metadata?.lastSignInTimestamp

                isFirstTime = isFirstTimeSignIn
            }
        }

        Timber.d("is first time login = $isFirstTime")

        auth.addAuthStateListener(authStateListener)

        return isFirstTime
    }

//    override fun onDestroy() {
//        super.onDestroy()
//
//    }


}
