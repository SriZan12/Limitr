package com.khadka.limitr.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlayPrompt {
    data class Update(val info: AppUpdateInfo, val flow: UpdateFlow) : PlayPrompt
    data object Review : PlayPrompt
}

data class PlayUiState(
    val prompt: PlayPrompt? = null,
    val installState: UpdateInstallState = UpdateInstallState.IDLE,
    val completingUpdate: Boolean = false,
    val installFailed: Boolean = false,
    val reviewRunning: Boolean = false
)

@HiltViewModel
class PlayEngagementViewModel @Inject constructor(
    private val updates: PlayUpdateRepository,
    private val prompts: PlayPromptRepository,
    private val selectUpdate: SelectUpdateFlow,
    private val requestReview: RequestReviewIfEligible
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlayUiState())
    val state = mutableState.asStateFlow()
    private var offer: AppUpdateInfo? = null
    private var ready = false
    private var checked = false
    private var allowNewPrompts = true
    private var updateRunning = false
    private var recoveryDeclined = false
    private var installGeneration = 0
    private var checkJob: Job? = null
    private var promptJob: Job? = null

    init {
        viewModelScope.launch {
            try { prompts.recordUse(System.currentTimeMillis()) }
            catch (_: java.io.IOException) { /* Optional prompts remain unavailable. */ }
        }
        // Register before any update launch; stays alive across rotation, removed on ViewModel clear.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            updates.installStates.collect { install ->
                installGeneration++
                mutableState.update { it.copy(installState = install, installFailed = false) }
            }
        }
    }

    fun onForeground(allowNewPrompts: Boolean = true) {
        this.allowNewPrompts = allowNewPrompts
        if (state.value.reviewRunning) return
        // Result callbacks normally arrive before onResume. Recovery also covers process recreation.
        if (state.value.prompt == null) updateRunning = false
        checked = false
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            val generation = installGeneration
            try {
                offer = updates.check() // Always obtain a fresh, single-use AppUpdateInfo.
                if (generation == installGeneration) mutableState.update {
                    it.copy(installState = offer!!.snapshot().installState)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { offer = null } // No Play Store/offline: core app continues normally.
            checked = true
            considerPrompt()
        }
    }

    fun setReady(value: Boolean) {
        ready = value
        if (!value && state.value.prompt == PlayPrompt.Review) {
            // An optional review must not wait on a queue and surprise the user on a later visit.
            mutableState.update { it.copy(prompt = null, reviewRunning = false) }
        }
        if (value) considerPrompt()
    }

    private fun considerPrompt() {
        if (!ready || !checked || updateRunning || state.value.reviewRunning ||
            state.value.prompt != null || promptJob?.isActive == true) return
        promptJob = viewModelScope.launch {
            try {
                val info = offer
                val snapshot = info?.snapshot()?.copy(installState = state.value.installState)
                val flow = snapshot?.let { selectUpdate(it, prompts.lastUpdateFlow(it.version)) }
                if (info != null && snapshot != null && flow != null) {
                    val recovery = snapshot.availability == UpdateAvailability.IN_PROGRESS
                    if (!allowNewPrompts && !recovery) return@launch
                    val allowed = if (recovery) !recoveryDeclined else
                        prompts.reserveUpdatePrompt(snapshot.version, snapshot.priority, System.currentTimeMillis(), flow)
                    if (allowed && ready) {
                        updateRunning = true
                        mutableState.update { it.copy(prompt = PlayPrompt.Update(info, flow)) }
                    }
                } else if (allowNewPrompts && (snapshot == null || snapshot.availability == UpdateAvailability.NONE) &&
                    state.value.installState == UpdateInstallState.IDLE &&
                    requestReview(System.currentTimeMillis()) && ready) {
                    mutableState.update { it.copy(prompt = PlayPrompt.Review, reviewRunning = true) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: java.io.IOException) { /* Never nag when the cooldown cannot be saved. */ }
        }
    }

    /** Called on the main thread immediately before launch: rotation cannot replay the same token. */
    fun takePrompt(prompt: PlayPrompt): Boolean {
        if (!ready || state.value.prompt !== prompt) return false
        mutableState.update { it.copy(prompt = null) }
        return true
    }

    fun updateResult(accepted: Boolean) {
        updateRunning = false
        // A restored Activity Result can arrive before this process has fetched any offer.
        // Respect that cancellation too, rather than needing an in-memory version number.
        recoveryDeclined = !accepted
        // Respect cancellation/failure; the next foreground check recovers downloads, not a retry loop.
    }

    fun reviewFinished() {
        mutableState.update { it.copy(reviewRunning = false) }
        // Do not record "reviewed", award anything, or show a success/error message.
    }

    fun installDownloadedUpdate() {
        if (state.value.installState != UpdateInstallState.DOWNLOADED || state.value.completingUpdate) return
        mutableState.update { it.copy(completingUpdate = true, installFailed = false) }
        viewModelScope.launch {
            try {
                updates.complete()
                mutableState.update { it.copy(installState = UpdateInstallState.INSTALLING) }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(installFailed = true) } }
            finally { mutableState.update { it.copy(completingUpdate = false) } }
        }
    }
}
