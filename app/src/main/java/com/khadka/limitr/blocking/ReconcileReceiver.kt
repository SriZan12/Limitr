package com.khadka.limitr.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReconcileReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var clock: SessionClock
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) clock.reboot()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try { coordinator.refresh() } finally { pending.finish() }
        }
    }
}
