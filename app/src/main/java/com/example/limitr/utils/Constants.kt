package com.example.limitr.utils

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {

    //   Permission Codes
    const val STORAGE_PERMISSION_CODE: Int = 1
    const val RC_POST_NOTIFICATION_PERMISSION: Int = 2

    //   Rewards
    const val REQUIRED_CRYPTO_FOR_UNBLOCK: Int = 2
    const val DAILY_CRYPTO_REWARD: Int = 1
    const val FIRST_LOGIN_REWARD: Int = 15


    //   DataStore Keys
    val CRYPTO = intPreferencesKey("Crypto")
    val LAST_LOGGED_DATE = stringPreferencesKey("last_logged_date")

    var OVERLAY_DISPLAYED = false

    var IS_NEW_USER: Boolean = false

    const val ANDROID_13_PLUS = 1

}
