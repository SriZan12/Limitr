package com.khadka.limitr

import com.khadka.limitr.repository.AppListRepository
import com.khadka.limitr.ui.home.model.App
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppListRepositoryTest {
    private val rows = listOf(App(appName = "Zulu", appPackageName = "z", usageDuration = "1 h 0 m", usagePercentage = 100.0),
        App(appName = "Alpha", appPackageName = "a", usageDuration = "0 h 0 m"))

    @Test fun preloadAndResumeShareOneReadAndRefreshKeepsVisibleRows() = runTest {
        var reads = 0
        var gate = CompletableDeferred<List<App>>()
        val repository = AppListRepository({ reads++; gate.await() }, { true }, backgroundScope)
        repository.preload(); runCurrent()
        repository.refresh(); runCurrent()
        assertEquals(1, reads)
        assertNull(repository.state.value.rows)
        gate.complete(rows); runCurrent()
        assertEquals(rows, repository.state.value.rows)
        gate = CompletableDeferred()
        repository.preload(); runCurrent()
        assertEquals(1, reads)
        repository.refresh(); runCurrent()
        assertEquals(2, reads)
        assertEquals(rows, repository.state.value.rows)
        gate.complete(rows.take(1)); runCurrent()
        assertEquals(rows.take(1), repository.state.value.rows)
    }

    @Test fun revocationClearsCachedUsageImmediatelyAndCannotBeOverwrittenByInflightRead() = runTest {
        var allowed = true
        var gate = CompletableDeferred(rows)
        val repository = AppListRepository({ gate.await() }, { allowed }, backgroundScope)
        repository.preload(); runCurrent()
        gate = CompletableDeferred()
        repository.refresh(); runCurrent()
        allowed = false
        repository.refresh()
        fun assertUnavailable() {
            assertEquals(listOf("Alpha", "Zulu"), repository.state.value.rows!!.map { it.appName })
            assertTrue(repository.state.value.rows!!.all { it.usageDuration == "—" && it.usagePercentage == 0.0 })
        }
        assertUnavailable()
        gate.complete(rows); runCurrent()
        assertUnavailable()
    }

    @Test fun failuresAreRetryableAndDoNotDiscardGoodRows() = runTest {
        var fail = true
        val repository = AppListRepository({ if (fail) error("Package service unavailable") else rows }, { true }, backgroundScope)
        repository.preload(); runCurrent()
        assertTrue(repository.state.value.failed)
        assertNull(repository.state.value.rows)
        fail = false
        repository.refresh(); runCurrent()
        assertFalse(repository.state.value.failed)
        assertEquals(rows, repository.state.value.rows)
        fail = true
        repository.refresh(); runCurrent()
        assertTrue(repository.state.value.failed)
        assertEquals(rows, repository.state.value.rows)
    }
}
