package com.khadka.limitr.blocking

import com.khadka.limitr.repository.MainFragmentRepository
import com.khadka.limitr.utils.Constants.REQUIRED_CRYPTO_FOR_UNBLOCK
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class UnblockResult { UNBLOCKED, INSUFFICIENT_CRYPTO, STALE }

/** One paid operation at a time across every details screen in this process. */
@Singleton
class CryptoUnblocker @Inject constructor(
    private val repository: SessionRepository,
    private val rewards: MainFragmentRepository
) {
    private val mutex = Mutex()

    suspend fun unblock(session: BlockingSession): UnblockResult = mutex.withLock {
        if (rewards.getCrypto().first() < REQUIRED_CRYPTO_FOR_UNBLOCK) return@withLock UnblockResult.INSUFFICIENT_CRYPTO
        // Leaving the screen must not cancel the debit after Room has removed the timer.
        // Process death still cannot be made atomic across Room and DataStore without a ledger.
        withContext(NonCancellable) {
            if (!repository.cancel(session)) return@withContext UnblockResult.STALE
            val spent = try {
                rewards.spendCrypto()
            } catch (failure: Exception) {
                try { repository.restoreIfAbsent(session) }
                catch (restoreFailure: Exception) { failure.addSuppressed(restoreFailure) }
                throw failure
            }
            if (spent) UnblockResult.UNBLOCKED else {
                repository.restoreIfAbsent(session)
                UnblockResult.INSUFFICIENT_CRYPTO
            }
        }
    }
}
