package com.example.limitr.di

import android.app.Application
import androidx.room.Room
import com.example.limitr.data.room.LimitrDao
import com.example.limitr.data.room.LimitrDatabase
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

        kotlin.synchronized(this) {
            return Room.databaseBuilder(
                application.applicationContext, LimitrDatabase::class.java, "LimitrTable"
            ).allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    @Provides
    fun providesDao(limitrDatabase: LimitrDatabase): LimitrDao {
        return limitrDatabase.getLimitrDao()
    }

}