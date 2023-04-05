package com.example.limitr.ui.home.blockedApps

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.limitr.data.room.model.LimitrEntities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlockedAppViewModels @Inject constructor() : ViewModel() {

    @Inject
    lateinit var blockedAppRepository: BlockedAppRepository

    fun getBlockedApps(): LiveData<List<LimitrEntities>> {
      return blockedAppRepository.getBlockedApps()
    }
}