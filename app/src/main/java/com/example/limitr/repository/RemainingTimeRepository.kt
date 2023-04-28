package com.example.limitr.repository

import androidx.lifecycle.LiveData
import com.example.limitr.data.room.appdatabase.LimitrDao
import com.example.limitr.data.room.appdatabase.model.LimitrEntities
import javax.inject.Inject

class RemainingTimeRepository @Inject constructor() {

    @Inject
    lateinit var limitrDao: LimitrDao

    fun getRemainingTime(appName: String): LiveData<LimitrEntities> {
        return limitrDao.getRemainingTime(appName)
    }

    suspend fun insertRemainingTime(limitrEntities: LimitrEntities) {
        limitrDao.insertRemainingTime(limitrEntities)
    }

    suspend fun deleteRemainingTime(appName: String) {
        limitrDao.deleteRemainingTime(appName)
    }

    suspend fun updateNotificationStatus(appName: String, notificationStatus: Boolean) {
        limitrDao.updateNotificationStatus(appName, notificationStatus)
    }
}