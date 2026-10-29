package com.khadka.limitr.play

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

internal class PromptTestStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = mutex.withLock {
        transform(data.value).also { data.value = it }
    }
}

class PlayPromptPolicyTest {
    private val policy = SelectUpdateFlow()
    private val available = UpdateSnapshot(20, 0, UpdateAvailability.AVAILABLE, UpdateInstallState.IDLE, true, true)
    private val now = 200 * DAY_MILLIS
    private val eligible = ReviewHistory(now - 7 * DAY_MILLIS, setOf("1", "2", "3"), now, 0)

    @Test fun prioritySelectsFlexibleOrImmediate() {
        for (priority in 0..3) assertEquals(UpdateFlow.FLEXIBLE, policy(available.copy(priority = priority)))
        for (priority in 4..5) assertEquals(UpdateFlow.IMMEDIATE, policy(available.copy(priority = priority)))
    }

    @Test fun onlyUseAllowedTypesAndDoNotEscalateMinorUpdates() {
        assertEquals(UpdateFlow.FLEXIBLE, policy(available.copy(priority = 5, immediateAllowed = false)))
        assertNull(policy(available.copy(flexibleAllowed = false)))
        assertNull(policy(available.copy(priority = 5, flexibleAllowed = false, immediateAllowed = false)))
        assertNull(policy(available.copy(availability = UpdateAvailability.NONE)))
    }

    @Test fun recoveryTakesPrecedenceAndDownloadsDoNotPromptAgain() {
        assertEquals(UpdateFlow.IMMEDIATE, policy(available.copy(availability = UpdateAvailability.IN_PROGRESS,
            installState = UpdateInstallState.DOWNLOADING)))
        assertNull(policy(available.copy(availability = UpdateAvailability.IN_PROGRESS,
            installState = UpdateInstallState.DOWNLOADING), UpdateFlow.FLEXIBLE))
        assertNull(policy(available.copy(availability = UpdateAvailability.IN_PROGRESS,
            installState = UpdateInstallState.DOWNLOADED)))
        for (state in listOf(UpdateInstallState.DOWNLOADING, UpdateInstallState.DOWNLOADED, UpdateInstallState.INSTALLING)) {
            assertNull(policy(available.copy(installState = state)))
        }
    }

    @Test fun reviewRequiresExperienceAndARecentPositiveAction() {
        val review = ReviewEligibility()
        assertTrue(review(eligible, now))
        assertFalse(review(eligible.copy(firstUseAt = now - 6 * DAY_MILLIS), now))
        assertFalse(review(eligible.copy(successfulDays = setOf("1", "2")), now))
        assertFalse(review(eligible.copy(positiveActionAt = 0), now))
        assertFalse(review(eligible.copy(positiveActionAt = now - 600_001), now))
        assertFalse(review(eligible.copy(positiveActionAt = now + 1), now))
    }

    @Test fun reviewCooldownIncludesClockRollback() {
        val review = ReviewEligibility()
        assertFalse(review(eligible.copy(lastAttemptAt = now - 89 * DAY_MILLIS), now))
        assertFalse(review(eligible.copy(lastAttemptAt = now + DAY_MILLIS), now))
        assertTrue(review(eligible.copy(lastAttemptAt = now - 90 * DAY_MILLIS), now))
    }

    @Test fun repeatedSavesInOneDayDoNotCreateExperienceAndHistoryStaysBounded() = runTest {
        val repo = PlayPromptRepository(PromptTestStore())
        repo.recordUse(now - 10 * DAY_MILLIS)
        repeat(20) { repo.recordBlockingSuccess(now - 2 * DAY_MILLIS + it) }
        assertEquals(1, repo.reviewHistory.first().successfulDays.size)
        assertFalse(repo.reserveReviewAttempt(now, ReviewEligibility()))
        for (day in 1..10) repo.recordBlockingSuccess(now + day * DAY_MILLIS)
        assertEquals(3, repo.reviewHistory.first().successfulDays.size)
    }

    @Test fun concurrentReviewReservationsAreAtomicAndPersistAcrossRepositoryRecreation() = runTest {
        val store = PromptTestStore()
        val repo = PlayPromptRepository(store)
        repo.recordUse(now - 8 * DAY_MILLIS)
        for (day in 2 downTo 0) repo.recordBlockingSuccess(now - day * DAY_MILLIS)
        val results = (1..20).map { async { repo.reserveReviewAttempt(now, ReviewEligibility()) } }.awaitAll()
        assertEquals(1, results.count { it })
        val recreated = PlayPromptRepository(store)
        assertEquals(now, recreated.reviewHistory.first().lastAttemptAt)
        assertEquals(0L, recreated.reviewHistory.first().positiveActionAt)
        recreated.recordBlockingSuccess(now + DAY_MILLIS)
        assertFalse(recreated.reserveReviewAttempt(now + DAY_MILLIS, ReviewEligibility()))
        recreated.recordBlockingSuccess(now + 90 * DAY_MILLIS)
        assertTrue(recreated.reserveReviewAttempt(now + 90 * DAY_MILLIS, ReviewEligibility()))
    }

    @Test fun updateCooldownPersistsButNewVersionOrPriorityCanPrompt() = runTest {
        val store = PromptTestStore()
        val repo = PlayPromptRepository(store)
        assertTrue(repo.reserveUpdatePrompt(20, 0, now))
        assertFalse(PlayPromptRepository(store).reserveUpdatePrompt(20, 0, now + 1))
        assertFalse(repo.reserveUpdatePrompt(20, 0, now - DAY_MILLIS))
        assertTrue(repo.reserveUpdatePrompt(20, 0, now + DAY_MILLIS))
        assertTrue(repo.reserveUpdatePrompt(21, 0, now + DAY_MILLIS))
        assertTrue(repo.reserveUpdatePrompt(21, 5, now + DAY_MILLIS))
    }
}
