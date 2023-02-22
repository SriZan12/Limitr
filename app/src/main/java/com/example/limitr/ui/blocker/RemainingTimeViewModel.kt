package com.example.limitr.ui.blocker

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.limitr.data.room.model.LimitrEntities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RemainingTimeViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var remainingTimeRepository: RemainingTimeRepository

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

    fun updateRemainingTime(limitrEntities: LimitrEntities) = liveData {
        try {
            emit(remainingTimeRepository.updateRemainingTime(limitrEntities))
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }


}