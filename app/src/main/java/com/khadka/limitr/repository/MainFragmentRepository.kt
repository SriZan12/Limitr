package com.khadka.limitr.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.khadka.limitr.utils.Constants.CRYPTO
import com.khadka.limitr.utils.Constants.LAST_LOGGED_DATE
import com.khadka.limitr.utils.Constants.FIRST_LOGIN_REWARD
import com.khadka.limitr.utils.Constants.DAILY_CRYPTO_REWARD
import com.khadka.limitr.utils.Constants.REQUIRED_CRYPTO_FOR_UNBLOCK
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class MainFragmentRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    fun getCrypto() = dataStore.data.map { it[CRYPTO] ?: 0 }
    fun getLastLoggedDate() = dataStore.data.map { it[LAST_LOGGED_DATE] ?: "" }
    suspend fun upsertCrypto(crypto: Int) { dataStore.edit { it[CRYPTO] = crypto } }
    suspend fun upsertEverydayDate(todayDate: String) { dataStore.edit { it[LAST_LOGGED_DATE] = todayDate } }
    suspend fun claimReward() {
        val today = LocalDate.now().toString()
        dataStore.edit {
            if (it[LAST_LOGGED_DATE] != today) {
                it[CRYPTO] = (it[CRYPTO] ?: 0) + if (it[CRYPTO] == null) FIRST_LOGIN_REWARD else DAILY_CRYPTO_REWARD
                it[LAST_LOGGED_DATE] = today
            }
        }
    }
    suspend fun spendCrypto(): Boolean {
        var spent = false
        dataStore.edit {
            val current = it[CRYPTO] ?: 0
            if (current >= REQUIRED_CRYPTO_FOR_UNBLOCK) { it[CRYPTO] = current - REQUIRED_CRYPTO_FOR_UNBLOCK; spent = true }
        }
        return spent
    }
}
