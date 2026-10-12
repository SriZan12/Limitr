package com.khadka.limitr.ui.home.main_fragment.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.khadka.limitr.R
import com.khadka.limitr.blocking.*
import com.khadka.limitr.databinding.FragmentHomeBinding
import com.khadka.limitr.repository.MainFragmentRepository
import com.khadka.limitr.ui.home.main_fragment.adapter.AppListViewPagerAdapter
import com.khadka.limitr.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FragmentHome : Fragment(R.layout.fragment_home) {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var rewards: MainFragmentRepository
    private var binding: FragmentHomeBinding? = null
    private var mediator: TabLayoutMediator? = null
    private var dialog: AlertDialog? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentHomeBinding.bind(view)
        binding = ui
        ui.viewPager.adapter = AppListViewPagerAdapter(this)
        mediator = TabLayoutMediator(ui.tabLayout, ui.viewPager) { tab, position ->
            tab.text = getString(if (position == 0) R.string.all_apps else R.string.blocked_apps)
        }.also { it.attach() }
        ui.profile.setOnClickListener { findNavController().navigate(R.id.fragmentEditProfile) }
        ui.permissionSettings.setOnClickListener { PermissionOnboarding.show(requireActivity() as androidx.fragment.app.FragmentActivity) }
        ui.usageSettings.setOnClickListener {
            dialog = AlertDialog.Builder(requireContext()).setTitle("Optional app usage statistics")
                .setMessage("Usage access lets LIMITR show time spent in apps over the last 24 hours. These statistics are processed on this device and are not transmitted. App blocking works without this permission.")
                .setNegativeButton("Not now", null).setPositiveButton("Agree and open settings") { _, _ -> startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }.show()
        }
        ui.claimReward.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try { rewards.claimReward() }
                catch (_: java.io.IOException) { ViewUtils.showToast(requireContext(), "Rewards storage is unavailable. Please try again.") }
            }
        }
        ui.history.setOnClickListener { startActivity(Intent(requireContext(), com.khadka.limitr.ui.history.ActivityHistory::class.java)) }
        ui.help.setOnClickListener {
            fun video(number: Int) = startActivity(Intent(requireContext(), com.khadka.limitr.ui.home.activity.VideoActivity::class.java).putExtra("video", number))
            dialog = AlertDialog.Builder(requireContext()).setTitle("Blocking setup")
                .setMessage("Open Blocking access, review the disclosure, then enable LIMITR in Android Accessibility settings. Usage access and notification access are optional. Android may restrict sideloaded apps; follow the system guidance. The videos below show older Android interfaces and may differ from your device.")
                .setNegativeButton("Close", null).setNeutralButton("Older Android video") { _, _ -> video(1) }
                .setPositiveButton("Android 13+ video") { _, _ -> video(2) }.show()
        }
        ui.privacy.setOnClickListener {
            dialog = AlertDialog.Builder(requireContext()).setTitle("Privacy and control")
                .setMessage("LIMITR works offline without sign-in. Schedules, app selections, Crypto rewards and your optional profile stay on this device. Android backup may copy local schedules and settings using your system backup preferences. Accessibility reads app package names only; no screen contents or event history are saved. Usage statistics and notification access are optional. Notification contents are not stored.\n\nDisable accessibility or stop a session at any time. Essential system apps stay accessible. Battery restrictions, force-stop, split screen, picture-in-picture and service disconnection may interrupt blocking.\n\nThis version no longer connects to Firebase. Existing remote accounts or profile data have not been deleted. See the store listing’s privacy policy for the owner’s contact and data deletion process.")
                .setPositiveButton(R.string.ok, null).show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { coordinator.state.collect { ui.enforcementStatus.text = SessionText.availability(requireContext(), it) } }
                launch { EnforcementStatus.state.collect { ui.enforcementStatus.text = SessionText.availability(requireContext(), coordinator.state.value) } }
                launch {
                    try { rewards.getCrypto().collect { ui.totalCrypto.text = getString(R.string.crypto_balance, it) } }
                    catch (_: java.io.IOException) { ui.totalCrypto.text = "Crypto unavailable" }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        binding?.enforcementStatus?.text = SessionText.availability(requireContext(), coordinator.state.value)
        binding?.profile?.let { LocalProfile.load(it, requireContext()) }
        viewLifecycleOwner.lifecycleScope.launch { coordinator.refresh() }
    }
    override fun onDestroyView() {
        dialog?.dismiss(); dialog = null
        mediator?.detach(); mediator = null
        binding?.viewPager?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
