package com.example.limitr.ui.mainactivity

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.limitr.R
import com.example.limitr.ui.theme.LimitrTheme
import com.example.limitr.utils.Constants.OVERLAY_DISPLAYED
import com.example.limitr.utils.OverlayScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private var showToolbar by mutableStateOf(true)

    @Inject
    lateinit var overlayScreen: OverlayScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayScreen.removeOverlayView()

        setContent {
            LimitrTheme {
                MainActivityScreen()
            }
        }
    }

    @Composable
    private fun MainActivityScreen() {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showToolbar) {
                TopAppBar(
                    title = { Text(text = getString(R.string.app_name)) },
                    navigationIcon = {
                        if (::navController.isInitialized && navController.previousBackStackEntry != null) {
                            TextButton(onClick = { navController.navigateUp() }) {
                                Text(text = "Back")
                            }
                        }
                    },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        FragmentContainerView(context).apply {
                            id = R.id.nav_host_fragment
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )

                            val navHostFragment =
                                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                                    ?: NavHostFragment.create(R.navigation.nav_graph).also {
                                        supportFragmentManager
                                            .beginTransaction()
                                            .replace(R.id.nav_host_fragment, it)
                                            .setPrimaryNavigationFragment(it)
                                            .commitNow()
                                    }

                            navController = navHostFragment.navController
                            navController.addOnDestinationChangedListener { _, destination, _ ->
                                showToolbar = destination.id != R.id.fragmentHome
                            }
                        }
                    },
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (::navController.isInitialized) {
            navController.navigateUp()
        } else {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        OVERLAY_DISPLAYED = false
    }
}
