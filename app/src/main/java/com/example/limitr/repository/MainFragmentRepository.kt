package com.example.limitr.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import com.example.limitr.utils.Constants.CRYPTO
import com.example.limitr.utils.Constants.LAST_LOGGED_DATE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class MainFragmentRepository @Inject constructor() {

    @Inject
    lateinit var dataStore: DataStore<androidx.datastore.preferences.core.Preferences>

    fun getCrypto(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[CRYPTO] ?: 0
        }
    }

    suspend fun upsertCrypto(crypto: Int) {
        Timber.d("CRYPTO = $crypto")
        dataStore.edit { prefrences ->
            prefrences[CRYPTO] = crypto
        }
    }

    fun getLastLoggedDate(): Flow<String> {
        return dataStore.data.map { date ->
            date[LAST_LOGGED_DATE] ?: ""
        }
    }

    suspend fun upsertEverydayDate(todayDate: String) {
        dataStore.edit { date ->
            date[LAST_LOGGED_DATE] = todayDate
        }
    }
}