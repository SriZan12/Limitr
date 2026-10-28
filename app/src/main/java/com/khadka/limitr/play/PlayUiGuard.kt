package com.khadka.limitr.play

import com.khadka.limitr.blocking.SessionCoordinator
import com.khadka.limitr.blocking.SessionState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** A scheduled Play Store block could cover Play's card with our accessibility overlay. Defer it. */
class PlayUiGuard @Inject constructor(private val coordinator: SessionCoordinator) {
    val availability = coordinator.state.map { allowed(it) }.distinctUntilChanged()
    fun canPresent(): Boolean = allowed(coordinator.state.value)
    private fun allowed(state: SessionState) =
        state.loaded && !state.error && state.sessions.none { it.packageName == "com.android.vending" }
}
