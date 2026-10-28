package com.khadka.limitr

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khadka.limitr.services.notifications.NotificationPreferences
import com.khadka.limitr.blocking.CryptoUnblocker
import com.khadka.limitr.blocking.SessionClock
import com.khadka.limitr.blocking.SessionCoordinator
import com.khadka.limitr.blocking.SessionRepository
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.ArrayDeque
import java.util.concurrent.Executor
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.khadka.limitr.repository.MainFragmentRepository
import com.khadka.limitr.ui.blocker.vm.SessionViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SessionCoordinatorTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun cancelledViewModelOperationDoesNotDisplayStorageFailure() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LimitrDatabase::class.java).build()
        val clock = SessionClock(context)
        val repository = SessionRepository(database, clock, context.getSharedPreferences("test", Context.MODE_PRIVATE))
        val store = object : DataStore<Preferences> {
            override val data = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(emptyPreferences())
        }
        val rewards = MainFragmentRepository(store)
        try {
            val model = SessionViewModel(repository, SessionCoordinator(context, repository, clock), clock, CryptoUnblocker(repository, rewards), NotificationPreferences(store))
            model.act { throw CancellationException("ViewModel cleared") }
            assertNull(model.error.value)
            assertFalse(model.busy.value)
        } finally { database.close(); Dispatchers.resetMain() }
    }

    @Test fun disconnectWhileWaitingForRoomDoesNotPublishStorageFailure() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val queued = ArrayDeque<Runnable>()
        val executor = Executor { queued.add(it) }
        val database = Room.inMemoryDatabaseBuilder(context, LimitrDatabase::class.java)
            .setTransactionExecutor(executor).setQueryExecutor(executor).build()
        val clock = SessionClock(context)
        val repository = SessionRepository(database, clock, context.getSharedPreferences("test", Context.MODE_PRIVATE))
        val coordinator = SessionCoordinator(context, repository, clock)
        try {
            val refresh = launch(start = CoroutineStart.UNDISPATCHED) { coordinator.refresh() }
            assertFalse("Room transaction must be pending", queued.isEmpty())
            refresh.cancelAndJoin()
            assertFalse("Lifecycle cancellation is not storage corruption", coordinator.state.value.error)
            assertFalse(coordinator.state.value.loaded)
        } finally {
            while (queued.isNotEmpty()) queued.poll().run()
            database.close()
        }
    }
}
