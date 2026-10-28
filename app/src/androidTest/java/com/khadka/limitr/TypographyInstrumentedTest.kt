package com.khadka.limitr

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import android.widget.TimePicker
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.allViews
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.khadka.limitr.ui.blocker.activity.BlockAppActivity
import com.khadka.limitr.ui.mainactivity.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TypographyInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun assertAcme(root: View) {
        val font = ResourcesCompat.getFont(context, R.font.acme)!!
        val texts = root.allViews.filterIsInstance<TextView>().filter { it.text.isNotEmpty() }.toList()
        assertTrue("No text inspected", texts.isNotEmpty())
        texts.forEach { assertEquals("Non-Acme text: ${it.text} (${it.javaClass.simpleName})",
            Typeface.create(font, it.typeface.style), it.typeface) }
    }
    private fun dialog(activity: BlockAppActivity) = BlockAppActivity::class.java.getDeclaredField("dialog")
        .apply { isAccessible = true }.get(activity) as Dialog

    @Test fun homeTabsAndLoadedRowsUseAcmeAndCacheSurvivesRecreation(): Unit = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            withTimeout(15000) {
                var ready = false
                while (!ready) {
                    scenario.onActivity { ready = it.apps.state.value.rows != null && it.findViewById<View>(R.id.app_name_tv) != null }
                    if (!ready) delay(100)
                }
            }
            scenario.onActivity { assertAcme(it.window.decorView) }
            scenario.recreate()
            scenario.onActivity {
                assertNotNull(it.apps.state.value.rows)
                assertEquals(View.GONE, it.findViewById<View>(R.id.appListSkeleton).visibility)
                assertAcme(it.window.decorView)
            }
        }
    }

    @Test fun detailsAndBothPickerTypesUseBundledAcme(): Unit = runBlocking {
        EntryPointAccessors.fromApplication(context, TestSessions::class.java).coordinator().refresh()
        ActivityScenario.launch<BlockAppActivity>(Intent(context, BlockAppActivity::class.java)
            .putExtra("packageName", "com.limitr.typography.fixture")).use { scenario ->
            scenario.onActivity { activity ->
                assertAcme(activity.window.decorView)
                activity.findViewById<View>(R.id.setTime).performClick()
                assertAcme(dialog(activity).window!!.decorView)
                dialog(activity).dismiss()
                activity.findViewById<View>(R.id.setInterval).performClick()
                val root = dialog(activity).window!!.decorView
                assertAcme(root)
                val picker = root.allViews.filterIsInstance<TimePicker>().single()
                assertFalse("Clock face hardcodes a different typeface", picker.allViews.any { it.javaClass.simpleName == "RadialTimePickerView" })
            }
            scenario.recreate()
            scenario.onActivity { assertAcme(dialog(it).window!!.decorView) }
        }
    }
}
