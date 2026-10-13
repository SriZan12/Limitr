package com.khadka.limitr.blocking

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM BlockingSession ORDER BY appName")
    fun observe(): Flow<List<BlockingSession>>

    @Query("SELECT * FROM BlockingSession")
    suspend fun all(): List<BlockingSession>

    @Query("SELECT * FROM BlockingSession WHERE packageName = :packageName")
    suspend fun get(packageName: String): BlockingSession?

    @Upsert suspend fun save(session: BlockingSession)

    @Query("DELETE FROM BlockingSession WHERE packageName = :packageName AND revision = :revision")
    suspend fun cancel(packageName: String, revision: String): Int

    @Query("UPDATE BlockingSession SET blockNotifications = :enabled WHERE packageName = :packageName AND revision = :revision")
    suspend fun notifications(packageName: String, revision: String, enabled: Boolean)
}
