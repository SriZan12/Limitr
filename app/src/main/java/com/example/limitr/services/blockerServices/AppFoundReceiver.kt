package com.example.limitr.services.blockerServices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.limitr.R
import com.example.limitr.ui.blocker.activity.ActivityBlocked
import timber.log.Timber

class AppFoundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("I AM INSIDE RECEIVER")
        Timber.d("APPNAMERECEIVER = ${intent?.getStringExtra(context?.getString(R.string.appName))}")
        Timber.d("APPpackageNAMERECEIVER = ${intent?.getStringExtra(context?.getString(R.string.packageName))}")
        Timber.d("I AM INSIDE RECEIVER")
        val appName = intent?.getStringExtra(context?.getString(R.string.appName))
        val appPackage = intent?.getStringExtra(context?.getString(R.string.packageName))
        val activityIntent = Intent(context, ActivityBlocked::class.java)
        activityIntent.putExtra(context?.getString(R.string.appName), appName)
        activityIntent.putExtra(context?.getString(R.string.packageName), appPackage)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context!!.startActivity(activityIntent)
    }
}