package com.example.limitr.utils

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.example.tasker.ui.profile.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseUtils {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance()
    private val userUid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var progressDialog: ProgressDialog
    private val tag = "TAG"

    fun getUserName(onCallBack: (username: String) -> Unit) {
        if (userUid != null) {
            FirebaseDatabase.getInstance().reference.child("Users")
                .child(userUid).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        onCallBack((snapshot.child("name").value.toString()))
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun uploadToFirebase(imageUri: Uri, context: Context) {
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Updating")
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
        val request = UserProfileChangeRequest.Builder()
            .setPhotoUri(uri)
            .build()
        assert(user != null)
        user!!.updateProfile(request).addOnSuccessListener {
            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }
    }

    fun updateNameToFirebase(
        fullName: String,
        progressBar: ProgressBar
    ) { // Updating the Name of the user
        val user = User(fullName)
        val userUid = firebaseAuth.currentUser?.uid

        if (userUid != null) {
            FirebaseDatabase.getInstance().reference.child("Users").child(userUid)
                .setValue(user).addOnSuccessListener {
                    progressBar.visibility = View.GONE
                }
        }
    }
}