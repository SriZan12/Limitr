package com.example.limitr.ui.profile.vm

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.limitr.R
import com.example.limitr.resource.LimitrResource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    //    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
//    private val firebaseStorage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val editProfileState =
        MutableLiveData<LimitrResource>(LimitrResource.Loading(0L))


    val _editProfileState: LiveData<LimitrResource> = editProfileState

    fun uploadToFirebase(imageUri: Uri, context: Context) {
        val user = firebaseAuth.currentUser
        val storageReference = firebaseStorage.reference
            .child(context.getString(R.string.UserProfileImages))
            .child(user!!.uid)

        storageReference.putFile(imageUri)
            .addOnSuccessListener { getDownloadImageUrl(storageReference, context) }
            .addOnProgressListener { snapshot ->
                val progress = 100 * snapshot.bytesTransferred / snapshot.totalByteCount
                editProfileState.value = LimitrResource.Loading(progress = progress)
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
            editProfileState.value =
                LimitrResource.Success(context.getString(R.string.profile_updated))
        }.addOnFailureListener { error ->
            LimitrResource.Error(error)
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