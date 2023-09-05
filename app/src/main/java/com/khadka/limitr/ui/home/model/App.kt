package com.khadka.limitr.ui.home.model

import android.graphics.drawable.Drawable

data class App(
    var appIcon: Drawable? = null,
    var appName: String? = null,
    var appPackageName: String? = null,
    var usagePercentage: Int = 0,
    var usageDuration: String? = null

) {

}
