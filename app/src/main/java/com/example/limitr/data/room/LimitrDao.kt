package com.example.limitr.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.limitr.data.room.model.LimitrEntities

@Dao
interface LimitrDao {

    @Query("SELECT * FROM LimitrTable WHERE appName = :appName")
    fun getRemainingTime(appName: String): LiveData<LimitrEntities>

    @Query("SELECT * FROM LimitrTable")
    fun getBlockedApps(): LiveData<List<LimitrEntities>>

    @Query("SELECT * FROM LimitrTable WHERE appName = :appName")
    fun getAppName(appName: String): LimitrEntities?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemainingTime(limitrEntities: LimitrEntities)

    @Query("DELETE FROM LimitrTable WHERE appName = :appName")
    suspend fun deleteRemainingTime(appName: String)

    @Query("UPDATE LimitrTable SET notificationStatus = :notificationStatus  WHERE appName = :appName")
    suspend fun updateNotificationStatus(appName: String, notificationStatus: Boolean)

}