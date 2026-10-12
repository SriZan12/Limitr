package com.khadka.limitr.data.local.appdatabase

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.khadka.limitr.data.local.appdatabase.model.LimitrEntities
import com.khadka.limitr.data.local.appdatabase.model.AppHistoryEntities
import kotlinx.coroutines.flow.Flow

@Dao
interface LimitrDao {

    @Query("SELECT * FROM LimitrTable WHERE appName = :appName")
    fun getRemainingTime(appName: String): LiveData<LimitrEntities>

    @Query("SELECT * FROM LimitrTable")
    fun getBlockedApps(): LiveData<List<LimitrEntities>>

    @Query("SELECT * FROM LimitrTable WHERE appName = :appName")
    suspend fun getAppName(appName: String): LimitrEntities?

    @Query("SELECT * FROM HistoryTable ORDER BY blockedDate DESC")
    fun history(): Flow<List<AppHistoryEntities>>

    @Query("SELECT * FROM LimitrTable WHERE appPackage IS NULL OR appPackage = ''")
    suspend fun unresolvedLegacy(): List<LimitrEntities>

    @Upsert
    suspend fun insertRemainingTime(limitrEntities: LimitrEntities)

    @Query("DELETE FROM LimitrTable WHERE appName = :appName")
    suspend fun deleteRemainingTime(appName: String)

    @Query("UPDATE LimitrTable SET notificationStatus = :notificationStatus  WHERE appName = :appName")
    suspend fun updateNotificationStatus(appName: String, notificationStatus: Boolean)

}
