package com.khadka.limitr.utils

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.khadka.limitr.utils.FirebaseUtils.USER_UID
import com.khadka.limitr.utils.FirebaseUtils.updateReferralStatus
import com.khadka.limitr.utils.ViewUtils.showToast
import timber.log.Timber
import javax.inject.Inject

class AppReferrer @Inject constructor() {


    fun startReferralClientConnection(context: Context) {
        val referrerClient: InstallReferrerClient =
            InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                Timber.d("INSIDE onInstallReferrerSetupFinished")
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        val response: ReferrerDetails = referrerClient.installReferrer
                        val referrerUrl: String = response.installReferrer
                        val referrerClickTime: Long = response.referrerClickTimestampSeconds
                        val appInstallTime: Long = response.installBeginTimestampSeconds
                        val instantExperienceLaunched: Boolean = response.googlePlayInstantParam

                        obtainReferrerDetails(referrerClient = referrerClient)

                       /* Timber.d("referrerUrl = $referrerUrl")
                        Timber.d("referrerClickTime = $referrerClickTime")
                        Timber.d("appInstallTime = $appInstallTime")
                        Timber.d("instantExperienceLaunched = $instantExperienceLaunched")*/

                        referrerClient.endConnection()
                    }

                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        showToast(context = context, message = "Featured Not Supported")
                    }

                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        showToast(context = context, message = "Feature Unavailable")
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun obtainReferrerDetails(referrerClient: InstallReferrerClient) {
        val response: ReferrerDetails = referrerClient.installReferrer

        val referrerUrl: String = response.installReferrer

        Timber.d("REFERRER URL = $referrerUrl")

        if (referrerUrl.isNotEmpty()) {
            val referrerParts = referrerUrl.split("&")
            Timber.d("Referreral parts = $referrerParts")

            val utmSource = referrerParts.find {
                it.contains("utm_source")
            }?.split("=")?.get(1)

            Timber.d("UTM_SOURCE = $utmSource")

            if (utmSource != null && utmSource == "refer") {
                val utmContent = referrerParts.find {
                    it.contains("utm_content")
                }?.split("=")?.get(1)

                Timber.d("UTM_CONTENT = $utmContent")

                if (utmContent != null) {
                        updateReferralStatus(userUID = utmContent, status = true)
                    Timber.d("REFCODE = $utmContent")
                }
            }
        }
    }

}