package com.example.limitr.repository

import androidx.lifecycle.LiveData
import com.example.limitr.data.room.appdatabase.LimitrDao
import com.example.limitr.data.room.appdatabase.model.LimitrEntities
import javax.inject.Inject


class BlockedAppRepository @Inject constructor() {

    @Inject
    lateinit var limitrDao: LimitrDao

    fun getBlockedApps(): LiveData<List<LimitrEntities>> {
        return limitrDao.getBlockedApps()
    }

}