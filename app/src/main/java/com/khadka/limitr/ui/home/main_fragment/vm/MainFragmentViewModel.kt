package com.khadka.limitr.ui.home.main_fragment.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadka.limitr.repository.MainFragmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainFragmentViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var mainFragmentRepository: MainFragmentRepository

    fun getCrypto(): Flow<Int> {
        return mainFragmentRepository.getCrypto()
    }

    fun upsertCrypto(crypto: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            mainFragmentRepository.upsertCrypto(crypto)
        }
    }

    fun getLastLoggedDate(): Flow<String> {
        return mainFragmentRepository.getLastLoggedDate()
    }

    fun upsertEverydayDate(todayDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mainFragmentRepository.upsertEverydayDate(todayDate)
        }
    }
}