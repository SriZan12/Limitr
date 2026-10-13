package com.khadka.limitr.blocking

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class SessionState(val sessions: List<BlockingSession> = emptyList(), val loaded: Boolean = false, val error: Boolean = false, val sampledAt: TimeSnapshot? = null)

@Singleton
class SessionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SessionRepository,
    private val clock: SessionClock
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(SessionState())
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var started = false
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val pending by lazy {
        PendingIntent.getBroadcast(context, 36, Intent(context, ReconcileReceiver::class.java).setAction("com.khadka.limitr.RECONCILE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun start() {
        if (started) return
        started = true
        scope.launch {
            try {
                repository.changes.collect { refresh() }
            } catch (error: SQLiteException) { storageFailure(error) }
              catch (error: IllegalStateException) { storageFailure(error) }
        }
        scope.launch {
            state.collectLatest { value ->
                if (value.error || !value.loaded) return@collectLatest
                val wait = ScheduleEvaluator.nextBoundary(value.sessions, clock.now()) ?: return@collectLatest
                delay(wait.coerceAtLeast(1))
                refresh()
            }
        }
    }

    suspend fun refresh() = mutex.withLock {
        try {
            val now = clock.now()
            val sessions = repository.reconcile(now)
            val previous = state.value
            mutableState.value = SessionState(sessions, loaded = true, sampledAt = now)
            if (previous.loaded && !previous.error) SessionNotifications.transitions(context, previous.sessions, sessions, now)
            alarms.cancel(pending)
            ScheduleEvaluator.nextBoundary(sessions, now)?.let { delay ->
                // Inexact wakeup is a recovery hint, never the source of session correctness.
                alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, pending)
            }
        } catch (error: SQLiteException) { storageFailure(error) }
          catch (error: IllegalStateException) { storageFailure(error) }
    }

    private fun storageFailure(error: RuntimeException) {
        // No package names, screen content, account information or database contents in diagnostics.
        Timber.e("Session storage unavailable: %s", error.javaClass.simpleName)
        mutableState.value = SessionState(error = true)
        alarms.cancel(pending)
    }
}
