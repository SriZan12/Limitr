package com.example.limitr.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.limitr.R
import com.example.limitr.databinding.FragmentEditProfileBinding
import com.example.limitr.utils.FirebaseUtils.updateNameToFirebase
import com.example.limitr.utils.FirebaseUtils.uploadToFirebase
import com.example.limitr.utils.ViewUtils.loadProfilePhoto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

class FragmentEditProfile :
    Fragment(R.layout.fragment_edit_profile), EasyPermissions.PermissionCallbacks{

    private lateinit var fragmentEditProfileBinding: FragmentEditProfileBinding
    private lateinit var imageUri: Uri
    private val STORAGEPERMISSIONCODE: Int = 1
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var firebaseAuth: FirebaseAuth
    private val editProfile = "EditTheProfile"

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

        firebaseStorage = FirebaseStorage.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        fragmentEditProfileBinding.profileName.setText(
            FirebaseAuth.getInstance().currentUser?.displayName
        )

        loadProfilePhoto(fragmentEditProfileBinding.profileImage,requireContext())

        fragmentEditProfileBinding.profileImage.setOnClickListener {
            checkStoragePermission()
        }

        fragmentEditProfileBinding.ButtonEditProfile.setOnClickListener {
            fragmentEditProfileBinding.progressBar.visibility = View.VISIBLE
            val name = fragmentEditProfileBinding.profileName.text.toString()
            updateNameToFirebase(name, fragmentEditProfileBinding.progressBar, requireContext())
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
                    uploadToFirebase(imageUri, requireContext())
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
