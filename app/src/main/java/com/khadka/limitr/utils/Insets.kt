package com.khadka.limitr.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object Insets {
    fun apply(view: View) {
        val left = view.paddingLeft; val top = view.paddingTop
        val right = view.paddingRight; val bottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            target.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
}
