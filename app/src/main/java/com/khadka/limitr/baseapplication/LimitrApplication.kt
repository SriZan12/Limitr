package com.khadka.limitr.baseapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import com.khadka.limitr.BuildConfig
import com.khadka.limitr.blocking.SessionCoordinator

@HiltAndroidApp
class LimitrApplication : Application() {
    @Inject lateinit var coordinator: SessionCoordinator
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        LegacyNotificationCleanup.removeBlockingAlerts(this)
        coordinator.start()
    }
}
