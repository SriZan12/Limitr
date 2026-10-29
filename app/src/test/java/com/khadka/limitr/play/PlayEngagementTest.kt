package com.khadka.limitr.play

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.review.testing.FakeReviewManager
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewInfo
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.TaskCompletionSource
import com.khadka.limitr.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class PlayEngagementTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private lateinit var fake: FakeAppUpdateManager
    private lateinit var updates: PlayUpdateRepository
    private lateinit var prompts: PlayPromptRepository
    private var modelNumber = 0
    private val launcher = object : ActivityResultLauncher<IntentSenderRequest>() {
        override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
        override val contract = ActivityResultContracts.StartIntentSenderForResult()
    }

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        fake = FakeAppUpdateManager(ApplicationProvider.getApplicationContext())
        updates = PlayUpdateRepository(fake)
        prompts = PlayPromptRepository(PromptTestStore())
    }

    @After fun cleanup() { models.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }

    private fun model(): PlayEngagementViewModel = PlayEngagementViewModel(updates, prompts,
        SelectUpdateFlow(), RequestReviewIfEligible(prompts, ReviewEligibility())).also {
        models.put("model-${modelNumber++}", it)
    }
    private fun TestScope.settle() { repeat(3) { runCurrent(); shadowOf(Looper.getMainLooper()).idle() } }

    @Test fun startupGateAndSingleUseTokenPreventDuplicateLaunchOnRecollection() = runTest(dispatcher) {
        fake.setUpdateAvailable(20)
        val model = model()
        model.onForeground()
        settle()
        assertNull(model.state.value.prompt)
        model.setReady(true)
        settle()
        val request = model.state.value.prompt as PlayPrompt.Update
        assertEquals(UpdateFlow.FLEXIBLE, request.flow)
        assertTrue(model.takePrompt(request))
        assertFalse(model.takePrompt(request))
        assertTrue(updates.launch(request.info, request.flow, launcher))
        assertTrue(fake.isConfirmationDialogVisible)
    }

    @Test fun cancellationDoesNotRePromptAfterForegroundOrViewModelRecreation() = runTest(dispatcher) {
        fake.setUpdateAvailable(20)
        fake.setUpdatePriority(5)
        val model = model()
        model.onForeground(); model.setReady(true); settle()
        val request = model.state.value.prompt as PlayPrompt.Update
        assertEquals(UpdateFlow.IMMEDIATE, request.flow)
        model.takePrompt(request)
        updates.launch(request.info, request.flow, launcher)
        fake.userRejectsUpdate(); model.updateResult(false)
        model.onForeground(); settle()
        assertNull(model.state.value.prompt)
        models.clear()
        val recreated = model()
        recreated.onForeground(); recreated.setReady(true); settle()
        assertNull(recreated.state.value.prompt)
    }

    @Test fun flexibleDownloadRequiresExplicitInstallAndRecoversWhenListenerWasAbsent() = runTest(dispatcher) {
        fake.setUpdateAvailable(20)
        val model = model()
        model.onForeground(); model.setReady(true); settle()
        val request = model.state.value.prompt as PlayPrompt.Update
        model.takePrompt(request)
        updates.launch(request.info, request.flow, launcher)
        fake.userAcceptsUpdate(); model.updateResult(true)
        fake.downloadStarts(); settle()
        assertEquals(UpdateInstallState.DOWNLOADING, model.state.value.installState)
        model.onForeground(); settle()
        assertNull("An in-progress flexible download must not become immediate", model.state.value.prompt)
        models.clear(); settle()
        fake.downloadCompletes(); settle()
        // The cleared collector must not receive the missed event.
        assertEquals(UpdateInstallState.DOWNLOADING, model.state.value.installState)
        val recreated = model()
        recreated.onForeground(); recreated.setReady(true); settle()
        assertEquals(UpdateInstallState.DOWNLOADED, recreated.state.value.installState)
        assertNull(recreated.state.value.prompt)
        assertFalse(fake.isInstallSplashScreenVisible)
        recreated.installDownloadedUpdate(); settle()
        assertTrue(fake.isInstallSplashScreenVisible)
    }

    @Test fun interruptedImmediateUpdateResumesEvenDuringPromptCooldownAndOnDetails() = runTest(dispatcher) {
        fake.setUpdateAvailable(20)
        fake.setUpdatePriority(5)
        prompts.reserveUpdatePrompt(20, 5, System.currentTimeMillis(), UpdateFlow.IMMEDIATE)
        updates.launch(updates.check(), UpdateFlow.IMMEDIATE, launcher)
        fake.userAcceptsUpdate(); fake.downloadStarts()
        val model = model()
        model.onForeground(allowNewPrompts = false); model.setReady(true); settle()
        val request = model.state.value.prompt as PlayPrompt.Update
        assertEquals(UpdateFlow.IMMEDIATE, request.flow)
        assertTrue(model.takePrompt(request))
        // Google's 2.1.0 fake omits launch PendingIntents once IN_PROGRESS, so a second launch
        // returns false. Validate the recovery request and graceful failure; real UI needs Play.
        assertFalse(updates.launch(request.info, request.flow, launcher))
        assertTrue(fake.isImmediateFlowVisible)
        model.updateResult(false)
        model.onForeground(); settle()
        assertNull(model.state.value.prompt)
        models.clear(); settle()
        val restoredResult = model()
        restoredResult.updateResult(false) // Result delivery precedes the new process's first query.
        restoredResult.onForeground(); restoredResult.setReady(true); settle()
        assertNull(restoredResult.state.value.prompt)
    }

    @Test fun nonHomeScreensDoNotStartNewPrompts() = runTest(dispatcher) {
        fake.setUpdateAvailable(20)
        val model = model()
        model.onForeground(allowNewPrompts = false); model.setReady(true); settle()
        assertNull(model.state.value.prompt)
    }

    @Test fun installFailureKeepsDownloadedUpdateAvailableForUserRetry() = runTest(dispatcher) {
        fake.setUpdateAvailable(20, AppUpdateType.FLEXIBLE)
        updates.launch(updates.check(), UpdateFlow.FLEXIBLE, launcher)
        fake.userAcceptsUpdate(); fake.downloadStarts(); fake.downloadCompletes()
        val model = model()
        model.onForeground(); model.setReady(true); settle()
        fake.setInstallErrorCode(InstallErrorCode.ERROR_INTERNAL_ERROR)
        model.installDownloadedUpdate(); settle()
        assertTrue(model.state.value.installFailed)
        assertFalse(model.state.value.completingUpdate)
        assertEquals(UpdateInstallState.DOWNLOADED, model.state.value.installState)
        fake.setInstallErrorCode(InstallErrorCode.NO_ERROR)
        model.installDownloadedUpdate(); settle()
        assertTrue(fake.isInstallSplashScreenVisible)
    }

    @Test fun eligibleReviewCompletesWithoutMarkingUserAsReviewedOrRequestingAgain() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        prompts.recordUse(now - 8 * DAY_MILLIS)
        for (day in 2 downTo 0) prompts.recordBlockingSuccess(now - day * DAY_MILLIS)
        val model = model()
        model.onForeground(); model.setReady(true); settle()
        assertEquals(PlayPrompt.Review, model.state.value.prompt)
        assertTrue(model.takePrompt(PlayPrompt.Review))
        val reviews = PlayReviewRepository(FakeReviewManager(ApplicationProvider.getApplicationContext()))
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try { reviews.launch(activity.get(), reviews.request()) } finally { activity.pause().stop().destroy() }
        model.reviewFinished()
        assertFalse(model.state.value.reviewRunning)
        assertTrue(prompts.reviewHistory.first().lastAttemptAt > 0)
        model.onForeground(); model.setReady(true); settle()
        assertNull(model.state.value.prompt)
    }

    @Test fun playUnavailableDoesNotBreakHomeOrProduceUpdateUi() = runTest(dispatcher) {
        fake.setInstallErrorCode(InstallErrorCode.ERROR_API_NOT_AVAILABLE)
        val model = model()
        model.onForeground(); model.setReady(true); settle()
        assertNull(model.state.value.prompt)
        assertEquals(UpdateInstallState.IDLE, model.state.value.installState)
    }

    @Test fun restartSnackbarWaitsForAnUnobstructedScreenAndRequiresATap() = runTest(dispatcher) {
        fake.setUpdateAvailable(20, AppUpdateType.FLEXIBLE)
        updates.launch(updates.check(), UpdateFlow.FLEXIBLE, launcher)
        fake.userAcceptsUpdate(); fake.downloadStarts(); fake.downloadCompletes()
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Limitr)
        controller.setup().visible().windowFocusChanged(true)
        val activity = controller.get()
        val root = FrameLayout(activity)
        activity.setContentView(root)
        var unobstructed = false
        val model = model()
        val host = PlayEngagementHost(activity, root, model, updates,
            PlayReviewRepository(FakeReviewManager(activity)), launcher, true) { unobstructed }
        try {
            settle(); advanceTimeBy(350); settle()
            assertNull(activity.findViewById<View>(com.google.android.material.R.id.snackbar_action))
            unobstructed = true; host.refreshReadiness()
            advanceTimeBy(350); settle()
            val action = activity.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
            assertNotNull("Downloaded update must offer restart after onboarding closes", action)
            assertEquals(activity.getString(R.string.play_update_restart), action.text.toString())
            assertFalse(fake.isInstallSplashScreenVisible)
            action.performClick(); settle()
            assertTrue(fake.isInstallSplashScreenVisible)
        } finally { controller.pause().stop().destroy(); settle() }
    }

    @Test fun leavingHomeDropsAnUnlaunchedReviewInsteadOfShowingItOnALaterVisit() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        prompts.recordUse(now - 8 * DAY_MILLIS)
        for (day in 2 downTo 0) prompts.recordBlockingSuccess(now - day * DAY_MILLIS)
        val model = model()
        model.onForeground(); model.setReady(true); settle()
        assertEquals(PlayPrompt.Review, model.state.value.prompt)
        model.setReady(false)
        assertNull(model.state.value.prompt)
        assertFalse(model.state.value.reviewRunning)
        model.onForeground(); model.setReady(true); settle()
        assertNull(model.state.value.prompt)
    }

    @Test fun reviewRequestFailureReleasesHostSilentlyAndKeepsCooldown() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        prompts.recordUse(now - 8 * DAY_MILLIS)
        for (day in 2 downTo 0) prompts.recordBlockingSuccess(now - day * DAY_MILLIS)
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Limitr)
        controller.setup().visible().windowFocusChanged(true)
        val activity = controller.get()
        val root = FrameLayout(activity)
        activity.setContentView(root)
        var requests = 0
        val failing = object : ReviewManager by FakeReviewManager(activity) {
            override fun requestReviewFlow() = Tasks.forException<ReviewInfo>(
                IllegalStateException("Simulated Play unavailability")
            ).also { requests++ }
        }
        val model = model()
        PlayEngagementHost(activity, root, model, updates, PlayReviewRepository(failing), launcher, true) { true }
        try {
            settle(); advanceTimeBy(350); settle()
            assertEquals(1, requests)
            assertFalse(model.state.value.reviewRunning)
            assertNull(model.state.value.prompt)
            assertTrue(prompts.reviewHistory.first().lastAttemptAt > 0)
            assertNull(activity.findViewById<View>(com.google.android.material.R.id.snackbar_text))
            model.onForeground(); model.setReady(true); settle()
            assertEquals(1, requests)
        } finally { controller.pause().stop().destroy(); settle() }
    }

    @Test fun lateReviewResponseMustNotLaunchAfterLeavingAndReturningHome() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        prompts.recordUse(now - 8 * DAY_MILLIS)
        for (day in 2 downTo 0) prompts.recordBlockingSuccess(now - day * DAY_MILLIS)
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Limitr)
        controller.setup().visible().windowFocusChanged(true)
        val activity = controller.get()
        val root = FrameLayout(activity)
        activity.setContentView(root)
        val delegate = FakeReviewManager(activity)
        val response = TaskCompletionSource<ReviewInfo>()
        var launches = 0
        val delayed = object : ReviewManager by delegate {
            override fun requestReviewFlow() = response.task
            override fun launchReviewFlow(activity: Activity, info: ReviewInfo) =
                delegate.launchReviewFlow(activity, info).also { launches++ }
        }
        val model = model()
        PlayEngagementHost(activity, root, model, updates, PlayReviewRepository(delayed), launcher, true) { true }
        try {
            settle(); advanceTimeBy(350); settle()
            assertTrue(model.state.value.reviewRunning)
            controller.pause().stop()
            controller.start().resume().visible().windowFocusChanged(true)
            advanceTimeBy(350); settle()
            response.setResult(delegate.requestReviewFlow().result)
            settle()
            assertEquals("A stale Play response must not surprise the user on a later visit", 0, launches)
            assertFalse(model.state.value.reviewRunning)
        } finally { controller.pause().stop().destroy(); settle() }
    }

    @Test fun pausingForAnAlreadyLaunchedReviewDoesNotEndItsFlowEarly() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        prompts.recordUse(now - 8 * DAY_MILLIS)
        for (day in 2 downTo 0) prompts.recordBlockingSuccess(now - day * DAY_MILLIS)
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Limitr)
        controller.setup().visible().windowFocusChanged(true)
        val activity = controller.get()
        val root = FrameLayout(activity)
        activity.setContentView(root)
        val completion = TaskCompletionSource<Void>()
        var launches = 0
        val manager = object : ReviewManager by FakeReviewManager(activity) {
            override fun launchReviewFlow(activity: Activity, info: ReviewInfo) =
                completion.task.also { launches++ }
        }
        val model = model()
        PlayEngagementHost(activity, root, model, updates, PlayReviewRepository(manager), launcher, true) { true }
        try {
            settle(); advanceTimeBy(350); settle()
            assertEquals(1, launches)
            controller.pause().stop(); settle()
            assertTrue("Only Play completion may finish an already launched review", model.state.value.reviewRunning)
            completion.setResult(null); settle()
            assertFalse(model.state.value.reviewRunning)
        } finally { controller.destroy(); settle() }
    }
}
