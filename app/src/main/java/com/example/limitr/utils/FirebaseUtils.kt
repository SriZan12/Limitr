package com.example.limitr.utils

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseUtils {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance()
    private lateinit var progressDialog: ProgressDialog
    private val tag = "TAG"

    fun uploadToFirebase(imageUri: Uri, context: Context) {
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Updating")
        progressDialog.setCancelable(false)
        progressDialog.show()
        val user = firebaseAuth.currentUser
        val storageReference = firebaseStorage.reference
            .child("UserProfileImages")
            .child(user!!.uid)

        storageReference.putFile(imageUri)
            .addOnSuccessListener { getDownloadImageUrl(storageReference, context) }

    }

    private fun getDownloadImageUrl(reference: StorageReference, context: Context) {
        // This method will download the image's Url from firebase and set the userProfile.
        reference.downloadUrl.addOnSuccessListener { uri ->
            setUserProfileImage(uri!!, context)
        }
    }


    private fun setUserProfileImage(
        uri: Uri,
        context: Context
    ) { // This method will help updating the image of user
        val user = FirebaseAuth.getInstance().currentUser
        val updateProfilePhoto = UserProfileChangeRequest.Builder()
            .setPhotoUri(uri)
            .build()
        assert(user != null)
        user!!.updateProfile(updateProfilePhoto).addOnSuccessListener {
            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }
    }

    fun updateNameToFirebase(
        fullName: String,
        progressBar: ProgressBar,
        context: Context
    ) {
        // Updating the Name of the user
        val updateUsername = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()

        firebaseAuth.currentUser?.updateProfile(updateUsername)?.addOnSuccessListener {
            progressBar.visibility = View.GONE
            Toast.makeText(context, "UserName Updated!", Toast.LENGTH_SHORT).show()
        }
    }
}