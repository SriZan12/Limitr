package com.khadka.limitr

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.blocking.*
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import com.khadka.limitr.services.notifications.BlockingAlerts
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BlockingAlertsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, LimitrDatabase::class.java).build()
    private val clock = SessionClock(context)
    private val repository = SessionRepository(database, clock, context.getSharedPreferences("alert_test", Context.MODE_PRIVATE))
    private val manager = context.getSystemService(NotificationManager::class.java)
    private fun coordinator() = SessionCoordinator(context, repository, clock)
    private fun posted() = manager.activeNotifications.single().notification
    @After fun close() { database.close() }

    @Test fun startsOnceAndEndsOnceWithoutAnActivityOrAccessibilityService() = runBlocking {
        val coordinator = coordinator()
        repository.timer("test.app", "Test", 2000, false)
        coordinator.refresh()
        assertTrue(posted().extras.getBoolean("limitr.blocking.active"))
        assertEquals("Blocking started for Test", posted().extras.getString("android.title"))
        assertEquals(context.getString(R.string.blocking_started_unavailable), posted().extras.getString("android.text"))
        manager.cancelAll()
        coordinator.refresh()
        coordinator().refresh() // Process-like reconstruction still remembers this revision.
        assertTrue(manager.activeNotifications.isEmpty())
        ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
        coordinator.refresh()
        assertFalse(posted().extras.getBoolean("limitr.blocking.active"))
        assertEquals("Test blocking ended", posted().extras.getString("android.title"))
        manager.cancelAll()
        coordinator().refresh()
        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test fun cancellationClearsStartWithoutReportingNaturalExpiry() = runBlocking {
        repository.timer("test.app", "Test", 60000, false)
        val coordinator = coordinator()
        coordinator.refresh()
        repository.cancel(repository.all().single())
        coordinator.refresh()
        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test fun futureIntervalWaitsForItsStartAndReplacementCannotEndTheNewBlock() = runBlocking {
        val now = clock.now().wall
        val coordinator = coordinator()
        repository.interval("test.app", "Test", now + 60000, now + 120000, false)
        coordinator.refresh()
        assertTrue(manager.activeNotifications.isEmpty())
        val started = clock.now().copy(wall = now + 61000)
        val reconciled = repository.reconcileWithExpired(started)
        BlockingAlerts(context).reconcile(reconciled.sessions, reconciled.expired, started)
        assertTrue(posted().extras.getBoolean("limitr.blocking.active"))
        repository.timer("test.app", "Replacement", 60000, false)
        coordinator.refresh()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        coordinator.refresh()
        assertEquals("Blocking started for Replacement", posted().extras.getString("android.title"))
        assertTrue(posted().extras.getBoolean("limitr.blocking.active"))
    }

    @Test fun lateRecoveryReportsExpiryFromPersistedRows() = runBlocking {
        repository.timer("test.app", "Test", 1000, false)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
        coordinator().refresh()
        assertEquals("Test blocking ended", posted().extras.getString("android.title"))
        assertTrue(repository.all().isEmpty())
    }

    @Test fun disabledNotificationsDoNotPreventBlockingAndGrantCanPublishCurrentStart() = runBlocking {
        shadowOf(manager).setNotificationsEnabled(false)
        repository.timer("test.app", "Test", 60000, false)
        val coordinator = coordinator()
        coordinator.refresh()
        assertTrue(coordinator.state.value.loaded)
        assertFalse(coordinator.state.value.error)
        assertEquals(1, repository.all().size)
        assertTrue(manager.activeNotifications.isEmpty())
        shadowOf(manager).setNotificationsEnabled(true)
        coordinator.refresh()
        assertTrue(posted().extras.getBoolean("limitr.blocking.active"))
    }

    @Test fun legacyCleanupDoesNotDeleteNewAlerts() = runBlocking {
        repository.timer("test.app", "Test", 60000, false)
        coordinator().refresh()
        com.khadka.limitr.baseapplication.LegacyNotificationCleanup.removeBlockingAlerts(context)
        assertEquals(BlockingAlerts.CHANNEL, posted().channelId)
        assertNotNull(manager.getNotificationChannel(BlockingAlerts.CHANNEL))
    }
}
