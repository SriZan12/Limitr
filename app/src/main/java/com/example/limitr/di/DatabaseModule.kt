package com.example.limitr.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.limitr.R
import com.example.limitr.data.local.appdatabase.LimitrDao
import com.example.limitr.data.local.appdatabase.LimitrDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

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
    fun providesLimitrDao(limitrDatabase: LimitrDatabase): LimitrDao {
        return limitrDatabase.getLimitrDao()
    }

    @Provides
    fun getSharedPrefDb(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(context.getString(R.string.my_sharedPref), Context.MODE_PRIVATE)
    }

    @Provides
    fun getSharedPrefEditor(sharedPreferences: SharedPreferences): SharedPreferences.Editor {
        return sharedPreferences.edit()
    }
}