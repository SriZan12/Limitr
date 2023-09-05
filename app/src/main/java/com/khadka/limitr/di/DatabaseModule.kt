package com.khadka.limitr.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.khadka.limitr.R
import com.khadka.limitr.data.local.appdatabase.LimitrDao
import com.khadka.limitr.data.local.appdatabase.LimitrDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences> by
preferencesDataStore(name = "Data_Store")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

//    singleton

    @Provides
    @Singleton
    fun getDatabase(application: Application): LimitrDatabase {

        synchronized(this) {
            return Room.databaseBuilder(
                application.applicationContext, LimitrDatabase::class.java, "LimitrTable"
            ).allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    @Provides
    @Singleton
    fun providesLimitrDao(limitrDatabase: LimitrDatabase): LimitrDao {
        return limitrDatabase.getLimitrDao()
    }

    @Provides
    @Singleton
    fun getSharedPrefDb(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.getString(R.string.my_sharedPref),
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun getSharedPrefEditor(sharedPreferences: SharedPreferences): SharedPreferences.Editor {
        return sharedPreferences.edit()
    }

    @Provides
    @Singleton
    fun getDataStore(@ApplicationContext context: Context): DataStore<androidx.datastore.preferences.core.Preferences> {
        return context.dataStore
    }
}