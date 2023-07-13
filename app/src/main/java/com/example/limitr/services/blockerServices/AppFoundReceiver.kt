package com.example.limitr.services.blockerServices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.limitr.ui.blocker.activity.ActivityBlocked
import timber.log.Timber

class AppFoundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val activityIntent = Intent(context, ActivityBlocked::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context!!.startActivity(activityIntent)

    }
}