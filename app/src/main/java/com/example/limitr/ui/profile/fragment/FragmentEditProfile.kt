package com.example.limitr.ui.profile.fragment

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limitr.R
import com.example.limitr.databinding.FragmentEditProfileBinding
import com.example.limitr.resource.EditProfileState
import com.example.limitr.ui.profile.vm.EditProfileViewModel
import com.example.limitr.utils.Constants.STORAGEPERMISSIONCODE
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.ViewUtils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FragmentEditProfile :
    Fragment(R.layout.fragment_edit_profile), EasyPermissions.PermissionCallbacks {

    private lateinit var fragmentEditProfileBinding: FragmentEditProfileBinding
    private lateinit var imageUri: Uri
    private val viewModel: EditProfileViewModel by viewModels()

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentEditProfileBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_edit_profile, container, false)
        return fragmentEditProfileBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        fragmentEditProfileBinding.profileName.setText(
            firebaseAuth.currentUser?.displayName
        )

        loadProfilePhoto(fragmentEditProfileBinding.profileImage, requireContext())

        fragmentEditProfileBinding.profileImage.setOnClickListener {
            checkStoragePermission()
        }

        fragmentEditProfileBinding.ButtonEditProfile.setOnClickListener {
            fragmentEditProfileBinding.progressBar.visibility = View.VISIBLE
            val name = fragmentEditProfileBinding.profileName.text.toString()
            viewModel.updateNameToFirebase(name, fragmentEditProfileBinding.progressBar, requireContext())
        }
    }

    private fun pickImage() {
        val pickImageIntent = Intent(Intent.ACTION_PICK)
        pickImageIntent.type = "image/*"
        launcher.launch(pickImageIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Timber.d("ResultCode = ${result.resultCode} + Activity = ${Activity.RESULT_OK}")
            if (result.resultCode == Activity.RESULT_OK) {
                val res = result.data
                if (res != null) {
                    imageUri = res.data!!

                    fragmentEditProfileBinding.profileImage.setImageURI(imageUri)
                    viewModel.uploadToFirebase(imageUri, requireContext())
                    observeEditProfileState()
                }
            }
        }

    private fun observeEditProfileState() {
        val progressDialog = ProgressDialog(requireContext())
        viewModel._editProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditProfileState.Loading -> {
                    progressDialog.setCancelable(false)
                    progressDialog.show()

                    progressDialog.setMessage("Uploading: ${state.progress} %")
                }

                is EditProfileState.Success -> {
                    showToast(requireContext(), "Profile Updated")
                    progressDialog.dismiss()
                }

                is EditProfileState.Error -> {
                    showToast(requireContext(), state.errorMessage.toString())
                }

                else -> {}
            }
        }
    }


    private fun hasStoragePermission(): Boolean {
        return EasyPermissions.hasPermissions(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private fun checkStoragePermission() {
        if (hasStoragePermission()) {
            pickImage()
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.storage_rationale),
                STORAGEPERMISSIONCODE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        pickImage()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

}
