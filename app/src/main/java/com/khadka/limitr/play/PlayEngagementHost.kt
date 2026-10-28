package com.khadka.limitr.play

import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.khadka.limitr.R
import com.khadka.limitr.utils.AcmeTypography
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Activity-owned SDK UI only. Repositories and the retained ViewModel never retain an Activity. */
class PlayEngagementHost(
    private val activity: AppCompatActivity,
    private val root: View,
    private val model: PlayEngagementViewModel,
    private val updates: PlayUpdateRepository,
    private val reviews: PlayReviewRepository,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    private val allowNewPrompts: Boolean,
    private val homeReady: () -> Boolean
) : DefaultLifecycleObserver {
    private var readinessJob: Job? = null
    private var snackbar: Snackbar? = null
    private var snackbarFailed = false
    private var restartDismissed = false

    init {
        activity.lifecycle.addObserver(this)
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.state.collect { render() } }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        restartDismissed = false
        model.onForeground(allowNewPrompts)
        refreshReadiness()
    }

    override fun onPause(owner: LifecycleOwner) {
        readinessJob?.cancel()
        model.setReady(false)
        dismissSnackbar()
    }

    override fun onDestroy(owner: LifecycleOwner) { dismissSnackbar() }

    fun refreshReadiness() {
        readinessJob?.cancel()
        model.setReady(false)
        readinessJob = activity.lifecycleScope.launch {
            // Let Home's startup/dialog transactions settle before presenting any Play UI.
            delay(300)
            model.setReady(isReady())
            render()
        }
    }

    private fun isReady() = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
        activity.hasWindowFocus() && !activity.isFinishing && homeReady()

    private fun render() {
        if (!isReady()) { dismissSnackbar(); return }
        val state = model.state.value
        val prompt = state.prompt
        if (prompt != null && model.takePrompt(prompt)) {
            dismissSnackbar()
            when (prompt) {
                is PlayPrompt.Update -> {
                    try {
                        if (!updates.launch(prompt.info, prompt.flow, launcher)) model.updateResult(false)
                    } catch (_: Exception) { model.updateResult(false) }
                }
                PlayPrompt.Review -> activity.lifecycleScope.launch {
                    try {
                        val info = reviews.request() // Short-lived token: acquire only at the eligible moment.
                        if (isReady()) reviews.launch(activity, info)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* Play may show nothing; keep the normal app flow. */ }
                    finally { model.reviewFinished() }
                }
            }
            return
        }
        if (state.installState == UpdateInstallState.DOWNLOADED && !state.reviewRunning &&
            !state.completingUpdate && !restartDismissed) {
            if (snackbar != null && snackbarFailed == state.installFailed) return
            dismissSnackbar()
            snackbarFailed = state.installFailed
            snackbar = Snackbar.make(root,
                if (state.installFailed) R.string.play_update_install_failed else R.string.play_update_downloaded,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.play_update_restart) { model.installDownloadedUpdate() }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(bar: Snackbar?, event: Int) {
                        if (event == DISMISS_EVENT_SWIPE) restartDismissed = true
                        if (snackbar === bar) snackbar = null
                    }
                }).also {
                    AcmeTypography.applyTree(it.view)
                    it.show()
                }
        } else dismissSnackbar()
    }

    private fun dismissSnackbar() { snackbar?.dismiss(); snackbar = null }
}

/** Call during onCreate, before STARTED, so the Activity Result registration survives recreation. */
fun AppCompatActivity.attachPlayEngagement(
    root: View,
    updates: PlayUpdateRepository,
    reviews: PlayReviewRepository,
    guard: PlayUiGuard,
    allowNewPrompts: Boolean = true,
    ready: () -> Boolean
): PlayEngagementHost {
    val model = ViewModelProvider(this)[PlayEngagementViewModel::class.java]
    val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        model.updateResult(it.resultCode == android.app.Activity.RESULT_OK)
    }
    val host = PlayEngagementHost(this, root, model, updates, reviews, launcher, allowNewPrompts) {
        guard.canPresent() && ready()
    }
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) { guard.availability.collect { host.refreshReadiness() } }
    }
    return host
}
