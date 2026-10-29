package com.khadka.limitr.play

import javax.inject.Inject

internal const val DAY_MILLIS = 86_400_000L

enum class UpdateFlow { FLEXIBLE, IMMEDIATE }
enum class UpdateAvailability { NONE, AVAILABLE, IN_PROGRESS }
enum class UpdateInstallState { IDLE, DOWNLOADING, DOWNLOADED, INSTALLING }

data class UpdateSnapshot(
    val version: Int,
    val priority: Int,
    val availability: UpdateAvailability,
    val installState: UpdateInstallState,
    val flexibleAllowed: Boolean,
    val immediateAllowed: Boolean
)

/** No Android dependency: recovery and installation take precedence over fresh prompts. */
class SelectUpdateFlow @Inject constructor() {
    operator fun invoke(update: UpdateSnapshot, previousFlow: UpdateFlow? = null): UpdateFlow? = when {
        update.installState == UpdateInstallState.DOWNLOADED || update.installState == UpdateInstallState.INSTALLING -> null
        update.availability == UpdateAvailability.IN_PROGRESS ->
            if (previousFlow == UpdateFlow.FLEXIBLE) null else UpdateFlow.IMMEDIATE
        update.installState != UpdateInstallState.IDLE -> null
        update.availability != UpdateAvailability.AVAILABLE -> null
        update.priority >= 4 && update.immediateAllowed -> UpdateFlow.IMMEDIATE
        update.flexibleAllowed -> UpdateFlow.FLEXIBLE
        else -> null
    }
}

data class ReviewHistory(
    val firstUseAt: Long = 0,
    val successfulDays: Set<String> = emptySet(),
    val positiveActionAt: Long = 0,
    val lastAttemptAt: Long = 0
)

/** These are LIMITR's conservative limits, not Google's undisclosed review quota. */
class ReviewEligibility @Inject constructor() {
    operator fun invoke(history: ReviewHistory, now: Long): Boolean =
        history.firstUseAt > 0 && now - history.firstUseAt >= 7 * DAY_MILLIS &&
            history.successfulDays.size >= 3 && history.positiveActionAt > 0 &&
            now - history.positiveActionAt in 0..(10 * 60_000L) &&
            (history.lastAttemptAt == 0L || now - history.lastAttemptAt >= 90 * DAY_MILLIS)
}
