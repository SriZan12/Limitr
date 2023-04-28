package com.example.limitr.utils

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.limitr.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object FirebaseUtils {

    fun loadProfilePhoto(imageView: ImageView, context: Context) {
        Glide.with(context)
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .placeholder(R.drawable.user)
            .into(imageView)
    }
}