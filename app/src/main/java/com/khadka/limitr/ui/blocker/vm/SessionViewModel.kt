package com.khadka.limitr.ui.blocker.vm

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadka.limitr.blocking.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: SessionRepository,
    val coordinator: SessionCoordinator,
    val clock: SessionClock,
    private val unblocker: CryptoUnblocker
) : ViewModel() {
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    val error = MutableStateFlow<String?>(null)
    private val mutableUnblocked = MutableStateFlow(false)
    val unblocked = mutableUnblocked.asStateFlow()
    fun unblockWithCrypto(session: BlockingSession) = act {
        when (unblocker.unblock(session)) {
            UnblockResult.INSUFFICIENT_CRYPTO -> error.value = "Not enough Crypto. Unblocking requires 2 Crypto."
            UnblockResult.UNBLOCKED -> mutableUnblocked.value = true
            UnblockResult.STALE -> Unit
        }
    }
    fun act(operation: suspend SessionRepository.() -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        error.value = null
        viewModelScope.launch {
            try { repository.operation(); coordinator.refresh() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: SQLiteException) { error.value = "Could not save your blocking changes. Existing data has been kept." }
            catch (failure: java.io.IOException) { error.value = "Could not read or save your local settings. Please try again." }
            catch (failure: IllegalStateException) { error.value = "Storage could not be opened. Existing data has been kept; contact support before clearing app data." }
            finally { mutableBusy.value = false }
        }
    }
}
