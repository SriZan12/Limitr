package com.example.limitr.ui.home.blockedApps.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.repository.BlockedAppRepository
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