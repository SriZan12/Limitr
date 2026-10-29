package com.khadka.limitr

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.blocking.*
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SessionRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, LimitrDatabase::class.java).build()
    private val clock = SessionClock(context)
    private val repository = SessionRepository(db, clock, context.getSharedPreferences("My SharePref", Context.MODE_PRIVATE))
    @After fun close() { db.close() }

    @Test fun staleCancellationDoesNotDeleteReplacement() = runBlocking {
        repository.timer("a", "Shared name", 10000, false)
        val original = repository.all().single()
        repository.timer("a", "Shared name", 20000, false)
        assertFalse(repository.cancel(original))
        val replacement = repository.all().single()
        assertNotEquals(original.revision, replacement.revision)
        assertTrue(repository.cancel(replacement))
        assertFalse(repository.cancel(replacement))
        assertTrue(repository.reconcile(clock.now()).isEmpty())
    }
    @Test fun duplicateLabelsAreIndependent() = runBlocking {
        repository.timer("a", "Same", 10000, false)
        repository.timer("b", "Same", 10000, true)
        assertEquals(2, repository.all().size)
        repository.timer("a", "Same", 20000, false)
        val history = db.getLimitrDao().history().first().associateBy { it.appPackage }
        assertEquals(2, history.size)
        assertEquals(2, history.getValue("a").noOfTimesBlocked)
        assertEquals(1, history.getValue("b").noOfTimesBlocked)
    }
    @Test fun expiredRowsAreRemovedAndClockRollbackCannotReviveThem() = runBlocking {
        repository.interval("a", "App", 1000, 2000, false)
        val sample = clock.now()
        assertTrue(repository.reconcile(sample.copy(wall = 2000)).isEmpty())
        assertTrue(repository.reconcile(sample.copy(wall = 1500)).isEmpty())
    }
    @Test fun legacyTimerRebasesOnceAndImportsNotificationPreference() = runBlocking {
        val sample = clock.now()
        context.getSharedPreferences("My SharePref", Context.MODE_PRIVATE).edit().putBoolean("A", true).commit()
        db.sessions().save(BlockingSession("a", "A", "legacy:a", "timer", sample.wall - 1000,
            sample.wall + 9000, null, 10000, null))
        val restored = repository.reconcile(sample).single()
        assertEquals("imported:a", restored.revision)
        assertTrue(restored.blockNotifications)
        repository.notifications(restored, false)
        assertFalse(repository.reconcile(sample).single().blockNotifications)
        assertEquals(sample.bootId, restored.bootId)
        assertEquals(9000, restored.duration)
        assertEquals(8000, ScheduleEvaluator.evaluate(restored, sample.copy(wall = sample.wall - 100000, elapsed = sample.elapsed + 1000)).remaining)
    }
    @Test fun oldNotificationToggleDoesNotMutateReplacement() = runBlocking {
        repository.timer("a", "A", 10000, false)
        val old = repository.all().single()
        repository.timer("a", "A", 20000, false)
        repository.notifications(old, true)
        assertFalse(repository.all().single().blockNotifications)
    }
    @Test fun debitRecoveryKeepsOriginalTimingAndHistoryAndDoesNotReplaceNewerTimer() = runBlocking {
        repository.timer("a", "A", 60000, true)
        val original = repository.all().single()
        val history = db.getLimitrDao().history().first()
        repository.cancel(original)
        repository.restoreIfAbsent(original)
        assertEquals(listOf(original), repository.all())
        assertEquals(history, db.getLimitrDao().history().first())
        repository.timer("a", "A", 120000, false)
        val replacement = repository.all().single()
        repository.restoreIfAbsent(original)
        assertEquals(listOf(replacement), repository.all())
    }
    @Test fun debitRecoveryDoesNotRestoreAnExpiredInterval() = runBlocking {
        repository.interval("a", "A", 1000, 2000, false)
        val expired = repository.all().single()
        repository.cancel(expired)
        repository.restoreIfAbsent(expired)
        assertTrue(repository.all().isEmpty())
    }
}
