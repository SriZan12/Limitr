package com.example.limitr.ui.home.blockedApps.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limitr.R
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.ui.blocker.activity.BlockAppActivity
import com.example.limitr.ui.home.blockedApps.vm.BlockedAppViewModels
import com.example.limitr.ui.theme.UiColor
import com.example.limitr.utils.DateAndTime.formatTimeInNumbers
import com.example.limitr.utils.DateAndTime.getRemainingTime
import com.example.limitr.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BlockedApps : Fragment() {

    private val viewModel: BlockedAppViewModels by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val blockedApps by viewModel.getBlockedApps().observeAsState(initial = emptyList())
                BlockedAppsScreen(
                    blockedApps = blockedApps,
                    onAppClick = { appPackageName ->
                        val intent = Intent(requireContext(), BlockAppActivity::class.java)
                        intent.putExtra(requireContext().getString(R.string.packageName), appPackageName)
                        startActivity(intent)
                    },
                )
            }
        }
    }
}

@Composable
private fun BlockedAppsScreen(
    blockedApps: List<LimitrEntities>,
    onAppClick: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (blockedApps.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(UiColor),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(R.string.no_apps_are_blocked),
                color = Color.White,
                fontSize = 20.sp,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UiColor)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        items(items = blockedApps, key = { it.appName }) { app ->
            BlockedAppItem(
                appInfo = app,
                onClick = {
                    app.appPackage?.let(onAppClick)
                },
            )
        }
    }
}

@Composable
private fun BlockedAppItem(
    appInfo: LimitrEntities,
    onClick: () -> Unit,
) {
    val timerText by produceState(initialValue = "") {
        while (true) {
            value = getTimerText(appInfo)
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        appInfo.appPackage?.let { pkg ->
            ViewUtils.getAppIconByPackageName(context = context, packageName = pkg)
                ?.let { icon ->
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = appInfo.appName,
                        modifier = Modifier.size(40.dp),
                    )
                }
        }

        Spacer(modifier = Modifier.size(10.dp))

        Column {
            Text(text = appInfo.appName, color = Color.White)

            if (appInfo.starTime != null && appInfo.endTime != null) {
                if (timerText.isNotEmpty()) {
                    Text(text = timerText, color = Color.White)
                }
                Text(
                    text = "${formatTime(appInfo.starTime)}-${formatTime(appInfo.endTime)}",
                    color = Color.White,
                )
            } else {
                Text(text = timerText, color = Color.White)
            }
        }
    }
}

private fun getTimerText(app: LimitrEntities): String {
    val currentRemainingTime = getRemainingTime(app.blockedTime, app.remainingTime)
    return if (currentRemainingTime != null && currentRemainingTime > 0) {
        formatTimeInNumbers(currentRemainingTime)
    } else {
        ""
    }
}

private fun formatTime(timeMillis: Long): String {
    return SimpleDateFormat("hh:mm aa", Locale.getDefault()).format(Date(timeMillis))
}
