package com.khadka.limitr.di

import android.content.Context
import com.khadka.limitr.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/*
 Dagger and Hilt,
 a module is a class that defines how to create or provide instances of a certain type of object.
 */

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun providesAuthRepository(firebaseAuth: FirebaseAuth, context: Context): AuthRepository {
        return AuthRepository(firebaseAuth, context)
    }

    @Provides
    @Singleton
    fun providesFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}