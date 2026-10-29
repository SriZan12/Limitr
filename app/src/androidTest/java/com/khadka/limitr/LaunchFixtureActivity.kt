package com.khadka.limitr

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Installed only in the test APK, never in the application artifact. */
class LaunchFixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "LIMITR launch detection fixture" })
    }
}

class NextLaunchFixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Second window from the same blocked package" })
    }
}
