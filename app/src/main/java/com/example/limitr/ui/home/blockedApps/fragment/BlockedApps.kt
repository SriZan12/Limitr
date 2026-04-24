package com.example.limitr.ui.home.blockedApps.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.observeAsState
import com.example.limitr.R
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.blockedApps.vm.BlockedAppViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedApps : Fragment() {

    private val viewModel: BlockedAppViewModels by viewModels()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    val blockedApps by viewModel.getBlockedApps().observeAsState(emptyList())
                    BlockedAppsScreen(
                        blockedApps = blockedApps,
                        onAppSelected = { appPackageName ->
                            val intent = Intent(requireContext(), BlockAppActivity::class.java).apply {
                                putExtra(requireContext().getString(R.string.packageName), appPackageName)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun BlockedAppsScreen(
        blockedApps: List<LimitrEntities>,
        onAppSelected: (String) -> Unit
    ) {
        if (blockedApps.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = getString(R.string.no_apps_are_blocked))
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(blockedApps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { app.appPackage?.let(onAppSelected) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = app.appPackage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Divider()
            }
        }
    }
}
