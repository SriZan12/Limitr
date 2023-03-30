package com.example.limitr.ui.home

import android.graphics.drawable.Drawable

interface OnAppClickListener {

    fun onClick(
        appName: String,
        appIcon: Drawable,
        appPackageName: String
    )
}