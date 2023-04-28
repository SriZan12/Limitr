package com.example.limitr.di

import android.app.Application
import androidx.room.Room
import com.example.limitr.data.room.appdatabase.LimitrDao
import com.example.limitr.data.room.appdatabase.LimitrDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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

}