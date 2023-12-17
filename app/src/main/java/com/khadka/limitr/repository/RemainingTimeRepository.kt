package com.khadka.limitr.repository

import androidx.lifecycle.LiveData
import com.khadka.limitr.data.local.appdatabase.model.historyentities.AppHistoryEntities
import com.khadka.limitr.data.local.appdatabase.room.appblock.LimitrDao
import com.khadka.limitr.data.local.appdatabase.model.limitrentities.LimitrEntities
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

    suspend fun insertAppBlockHistory(historyEntities: AppHistoryEntities){
        limitrDao.insertAppBlockHistory(historyEntities = historyEntities)
    }

    fun getAppHistory(appName: String): LiveData<List<AppHistoryEntities>> {
        return limitrDao.getHistory(appName = appName)
    }
}