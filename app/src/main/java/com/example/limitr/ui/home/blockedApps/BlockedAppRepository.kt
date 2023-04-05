package com.example.limitr.ui.home.blockedApps

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.example.limitr.data.room.LimitrDao
import com.example.limitr.data.room.model.LimitrEntities
import javax.inject.Inject


class BlockedAppRepository @Inject constructor() {

    @Inject
    lateinit var limitrDao: LimitrDao

    fun getBlockedApps(): LiveData<List<LimitrEntities>> {
        return limitrDao.getBlockedApps()
    }

}