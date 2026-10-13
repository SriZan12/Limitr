package com.khadka.limitr.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.khadka.limitr.R

object LocalProfile {
    fun load(view: ImageView, context: Context) {
        val uri = context.getSharedPreferences("local_profile", Context.MODE_PRIVATE).getString("photo", null)
        Glide.with(view).load(uri).placeholder(R.drawable.user).error(R.drawable.user).into(view)
    }
}
