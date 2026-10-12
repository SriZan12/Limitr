package com.khadka.limitr.utils

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup

fun dialogShow(context: Context, layout: Int): Dialog {

    val dialog = Dialog(context)

    dialog.apply {
        window?.setContentView(layout)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setCancelable(false)
    }

    return dialog
}