package com.khadka.limitr.ui.home

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.khadka.limitr.R
import com.khadka.limitr.databinding.AccessibilityServiceInfoBinding
import com.khadka.limitr.utils.ViewUtils.showToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccessibilityServiceInfo : Fragment(R.layout.accessibility_service_info) {

    private lateinit var binding: AccessibilityServiceInfoBinding

    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var editor: SharedPreferences.Editor

    override fun onStart() {
        super.onStart()

        val isAcceptedConsent = sharedPref.getBoolean("checkbox-status", false)

        if (isAcceptedConsent) {
            navigateToHomeFragment()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.accessibility_service_info, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.acceptButton.setOnClickListener {
            if (binding.acceptCheckbox.isChecked) {
                navigateToHomeFragment()
            } else {
                showToast(requireContext(), "First Agree to Grant Limitr Accessibility Service")
            }
        }

        binding.acceptCheckbox.setOnClickListener {

            if (binding.acceptCheckbox.isChecked) {
                editor.putBoolean("checkbox-status", true)
                editor.apply()
                binding.acceptCheckbox.isChecked = true
            } else {
                editor.putBoolean("checkbox-status", false)
                editor.apply()
                binding.acceptCheckbox.isChecked = true
            }
        }

    }

    private fun navigateToHomeFragment() {
        val action =
            AccessibilityServiceInfoDirections.actionAccessibilityServiceInfoToFragmentHome()
        findNavController().navigate(action)
    }

}