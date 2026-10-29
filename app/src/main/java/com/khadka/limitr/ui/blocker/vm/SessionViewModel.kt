package com.khadka.limitr.ui.blocker.vm

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadka.limitr.blocking.*
import com.khadka.limitr.services.notifications.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import com.khadka.limitr.play.RecordBlockingSuccess

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: SessionRepository,
    val coordinator: SessionCoordinator,
    val clock: SessionClock,
    private val unblocker: CryptoUnblocker,
    private val notificationPreferences: NotificationPreferences,
    private val recordBlockingSuccess: RecordBlockingSuccess
) : ViewModel() {
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    val error = MutableStateFlow<String?>(null)
    private val mutableUnblocked = MutableStateFlow(false)
    val unblocked = mutableUnblocked.asStateFlow()
    private val mutableNotificationChoices = MutableStateFlow<Map<String, Boolean>?>(null)
    val notificationChoices = mutableNotificationChoices.asStateFlow()
    private var notificationJob: Job? = null
    private val mutableAlertSetup = MutableStateFlow(false)
    val alertSetup = mutableAlertSetup.asStateFlow()

    fun observeNotificationChoice(packageName: String) {
        notificationJob?.cancel()
        notificationJob = viewModelScope.launch {
            try {
                combine(notificationPreferences.choices, coordinator.state) { choices, state -> choices to state }
                    .collect { (choices, state) ->
                        mutableNotificationChoices.value = choices
                        // Preserve an existing block's option before that row expires; explicit choices win.
                        state.sessions.firstOrNull { it.packageName == packageName && packageName !in choices }?.let {
                            notificationPreferences.inherit(packageName, it.blockNotifications)
                        }
                    }
            } catch (failure: java.io.IOException) {
                mutableNotificationChoices.value = null
                error.value = "Could not load notification preferences. Please reopen this screen to retry."
            }
        }
    }

    fun setNotificationHiding(packageName: String, enabled: Boolean) = act { notificationPreferences.set(packageName, enabled) }
    fun saveTimer(packageName: String, label: String, duration: Long, hideNotifications: Boolean) =
        act(afterSuccess = { mutableAlertSetup.value = true; recordBlockingSuccess() }) { timer(packageName, label, duration, hideNotifications) }
    fun saveInterval(packageName: String, label: String, start: Long, end: Long, hideNotifications: Boolean) =
        act(afterSuccess = { mutableAlertSetup.value = true; recordBlockingSuccess() }) { interval(packageName, label, start, end, hideNotifications) }
    fun consumeAlertSetup() { mutableAlertSetup.value = false }
    fun unblockWithCrypto(session: BlockingSession) = act {
        when (unblocker.unblock(session)) {
            UnblockResult.INSUFFICIENT_CRYPTO -> error.value = "Not enough Crypto. Unblocking requires 2 Crypto."
            UnblockResult.UNBLOCKED -> mutableUnblocked.value = true
            UnblockResult.STALE -> Unit
        }
    }
    fun act(afterSuccess: (suspend () -> Unit)? = null, operation: suspend SessionRepository.() -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        error.value = null
        viewModelScope.launch {
            try { repository.operation(); coordinator.refresh(); afterSuccess?.invoke() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: SQLiteException) { error.value = "Could not save your blocking changes. Existing data has been kept." }
            catch (failure: java.io.IOException) { error.value = "Could not read or save your local settings. Please try again." }
            catch (failure: IllegalStateException) { error.value = "Storage could not be opened. Existing data has been kept; contact support before clearing app data." }
            finally { mutableBusy.value = false }
        }
    }
}
