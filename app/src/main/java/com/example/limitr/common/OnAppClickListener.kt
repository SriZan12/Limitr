package com.example.limitr.common

import android.graphics.drawable.Drawable

interface OnAppClickListener {

    fun onClick(
        appPackageName: String
    )
}