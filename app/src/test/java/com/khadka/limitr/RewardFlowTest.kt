package com.khadka.limitr

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.khadka.limitr.repository.MainFragmentRepository
import com.khadka.limitr.utils.Constants.CRYPTO
import com.khadka.limitr.utils.Constants.LAST_LOGGED_DATE
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class RewardFlowTest {
    @get:Rule val folder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder.root, "rewards.preferences_pb") })
    }
    private val repository by lazy { MainFragmentRepository(store) }

    @After fun close(): Unit = runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }

    @Test fun showingWelcomeOfferDoesNotAwardAndRepeatedClaimsAwardOnce() = runBlocking {
        repeat(3) { assertEquals(15, repository.rewardOffer().first()) }
        assertEquals(0, repository.getCrypto().first())
        coroutineScope { repeat(20) { launch { repository.claimReward() } } }
        assertEquals(15, repository.getCrypto().first())
        assertNull(repository.rewardOffer().first())
    }

    @Test fun existingBalanceGetsDailyOfferAndTodaysClaimIsNotOfferedAgain() = runBlocking {
        store.edit { it[CRYPTO] = 7; it[LAST_LOGGED_DATE] = LocalDate.now().minusDays(1).toString() }
        assertEquals(1, repository.rewardOffer().first())
        repository.claimReward()
        assertEquals(8, repository.getCrypto().first())
        assertNull(repository.rewardOffer().first())
        repository.claimReward()
        assertEquals(8, repository.getCrypto().first())
    }
}
