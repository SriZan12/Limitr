package com.khadka.limitr

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.inspector.WindowInspector
import android.widget.EditText
import android.widget.NumberPicker
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.khadka.limitr.blocking.*
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val entry = EntryPointAccessors.fromApplication(context, TestSessions::class.java)
    private val repository = entry.repository()
    private val coordinator = entry.coordinator()
    private val clock = entry.clock()
    private val target = "com.limitr.instrumented.fixture"

    @After fun cleanup(): Unit = runBlocking {
        repository.all().filter { it.packageName == target }.forEach { repository.cancel(it) }
        coordinator.refresh()
        Unit
    }

    @Test fun activityRecreationKeepsSessionAndExpiryIsAutomatic() = runBlocking {
        repository.timer(target, "Test app", 60000, false)
        coordinator.refresh()
        val revision = repository.all().first { it.packageName == target }.revision
        ActivityScenario.launch<BlockAppActivity>(Intent(context, BlockAppActivity::class.java).putExtra("packageName", target)).use {
            it.recreate()
            assertEquals(revision, repository.all().firstOrNull { it.packageName == target }?.revision)
            repository.timer(target, "Test app", 1000, false)
            coordinator.refresh()
            await { coordinator.state.value.sessions.none { it.packageName == target } }
        }
        assertTrue(repository.all().none { it.packageName == target })
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    fun keyboardTimerEntryIsCommittedWhenStartIsTapped() = runBlocking {
        coordinator.refresh()
        ActivityScenario.launch<BlockAppActivity>(Intent(context, BlockAppActivity::class.java).putExtra("packageName", target)).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<android.view.View>(R.id.setTime).performClick()
                val root = WindowInspector.getGlobalWindowViews().first { it.findViewById<NumberPicker>(R.id.second_picker) != null }
                val picker = root.findViewById<NumberPicker>(R.id.second_picker)
                val input = (0 until picker.childCount).map { picker.getChildAt(it) }.filterIsInstance<EditText>().single()
                input.requestFocus()
                input.setText("30")
                root.findViewById<android.view.View>(R.id.buttonStartTime).performClick()
            }
            await { coordinator.state.value.sessions.any { it.packageName == target && it.duration == 30000L } }
        }
    }

    @Test fun scheduledStartPublishesTransitionWithoutAWindowEvent() = runBlocking {
        val wall = clock.now().wall
        repository.interval(target, "Test app", wall + 1000, wall + 5000, false)
        coordinator.refresh()
        val initial = coordinator.state.value.sampledAt!!.wall
        await {
            val state = coordinator.state.value
            val sampled = state.sampledAt
            sampled != null && sampled.wall > initial && sampled.wall >= wall + 1000 &&
                ScheduleEvaluator.activeFor(state.sessions, target, sampled) != null
        }
    }

    @Test fun repeatedSessionsAndStaleReconciliationAreIdempotent() = runBlocking {
        repeat(30) {
            repository.timer(target, "Test app", 60000, false)
            val previous = repository.all().first { it.packageName == target }
            repository.timer(target, "Test app", 60000, false)
            assertFalse(repository.cancel(previous))
            val current = repository.all().first { it.packageName == target }
            assertTrue(repository.cancel(current))
        }
        coordinator.refresh()
        context.sendBroadcast(Intent(context, ReconcileReceiver::class.java).setAction("com.khadka.limitr.RECONCILE"))
        SystemClock.sleep(200)
        assertTrue(repository.all().none { it.packageName == target })
    }

    private fun await(predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10000
        while (!predicate() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
        assertTrue("Condition not reached within 10 seconds", predicate())
    }
}
