package com.khadka.limitr.ui.splash_activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.khadka.limitr.ui.mainactivity.MainActivity
import com.khadka.limitr.repository.AppListRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    @Inject lateinit var apps: AppListRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apps.preload()
        startActivity(Intent(this, MainActivity::class.java))
        finish()

    }
}
