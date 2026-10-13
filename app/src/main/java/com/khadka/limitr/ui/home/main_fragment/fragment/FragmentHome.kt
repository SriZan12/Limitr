package com.khadka.limitr.ui.home.main_fragment.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import com.khadka.limitr.R
import com.khadka.limitr.blocking.*
import com.khadka.limitr.databinding.FragmentHomeBinding
import com.khadka.limitr.repository.MainFragmentRepository
import com.khadka.limitr.ui.home.main_fragment.adapter.AppListViewPagerAdapter
import com.khadka.limitr.utils.*
import com.khadka.limitr.utils.Permissions.isAccessibilityEnabled
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class FragmentHome : Fragment(R.layout.fragment_home) {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var rewards: MainFragmentRepository
    private var binding: FragmentHomeBinding? = null
    private var mediator: TabLayoutMediator? = null
    private var dialog: AlertDialog? = null
    private var menu: PopupMenu? = null
    private var rewardJob: Job? = null
    private var startupStage = "start"
    private var rewardCheckedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startupStage = savedInstanceState?.getString("startupStage") ?: "start"
        rewardCheckedDate = savedInstanceState?.getString("rewardCheckedDate").orEmpty()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("startupStage", startupStage)
        outState.putString("rewardCheckedDate", rewardCheckedDate)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentHomeBinding.bind(view)
        binding = ui
        ui.viewPager.adapter = AppListViewPagerAdapter(this)
        mediator = TabLayoutMediator(ui.tabLayout, ui.viewPager) { tab, position ->
            tab.text = getString(if (position == 0) R.string.all_apps else R.string.blocked_apps)
        }.also { it.attach() }
        menu = PopupMenu(requireContext(), ui.options).apply {
            inflate(R.menu.home_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_permissions -> showPermissions()
                    R.id.action_accessibility_disclosure -> PermissionOnboarding.show(childFragmentManager)
                    R.id.action_reward -> showReward(fromMenu = true)
                    R.id.action_history -> startActivity(Intent(requireContext(), com.khadka.limitr.ui.history.ActivityHistory::class.java))
                    R.id.action_help -> showHelp()
                    R.id.action_privacy -> showPrivacy()
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
        }
        ui.options.setOnClickListener { menu?.show() }
        ui.enforcementStatus.setOnClickListener { showPermissions() }
        childFragmentManager.setFragmentResultListener(PermissionOnboarding.RESULT, viewLifecycleOwner) { _, result ->
            renderStatus()
            startupStage = if (result.getBoolean("accepted")) "permissions" else "reward"
            continueStartup()
        }
        childFragmentManager.setFragmentResultListener(PermissionSetupDialog.RESULT, viewLifecycleOwner) { _, _ ->
            renderStatus()
            startupStage = "reward"
            continueStartup()
        }
        childFragmentManager.setFragmentResultListener(PermissionSetupDialog.HELP, viewLifecycleOwner) { _, _ -> showHelp() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { coordinator.state.collect { renderStatus() } }
                launch { EnforcementStatus.state.collect { renderStatus() } }
                launch {
                    try {
                        rewards.getCrypto().collect {
                            ui.totalCrypto.text = it.toString()
                            ui.cryptoBalance.contentDescription = getString(R.string.crypto_balance, it)
                        }
                    } catch (_: java.io.IOException) { ui.totalCrypto.text = "—" }
                }
            }
        }
    }
    private fun renderStatus() {
        val ui = binding ?: return
        val state = coordinator.state.value
        ui.enforcementStatus.isVisible = state.error || !state.loaded ||
            !EnforcementStatus.consent(requireContext()) || !requireContext().isAccessibilityEnabled() ||
            EnforcementStatus.state.value != Enforcement.CONNECTED
        ui.enforcementStatus.text = SessionText.availability(requireContext(), state)
    }
    override fun onResume() {
        super.onResume()
        renderStatus()
        viewLifecycleOwner.lifecycleScope.launch { coordinator.refresh() }
        // Fragment reaches RESUMED after this callback returns; cached DataStore reads can be immediate.
        view?.post { continueStartup() }
    }
    private fun continueStartup() {
        if (binding == null || childFragmentManager.isStateSaved || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (startupStage == "start") {
            startupStage = when {
                !EnforcementStatus.consent(requireContext()) -> "disclosure"
                !requireContext().isAccessibilityEnabled() -> "permissions"
                else -> "reward"
            }
        }
        if (startupStage == "done" && rewardCheckedDate != LocalDate.now().toString()) startupStage = "reward"
        when (startupStage) {
            "disclosure" -> PermissionOnboarding.show(childFragmentManager)
            "permissions" -> showPermissions()
            "reward" -> showReward()
        }
    }
    private fun showPermissions() {
        if (!childFragmentManager.isStateSaved && childFragmentManager.findFragmentByTag(PermissionSetupDialog.TAG) == null) {
            PermissionSetupDialog().show(childFragmentManager, PermissionSetupDialog.TAG)
        }
    }
    private fun showReward(fromMenu: Boolean = false) {
        if (rewardJob?.isActive == true || childFragmentManager.findFragmentByTag("reward") != null) return
        rewardJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val offer = rewards.rewardOffer().first()
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || childFragmentManager.isStateSaved) return@launch
                startupStage = "done"
                rewardCheckedDate = LocalDate.now().toString()
                if (offer != null) RewardDialog().apply { arguments = bundleOf("amount" to offer) }.show(childFragmentManager, "reward")
                else if (fromMenu) ViewUtils.showToast(requireContext(), getString(R.string.reward_already_claimed))
            } catch (_: java.io.IOException) {
                startupStage = "done"
                rewardCheckedDate = LocalDate.now().toString()
                ViewUtils.showToast(requireContext(), getString(R.string.reward_storage_error))
            }
        }
    }
    private fun showHelp() {
        fun video(number: Int) = startActivity(Intent(requireContext(), com.khadka.limitr.ui.home.activity.VideoActivity::class.java).putExtra("video", number))
        dialog?.dismiss()
        dialog = AlertDialog.Builder(requireContext()).setTitle(R.string.help_instructions)
            .setMessage(R.string.setup_help_body).setNegativeButton(R.string.ok, null)
            .setNeutralButton(R.string.video_older_android) { _, _ -> video(1) }
            .setPositiveButton(R.string.video_android_13) { _, _ -> video(2) }.show()
    }
    private fun showPrivacy() {
        dialog?.dismiss()
        dialog = AlertDialog.Builder(requireContext()).setTitle(R.string.privacy_control)
            .setMessage("LIMITR works offline without sign-in. Schedules, app selections and Crypto rewards stay on this device. Android backup may copy local schedules and settings using your system backup preferences. Accessibility reads app package names only; no screen contents or event history are saved. Usage statistics and notification access are optional. Notification contents are not stored.\n\nDisable accessibility or stop a session at any time. Essential system apps stay accessible. Battery restrictions, force-stop, split screen, picture-in-picture and service disconnection may interrupt blocking.\n\nThis version no longer connects to Firebase. Existing remote accounts or profile data have not been deleted. See the store listing’s privacy policy for the owner’s contact and data deletion process.")
            .setPositiveButton(R.string.ok, null).show()
    }
    override fun onDestroyView() {
        dialog?.dismiss(); dialog = null
        menu?.dismiss(); menu = null
        mediator?.detach(); mediator = null
        binding?.viewPager?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
