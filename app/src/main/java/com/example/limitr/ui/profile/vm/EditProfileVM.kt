package com.example.limitr.ui.profile.vm

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.limitr.resource.EditProfileState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor() : ViewModel() {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseStorage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val editProfileState =
        MutableLiveData<EditProfileState>(EditProfileState.Loading(0L))


    val _editProfileState: LiveData<EditProfileState> = editProfileState
    private lateinit var progressDialog: ProgressDialog

    fun uploadToFirebase(imageUri: Uri, context: Context) {
        val user = firebaseAuth.currentUser
        val storageReference = firebaseStorage.reference
            .child("UserProfileImages")
            .child(user!!.uid)

        storageReference.putFile(imageUri)
            .addOnSuccessListener { getDownloadImageUrl(storageReference, context) }
            .addOnProgressListener { snapshot ->
                val progress = 100 * snapshot.bytesTransferred / snapshot.totalByteCount
                editProfileState.value = EditProfileState.Loading(progress = progress)
            }

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
            editProfileState.value = EditProfileState.Success
            progressDialog.dismiss()
        }.addOnFailureListener { error ->
            EditProfileState.Error(error.message.toString())
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