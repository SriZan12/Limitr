package com.khadka.limitr

import com.khadka.limitr.blocking.SessionRepository
import com.khadka.limitr.blocking.SessionCoordinator
import com.khadka.limitr.blocking.SessionClock
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Instrumentation access to the actual app graph, excluded from release builds. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestSessions {
    fun repository(): SessionRepository
    fun coordinator(): SessionCoordinator
    fun clock(): SessionClock
}
