package com.khadka.limitr

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SessionCoordinatorTest {
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
