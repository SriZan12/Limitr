package com.khadka.limitr.repository

import androidx.lifecycle.LiveData
import com.khadka.limitr.data.local.appdatabase.room.LimitrDao
import com.khadka.limitr.data.local.appdatabase.model.LimitrEntities
import javax.inject.Inject


class BlockedAppRepository @Inject constructor() {

    @Inject
    lateinit var limitrDao: LimitrDao

    fun getBlockedApps(): LiveData<List<LimitrEntities>> {
        return limitrDao.getBlockedApps()
    }

}