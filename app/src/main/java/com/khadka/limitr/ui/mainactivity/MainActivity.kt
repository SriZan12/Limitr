package com.khadka.limitr.ui.mainactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.khadka.limitr.R
import com.khadka.limitr.databinding.ActivityMainBinding
import com.khadka.limitr.utils.Insets
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import com.khadka.limitr.repository.AppListRepository
import javax.inject.Inject
import com.khadka.limitr.play.*
import com.khadka.limitr.ui.home.main_fragment.fragment.FragmentHome

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var apps: AppListRepository
    @Inject lateinit var updates: PlayUpdateRepository
    @Inject lateinit var reviews: PlayReviewRepository
    @Inject lateinit var playGuard: PlayUiGuard
    private var playHost: PlayEngagementHost? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apps.preload()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.Ui_Color))
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        Insets.apply(binding.root)
        binding.mainToolbar.visibility = android.view.View.GONE
        playHost = attachPlayEngagement(binding.root, updates, reviews, playGuard) {
            val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            (navHost?.childFragmentManager?.primaryNavigationFragment as? FragmentHome)?.isReadyForPlayPrompt() == true
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        playHost?.refreshReadiness()
    }
    fun onPlayPromptReadinessChanged() { playHost?.refreshReadiness() }
    override fun onSupportNavigateUp(): Boolean =
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController.navigateUp() || super.onSupportNavigateUp()
}
