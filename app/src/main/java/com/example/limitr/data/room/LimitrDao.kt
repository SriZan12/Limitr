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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemainingTime(limitrEntities: LimitrEntities)

    @Delete
    suspend fun deleteRemainingTime(limitrEntities: LimitrEntities)

    @Update
    suspend fun updateRemainingTime(limitrEntities: LimitrEntities)

}