package com.example.limitr.utils
import com.example.limitr.MainActivity

object ViewUtils {

    fun hideToolBar(mainActivity: MainActivity) {
        val act: MainActivity = mainActivity
        act.supportActionBar?.hide()
    }
}