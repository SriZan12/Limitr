package com.khadka.limitr

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.inspector.WindowInspector
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.TimePicker
import androidx.core.view.allViews
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class PickerRecreationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val entry = EntryPointAccessors.fromApplication(context, TestSessions::class.java)
    private val target = "com.limitr.picker.fixture"
    private fun launch() = ActivityScenario.launch<BlockAppActivity>(Intent(context, BlockAppActivity::class.java)
        .putExtra("packageName", target))
    private fun pickerRoot() = WindowInspector.getGlobalWindowViews().firstOrNull { root -> root.allViews.any { it is TimePicker } }
    private fun timePicker() = pickerRoot()?.allViews?.filterIsInstance<TimePicker>()?.firstOrNull()

    @After fun cleanup(): Unit = runBlocking {
        entry.repository().all().filter { it.packageName == target }.forEach { entry.repository().cancel(it) }
        entry.coordinator().refresh()
    }

    @Test fun timerKeyboardInputSurvivesActivityRecreation() = runBlocking {
        entry.coordinator().refresh()
        launch().use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.setTime).performClick()
            }
            // Let the dialog acquire window focus before editing its input, as a user would.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                val root = WindowInspector.getGlobalWindowViews().first { it.findViewById<NumberPicker>(R.id.minute_picker) != null }
                val picker = root.findViewById<NumberPicker>(R.id.minute_picker)
                val input = picker.allViews.filterIsInstance<EditText>().single()
                input.requestFocus()
                input.setText("17")
                assertEquals("17", input.text.toString())
            }
            scenario.recreate()
            scenario.onActivity {
                val root = WindowInspector.getGlobalWindowViews().firstOrNull { it.findViewById<NumberPicker>(R.id.minute_picker) != null }
                assertNotNull("Timer editor disappeared after recreation", root)
                assertEquals(17, root!!.findViewById<NumberPicker>(R.id.minute_picker).value)
                root.findViewById<View>(R.id.buttonStartTime).performClick()
            }
            awaitSaved()
            assertEquals(17 * 60000L, entry.repository().all().single { it.packageName == target }.duration)
        }
    }

    @Test fun intervalStartEndAndConfirmationSurviveActivityRecreation() = runBlocking {
        entry.coordinator().refresh()
        launch().use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.setInterval).performClick()
                timePicker()!!.apply { hour = 10; minute = 15 }
            }
            scenario.recreate()
            scenario.onActivity {
                assertNotNull("Start-time editor disappeared after recreation", timePicker())
                assertEquals(10, timePicker()!!.hour)
                assertEquals(15, timePicker()!!.minute)
                pickerRoot()!!.findViewById<View>(android.R.id.button1).performClick()
                timePicker()!!.apply { hour = 11; minute = 45 }
            }
            scenario.recreate()
            var confirmation = ""
            scenario.onActivity {
                assertNotNull("End-time editor disappeared after recreation", timePicker())
                assertEquals(11, timePicker()!!.hour)
                assertEquals(45, timePicker()!!.minute)
                pickerRoot()!!.findViewById<View>(android.R.id.button1).performClick()
                confirmation = confirmationRoot()!!.findViewById<TextView>(android.R.id.message).text.toString()
            }
            scenario.recreate()
            scenario.onActivity {
                assertNotNull("Interval confirmation disappeared after recreation", confirmationRoot())
                assertEquals(confirmation, confirmationRoot()!!.findViewById<TextView>(android.R.id.message).text.toString())
                confirmationRoot()!!.findViewById<View>(android.R.id.button1).performClick()
            }
            awaitSaved()
            val saved = entry.repository().all().single { it.packageName == target }
            assertEquals("interval", saved.kind)
            assertEquals(90 * 60000L, saved.duration)
        }
    }

    private fun confirmationRoot() = WindowInspector.getGlobalWindowViews().firstOrNull { it.findViewById<TextView>(android.R.id.message) != null }
    private suspend fun awaitSaved() = kotlinx.coroutines.withTimeout(10000) {
        while (entry.repository().all().none { it.packageName == target }) kotlinx.coroutines.delay(50)
    }
}
