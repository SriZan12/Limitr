package com.example.limitr.utils

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {

    //   Permission Codes
    const val STORAGEPERMISSIONCODE: Int = 1
    const val RC_POST_NOTIFICATION_PERMISSION: Int = 2

    //   Rewards
    const val REQUIREDCRYPTOFORUNBLOCK: Int = 2
    const val DAILYCRYPTOREWARD: Int = 1
    const val FIRST_LOGIN_REWARD: Int = 15


    //   DataStore Keys
    val CRYPTO = intPreferencesKey("Crypto")
    val LASTLOGGEDDATE = stringPreferencesKey("last_logged_date")

    var OVERLAY_DISPLAYED = false

    var IS_NEW_USER: Boolean = false

}
