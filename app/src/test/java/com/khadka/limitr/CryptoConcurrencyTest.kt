package com.khadka.limitr

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.services.notifications.NotificationPreferences
import com.khadka.limitr.blocking.CryptoUnblocker
import com.khadka.limitr.blocking.SessionClock
import com.khadka.limitr.blocking.SessionCoordinator
import com.khadka.limitr.blocking.SessionRepository
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import com.khadka.limitr.repository.MainFragmentRepository
import com.khadka.limitr.ui.blocker.vm.SessionViewModel
import com.khadka.limitr.utils.Constants.CRYPTO
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class CryptoConcurrencyTest {
    @Test fun twoScreensCannotUnblockTwoAppsWithOnlyTwoCrypto() = checkConcurrentUnblocks(2, 1, 0)
    @Test fun twoAffordableUnblocksEachDebitTheirCost() = checkConcurrentUnblocks(4, 0, 0)
    @Test fun failedDebitRestoresTheOriginalTimer() = checkInterruptedDebit(failWrite = true)
    @Test fun leavingDetailsDoesNotInterruptAnAlreadyStartedDebit() = checkInterruptedDebit(failWrite = false)

    private fun checkInterruptedDebit(failWrite: Boolean) = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LimitrDatabase::class.java).build()
        val clock = SessionClock(context)
        val repository = SessionRepository(database, clock, context.getSharedPreferences("qa", Context.MODE_PRIVATE))
        val coordinator = SessionCoordinator(context, repository, clock)
        val writeStarted = CompletableDeferred<Unit>()
        val continueWrite = CompletableDeferred<Unit>()
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(CRYPTO to 2))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                if (failWrite) throw java.io.IOException("Simulated disk write failure")
                writeStarted.complete(Unit)
                continueWrite.await()
                return transform(data.value).also { data.value = it }
            }
        }
        val rewards = MainFragmentRepository(store)
        val unblocker = CryptoUnblocker(repository, rewards)
        val models = ViewModelStore()
        try {
            repository.timer("qa.first", "First", 60000, true)
            val original = repository.all().single()
            val model = SessionViewModel(repository, coordinator, clock, unblocker, NotificationPreferences(store), com.khadka.limitr.play.RecordBlockingSuccess(com.khadka.limitr.play.PlayPromptRepository(store)))
            models.put("qa", model)
            model.unblockWithCrypto(original)
            if (!failWrite) {
                withTimeout(5000) { writeStarted.await() }
                models.clear()
                continueWrite.complete(Unit)
            }
            withTimeout(5000) { model.busy.first { !it } }
            if (failWrite) {
                assertNotNull(model.error.value)
                assertEquals(listOf(original), repository.all())
                assertEquals(2, rewards.getCrypto().first())
            } else {
                assertTrue(repository.all().isEmpty())
                assertEquals(0, rewards.getCrypto().first())
            }
        } finally {
            continueWrite.complete(Unit)
            models.clear()
            database.close()
            Dispatchers.resetMain()
        }
    }

    private fun checkConcurrentUnblocks(balance: Int, remainingApps: Int, remainingCrypto: Int) = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LimitrDatabase::class.java).build()
        val clock = SessionClock(context)
        val repository = SessionRepository(database, clock, context.getSharedPreferences("qa", Context.MODE_PRIVATE))
        val coordinator = SessionCoordinator(context, repository, clock)
        val writeStarted = CompletableDeferred<Unit>()
        val continueWrite = CompletableDeferred<Unit>()
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(CRYPTO to balance))
            private val mutex = Mutex()
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
                writeStarted.complete(Unit)
                continueWrite.await()
                transform(data.value).also { data.value = it }
            }
        }
        val rewards = MainFragmentRepository(store)
        val unblocker = CryptoUnblocker(repository, rewards)
        try {
            repository.timer("qa.first", "First", 60000, false)
            repository.timer("qa.second", "Second", 60000, false)
            val saved = repository.all()
            val first = SessionViewModel(repository, coordinator, clock, unblocker, NotificationPreferences(store), com.khadka.limitr.play.RecordBlockingSuccess(com.khadka.limitr.play.PlayPromptRepository(store)))
            val second = SessionViewModel(repository, coordinator, clock, unblocker, NotificationPreferences(store), com.khadka.limitr.play.RecordBlockingSuccess(com.khadka.limitr.play.PlayPromptRepository(store)))
            first.unblockWithCrypto(saved.single { it.packageName == "qa.first" })
            withTimeout(5000) { writeStarted.await() }
            // A second details screen requests an unblock while the first debit is still writing.
            second.unblockWithCrypto(saved.single { it.packageName == "qa.second" })
            continueWrite.complete(Unit)
            withTimeout(5000) {
                first.busy.first { !it }
                second.busy.first { !it }
            }
            assertEquals(remainingApps, repository.all().size)
            assertEquals(remainingCrypto, rewards.getCrypto().first())
        } finally {
            continueWrite.complete(Unit)
            database.close()
            Dispatchers.resetMain()
        }
    }
}
