package com.example.limitr

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.limitr.databinding.ActivityMainBinding
import com.example.limitr.databinding.FragmentHomeBinding
import com.example.limitr.ui.home.FragmentHome
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val activity = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(activityMainBinding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.fragmentSignup,
                R.id.fragmentHome
            )
        )

        setSupportActionBar(activityMainBinding.mainToolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { item1, item2, item3 ->
            if (item2.id == R.id.fragmentHome) {
                activityMainBinding.mainToolbar.visibility = View.GONE
            } else {
                activityMainBinding.mainToolbar.visibility = View.VISIBLE

            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }

    override fun onBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment)
        val currentDestination = navController.currentDestination

        Timber.d("Fragment = $currentDestination")

        if (currentDestination?.id == R.id.fragmentHome) {
            finishAffinity()
        }

        // The back pressed event has not been handled in the Fragment.
        super.onBackPressed()
    }

}