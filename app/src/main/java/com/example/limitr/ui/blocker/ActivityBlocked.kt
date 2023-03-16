package com.example.limitr.ui.blocker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.limitr.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityBlocked : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)
    }
}