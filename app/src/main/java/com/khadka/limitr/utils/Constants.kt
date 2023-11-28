package com.khadka.limitr.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.khadka.limitr.utils.FirebaseUtils.USER_UID

object Constants {

    //   Permission Codes
    const val STORAGE_PERMISSION_CODE: Int = 1
    const val RC_POST_NOTIFICATION_PERMISSION: Int = 2

    //   Rewards
    const val REQUIRED_CRYPTO_FOR_UNBLOCK: Int = 2
    const val DAILY_CRYPTO_REWARD: Int = 1
    const val REFERRAL_CRYPTO_REWARD = 2
    const val FIRST_LOGIN_REWARD: Int = 10


    //   DataStore Keys
    val CRYPTO = intPreferencesKey("Crypto")
    val LAST_LOGGED_DATE = stringPreferencesKey("last_logged_date")
    val IS_REFERRAL_CODE_CHECKED = booleanPreferencesKey("IsReferralCodeChecked")

    var OVERLAY_DISPLAYED = false

    var IS_NEW_USER: Boolean = false

    const val ANDROID_13_PLUS = 1

    private const val APP_PACKAGE_NAME = "com.khadka.Limitr"

    val APP_REFERRAL_LINK =
        "https://play.google.com/store/apps/details?id=$APP_PACKAGE_NAME&referrer=utm_source%3Drefer%26utm_content%3D${USER_UID}"

    const val REFERRAL_NODE = "referralStatus"
}
