package com.khadka.limitr.ui.home.blockedApps.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.khadka.limitr.data.local.appdatabase.model.limitrentities.LimitrEntities
import com.khadka.limitr.repository.BlockedAppRepository
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