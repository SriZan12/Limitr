package com.khadka.limitr.ui.blocker.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Compatibility entry point for old notifications; actual blocking is service-owned. */
class ActivityBlocked : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra("packageName")?.let { startActivity(Intent(this, BlockAppActivity::class.java).putExtra("packageName", it)) }
        finish()
    }
}
