package com.khadka.limitr.play

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayPromptRepository @Inject constructor(private val store: DataStore<Preferences>) {
    val reviewHistory = store.data.map { it.reviewHistory() }

    suspend fun recordUse(now: Long) {
        store.edit { if (it[FIRST_USE] == null) it[FIRST_USE] = now }
    }

    suspend fun recordBlockingSuccess(now: Long) {
        store.edit {
            if (it[FIRST_USE] == null) it[FIRST_USE] = now
            // UTC dates avoid counting a time-zone change as another day of experience.
            val today = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate().toString()
            it[SUCCESS_DAYS] = ((it[SUCCESS_DAYS] ?: emptySet()) + today).sorted().takeLast(3).toSet()
            it[POSITIVE_ACTION] = now
        }
    }

    suspend fun reserveReviewAttempt(now: Long, eligible: ReviewEligibility): Boolean {
        var reserved = false
        store.edit {
            if (eligible(it.reviewHistory(), now)) {
                it[REVIEW_ATTEMPT] = now
                it.remove(POSITIVE_ACTION)
                reserved = true
            }
        }
        return reserved
    }

    suspend fun lastUpdateFlow(version: Int): UpdateFlow? = store.data.first().let {
        if (it[UPDATE_VERSION] != version) null else
            UpdateFlow.entries.firstOrNull { flow -> flow.name == it[UPDATE_FLOW] }
    }

    suspend fun reserveUpdatePrompt(version: Int, priority: Int, now: Long, flow: UpdateFlow = UpdateFlow.FLEXIBLE): Boolean {
        var reserved = false
        store.edit {
            val sameOffer = it[UPDATE_VERSION] == version && it[UPDATE_PRIORITY] == priority
            val last = it[UPDATE_ATTEMPT]
            if (!sameOffer || last == null || now - last >= DAY_MILLIS) {
                it[UPDATE_VERSION] = version
                it[UPDATE_PRIORITY] = priority
                it[UPDATE_ATTEMPT] = now
                it[UPDATE_FLOW] = flow.name
                reserved = true
            }
        }
        return reserved
    }

    private fun Preferences.reviewHistory() = ReviewHistory(
        this[FIRST_USE] ?: 0, this[SUCCESS_DAYS] ?: emptySet(),
        this[POSITIVE_ACTION] ?: 0, this[REVIEW_ATTEMPT] ?: 0
    )

    private companion object {
        val FIRST_USE = longPreferencesKey("play_first_use")
        val SUCCESS_DAYS = stringSetPreferencesKey("play_success_days")
        val POSITIVE_ACTION = longPreferencesKey("play_positive_action")
        val REVIEW_ATTEMPT = longPreferencesKey("play_review_attempt")
        val UPDATE_VERSION = intPreferencesKey("play_update_version")
        val UPDATE_PRIORITY = intPreferencesKey("play_update_priority")
        val UPDATE_ATTEMPT = longPreferencesKey("play_update_attempt")
        val UPDATE_FLOW = stringPreferencesKey("play_update_flow")
    }
}

class RecordBlockingSuccess @Inject constructor(private val prompts: PlayPromptRepository) {
    suspend operator fun invoke() {
        // Optional engagement must never turn a successfully saved block into a storage error.
        try { prompts.recordBlockingSuccess(System.currentTimeMillis()) }
        catch (_: java.io.IOException) { /* Skip this milestone. */ }
    }
}

class RequestReviewIfEligible @Inject constructor(
    private val prompts: PlayPromptRepository,
    private val eligibility: ReviewEligibility
) {
    suspend operator fun invoke(now: Long) = prompts.reserveReviewAttempt(now, eligibility)
}
