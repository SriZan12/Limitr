package com.khadka.limitr.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.khadka.limitr.R
import com.google.firebase.auth.FirebaseAuth

object FirebaseUtils {

    fun loadProfilePhoto(imageView: ImageView, context: Context) {
        Glide.with(context)
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .placeholder(R.drawable.user)
            .into(imageView)
    }
}