package com.khadka.limitr.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ReconcileReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: SessionCoordinator
    @Inject lateinit var clock: SessionClock
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) clock.reboot()
        val pending = goAsync()
        // goAsync keeps the broadcast alive only for a bounded amount of time. Do not keep
        // a receiver or main-thread work queued indefinitely behind a database transaction.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (withTimeoutOrNull(8_000L) { coordinator.refresh(); true } == null) {
                    Timber.w("Session reconciliation exceeded the broadcast deadline")
                }
            } finally { pending.finish() }
        }
    }
}
