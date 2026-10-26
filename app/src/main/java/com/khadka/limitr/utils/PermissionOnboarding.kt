package com.khadka.limitr.utils

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.annotation.LayoutRes
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.khadka.limitr.R
import com.khadka.limitr.blocking.EnforcementStatus
import com.khadka.limitr.utils.Permissions.isAccessibilityEnabled

/** DialogFragment keeps the original popup flow intact across Activity recreation. */
abstract class LimitrDialogFragment(@LayoutRes layout: Int) : DialogFragment(layout) {
    override fun getTheme() = R.style.Theme_Limitr_Dialog
    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        dialog?.window?.setLayout(
            minOf(metrics.widthPixels - (32 * metrics.density).toInt(), (560 * metrics.density).toInt()),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}

object PermissionOnboarding {
    const val RESULT = "accessibility_disclosure_result"
    const val TAG = "accessibility_disclosure"
    fun show(activity: FragmentActivity) = show(activity.supportFragmentManager, openSettings = true)
    fun show(manager: FragmentManager, openSettings: Boolean = false) {
        if (!manager.isStateSaved && manager.findFragmentByTag(TAG) == null) {
            AccessibilityDisclosureDialog().apply { arguments = bundleOf("openSettings" to openSettings) }.show(manager, TAG)
        }
    }
}

class AccessibilityDisclosureDialog : LimitrDialogFragment(R.layout.accessibility_service_info_dialog) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val checkbox = view.findViewById<CheckBox>(R.id.acceptCheckbox)
        val accept = view.findViewById<Button>(R.id.acceptButton)
        checkbox.isChecked = savedInstanceState?.getBoolean("checked") ?: false
        accept.isEnabled = checkbox.isChecked
        checkbox.setOnCheckedChangeListener { _, checked -> accept.isEnabled = checked }
        accept.setOnClickListener {
            EnforcementStatus.accept(requireContext())
            if (requireArguments().getBoolean("openSettings")) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            finish(true)
        }
        view.findViewById<Button>(R.id.denyButton).setOnClickListener { finish(false) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("checked", view?.findViewById<CheckBox>(R.id.acceptCheckbox)?.isChecked == true)
    }
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        parentFragmentManager.setFragmentResult(PermissionOnboarding.RESULT, bundleOf("accepted" to false))
    }
    private fun finish(accepted: Boolean) {
        parentFragmentManager.setFragmentResult(PermissionOnboarding.RESULT, bundleOf("accepted" to accepted))
        dismiss()
    }
}

class PermissionSetupDialog : LimitrDialogFragment(R.layout.permission_layout) {
    companion object {
        const val TAG = "permission_setup"
        const val RESULT = "permission_setup_finished"
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.grantAccessiblePerm).setOnClickListener {
            if (EnforcementStatus.consent(requireContext())) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            else PermissionOnboarding.show(childFragmentManager, openSettings = true)
        }
        view.findViewById<Button>(R.id.grantUsageStateManager).setOnClickListener {
            // The disclosure is visible directly above this affirmative button.
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        view.findViewById<Button>(R.id.continueSetup).setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULT, Bundle())
            dismiss()
        }
    }
    override fun onResume() {
        super.onResume()
        view?.findViewById<Button>(R.id.grantAccessiblePerm)?.apply {
            val granted = requireContext().isAccessibilityEnabled() && EnforcementStatus.consent(requireContext())
            setText(if (granted) R.string.Granted else R.string.grant_permission)
            isEnabled = !granted
        }
        view?.findViewById<Button>(R.id.grantUsageStateManager)?.apply {
            val granted = Permissions.isUsageStateManagerEnabled(requireContext())
            setText(if (granted) R.string.Granted else R.string.usage_agree)
            isEnabled = !granted
        }
    }
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        parentFragmentManager.setFragmentResult(RESULT, Bundle())
    }
}
