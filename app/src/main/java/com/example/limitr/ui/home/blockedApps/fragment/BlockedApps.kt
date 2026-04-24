package com.example.limitr.ui.home.blockedApps.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limitr.R
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.blockedApps.vm.BlockedAppViewModels
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = getString(R.string.no_apps_are_blocked), style = MaterialTheme.typography.titleMedium)
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(blockedApps) { app ->
                BlockedAppCard(
                    app = app,
                    onClick = { app.appPackage?.let(onAppSelected) }
                )
            }
        }
    }

    @Composable
    private fun BlockedAppCard(app: LimitrEntities, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                Text(text = app.appPackage.orEmpty(), style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Blocked: ${app.blockedTime?.let { Date(it) } ?: "--"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
