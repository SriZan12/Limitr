package com.khadka.limitr.ui.blocker.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.khadka.limitr.data.local.appdatabase.model.LimitrEntities
import com.khadka.limitr.repository.RemainingTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlockedAppVM @Inject constructor(private val remainingTimeRepository: RemainingTimeRepository) :
    ViewModel() {


    fun getRemainingTime(appName: String?): LiveData<LimitrEntities> {
        return remainingTimeRepository.getRemainingTime(appName!!)
    }

    fun insertRemainingTime(limitrEntities: LimitrEntities) = liveData {
        try {
            emit(remainingTimeRepository.insertRemainingTime(limitrEntities))
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    fun deleteRemainingTime(appName: String) = liveData {
        try {
            emit(remainingTimeRepository.deleteRemainingTime(appName))
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

//    fun getAppName(appName: String): LimitrEntities? {
//        return remainingTimeRepository.getAppName(appName)
//    }


}