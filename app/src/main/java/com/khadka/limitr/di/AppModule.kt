package com.khadka.limitr.di

import android.content.Context
import com.khadka.limitr.utils.AppReferrer
import com.khadka.limitr.utils.OverlayScreen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesOverlayScreen(): OverlayScreen {
        return OverlayScreen()
    }

    @Provides
    fun provideContext(@ApplicationContext appContext: Context): Context {
        return appContext
    }
}