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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.Ui_Color))
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        Insets.apply(binding.root)
        binding.mainToolbar.visibility = android.view.View.GONE
    }
    override fun onSupportNavigateUp(): Boolean =
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController.navigateUp() || super.onSupportNavigateUp()
}
