package com.example.limitr.ui.home

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

interface OnAppClickListener {

    fun onClick(appName: String, appIcon: Drawable,appPackageName: String)
}