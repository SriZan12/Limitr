package com.khadka.limitr.ui.profile.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.khadka.limitr.R
import com.khadka.limitr.databinding.FragmentEditProfileBinding
import com.khadka.limitr.utils.LocalProfile
import com.khadka.limitr.utils.ViewUtils

class FragmentEditProfile : Fragment(R.layout.fragment_edit_profile) {
    private var binding: FragmentEditProfileBinding? = null
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val prefs = requireContext().getSharedPreferences("local_profile", Context.MODE_PRIVATE)
                val old = prefs.getString("photo", null)
                prefs.edit().putString("photo", uri.toString()).apply()
                if (old != null && old != uri.toString()) {
                    try { requireContext().contentResolver.releasePersistableUriPermission(android.net.Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    catch (_: SecurityException) { /* The old document provider may already have revoked access. */ }
                }
                binding?.profileImage?.let { LocalProfile.load(it, requireContext()) }
            } catch (_: SecurityException) { ViewUtils.showToast(requireContext(), "This provider cannot keep access to that photo. Please select another.") }
        }
    }
    override fun onViewCreated(view: View, state: Bundle?) {
        val ui = FragmentEditProfileBinding.bind(view)
        binding = ui
        val prefs = requireContext().getSharedPreferences("local_profile", Context.MODE_PRIVATE)
        ui.profileName.setText(prefs.getString("name", ""))
        LocalProfile.load(ui.profileImage, requireContext())
        ui.profileImage.setOnClickListener { picker.launch(arrayOf("image/*")) }
        ui.ButtonEditProfile.setOnClickListener {
            prefs.edit().putString("name", ui.profileName.text.toString().trim()).apply()
            ViewUtils.showToast(requireContext(), "Profile saved on this device")
        }
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
