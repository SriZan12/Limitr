package com.khadka.limitr.ui.home.main_fragment.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.khadka.limitr.R
import com.khadka.limitr.repository.MainFragmentRepository
import com.khadka.limitr.utils.Constants
import com.khadka.limitr.utils.LimitrDialogFragment
import com.khadka.limitr.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RewardDialog : LimitrDialogFragment(R.layout.claim_rewards) {
    @Inject lateinit var rewards: MainFragmentRepository
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val amount = requireArguments().getInt("amount")
        view.findViewById<TextView>(R.id.cryptoText).text = amount.toString()
        view.findViewById<TextView>(R.id.firstLoginText).setText(
            if (amount == Constants.FIRST_LOGIN_REWARD) R.string.first_login_reward else R.string.daily_Login_Reward
        )
        val claim = view.findViewById<Button>(R.id.claimReward)
        claim.setOnClickListener {
            claim.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    rewards.claimReward()
                    // Defer dismissal if Android saved FragmentManager state during the disk write.
                    claimed = true
                    if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) dismiss()
                } catch (_: java.io.IOException) {
                    claim.isEnabled = true
                    ViewUtils.showToast(requireContext(), getString(R.string.reward_storage_error))
                }
            }
        }
    }
    private var claimed = false
    override fun onResume() { super.onResume(); if (claimed) dismiss() }
}
