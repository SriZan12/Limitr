package com.example.limitr.ui.home.main_fragment.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.limitr.R
import com.example.limitr.data.local.appdatabase.model.LimitrEntities
import com.example.limitr.ui.home.activity.VideoActivity
import com.example.limitr.ui.home.blockedApps.vm.BlockedAppViewModels
import com.example.limitr.ui.home.main_fragment.vm.MainFragmentViewModel
import com.example.limitr.ui.home.model.App
import com.example.limitr.ui.theme.Danger
import com.example.limitr.ui.theme.LimitrTheme
import com.example.limitr.ui.theme.Normal
import com.example.limitr.ui.theme.UiColor
import com.example.limitr.ui.theme.Warning
import com.example.limitr.utils.Constants.DAILY_CRYPTO_REWARD
import com.example.limitr.utils.Constants.FIRST_LOGIN_REWARD
import com.example.limitr.utils.Constants.IS_NEW_USER
import com.example.limitr.utils.DateAndTime.formatTimeInNumbers
import com.example.limitr.utils.DateAndTime.getRemainingTime
import com.example.limitr.utils.DateAndTime.getTodayDate
import com.example.limitr.utils.FirebaseUtils.loadProfilePhoto
import com.example.limitr.utils.Permissions.isAccessibilityEnabled
import com.example.limitr.utils.Permissions.isUsageStateManagerEnabled
import com.example.limitr.utils.Status
import com.example.limitr.utils.ViewUtils
import com.example.limitr.utils.ViewUtils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class FragmentHome : Fragment(R.layout.fragment_home) {

    private var dialog: Dialog? = null
    private var onBackPressed = 0L
    private val mainViewModel: MainFragmentViewModel by viewModels()
    private val blockedAppViewModel: BlockedAppViewModels by viewModels()
    private var selectedTabIndex by mutableIntStateOf(0)
    private var usageApps by mutableStateOf(listOf<App>())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LimitrTheme {
                    HomeScreen()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (System.currentTimeMillis() < onBackPressed + 2000) {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                requireActivity().finish()
            } else {
                showToast(requireContext(), getString(R.string.press_again_to_exit))
                onBackPressed = System.currentTimeMillis()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!Settings.canDrawOverlays(requireContext()) ||
            !requireContext().isAccessibilityEnabled() || !isUsageStateManagerEnabled(requireContext())
        ) {
            showPermissionDialog()
        } else if (IS_NEW_USER) {
            showFirstLoginRewardDialog()

        } else {
            checkLastLoggedInDate()
            loadStatistics()
        }
    }

    @Composable
    private fun HomeScreen() {
        val crypto by mainViewModel.getCrypto().collectAsState(initial = 0)
        val blockedApps by blockedAppViewModel.getBlockedApps().observeAsState(initial = emptyList())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(UiColor),
        ) {
            TopHeader(
                crypto = crypto,
                onProfileClick = {
                    val action = FragmentHomeDirections.actionFragmentHomeToFragmentEditProfile()
                    findNavController().navigate(action)
                },
            )

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(getString(R.string.all_apps), color = androidx.compose.ui.graphics.Color.White) },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(getString(R.string.blocked_apps), color = androidx.compose.ui.graphics.Color.White) },
                )
            }

            if (selectedTabIndex == 0) {
                AppUsageList(
                    apps = usageApps,
                    onAppClick = { appPackage ->
                        val intent = Intent(requireContext(), com.example.limitr.ui.blocker.activity.BlockAppActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra(requireContext().getString(R.string.packageName), appPackage)
                        startActivity(intent)
                    },
                )
            } else {
                BlockedAppsList(
                    blockedApps = blockedApps,
                    onAppClick = { appPackage ->
                        val intent = Intent(requireContext(), com.example.limitr.ui.blocker.activity.BlockAppActivity::class.java)
                        intent.putExtra(requireContext().getString(R.string.packageName), appPackage)
                        startActivity(intent)
                    },
                )
            }
        }
    }

    @Composable
    private fun TopHeader(crypto: Int, onProfileClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AndroidView(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onProfileClick() },
                factory = { context ->
                    de.hdodenhof.circleimageview.CircleImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(40, 40)
                    }
                },
                update = { imageView ->
                    loadProfilePhoto(imageView, requireContext())
                },
            )

            Text(
                text = getString(R.string.app_name),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 24.sp,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = crypto.toString(),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 18.sp,
                )
                Image(
                    bitmap = requireContext().getDrawable(R.drawable.crypto)!!.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }

    @Composable
    private fun AppUsageList(apps: List<App>, onAppClick: (String) -> Unit) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            items(items = apps, key = { it.appPackageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppClick(app.appPackageName) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        bitmap = app.appIcon!!.toBitmap().asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier.size(40.dp),
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = app.appName, color = androidx.compose.ui.graphics.Color.White)
                            Text(text = app.usageDuration, color = androidx.compose.ui.graphics.Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = app.usagePercentage / 100f,
                                modifier = Modifier.weight(1f),
                                color = when {
                                    app.usagePercentage < 50 -> Normal
                                    app.usagePercentage in 50..79 -> Warning
                                    else -> Danger
                                },
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = "${app.usagePercentage}%", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BlockedAppsList(blockedApps: List<LimitrEntities>, onAppClick: (String) -> Unit) {
        if (blockedApps.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = getString(R.string.no_apps_are_blocked),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 20.sp,
                )
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(items = blockedApps, key = { it.appName }) { app ->
                val timerText by produceState(initialValue = "") {
                    while (true) {
                        value = getTimerText(app)
                        delay(1000)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppClick(app.appPackage ?: "") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val icon = app.appPackage?.let { ViewUtils.getAppIconByPackageName(requireContext(), it) }
                    if (icon != null) {
                        Image(
                            bitmap = icon.toBitmap().asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(text = app.appName, color = androidx.compose.ui.graphics.Color.White)
                        if (app.starTime != null && app.endTime != null) {
                            Text(
                                text = "${formatTime(app.starTime)}-${formatTime(app.endTime)}",
                                color = androidx.compose.ui.graphics.Color.White,
                            )
                            if (timerText.isNotEmpty()) {
                                Text(text = timerText, color = androidx.compose.ui.graphics.Color.White)
                            }
                        } else {
                            Text(text = timerText, color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
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

    private fun loadStatistics() {
        val usageStateManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var appList = usageStateManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 3600 * 24,
            System.currentTimeMillis(),
        )
        appList = appList.filter { app -> app.totalTimeInForeground > 0 }.toList()

        if (appList.isNotEmpty()) {
            val mySortedMap: MutableMap<String?, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.packageName] = usageStats
            }
            showAppsUsage(mySortedMap)
        } else {
            usageApps = emptyList()
        }
    }

    private fun showAppsUsage(mySortedMap: Map<String?, UsageStats>) {
        val appsList = ArrayList<App>()
        val usageStatsList: List<UsageStats> = ArrayList(mySortedMap.values)
        val sorted = usageStatsList.sortedBy { it.totalTimeInForeground }

        var totalTime = 0L
        for (usageStats in sorted) {
            totalTime += usageStats.totalTimeInForeground
        }

        for (usageStats in sorted) {
            try {
                val packageName = usageStats.packageName
                val icon = ViewUtils.getAppIconByPackageName(requireContext(), packageName)
                val appName = ViewUtils.getAppNameByPackageName(requireContext(), packageName)
                if (appName.trim() != context?.getString(R.string.app_name)) {
                    val usageDuration = getDurationBreakdown(usageStats.totalTimeInForeground)
                    val usagePercentage = (usageStats.totalTimeInForeground * 100 / totalTime).toInt()
                    appsList.add(App(icon, appName, packageName, usagePercentage, usageDuration))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        usageApps = appsList.reversed()
    }

    private fun getDurationBreakdown(millis: Long): String {
        var timeInMillis = millis
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        timeInMillis -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        timeInMillis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis)
        return "$hours h $minutes m $seconds s"
    }

    private fun goToDisplayOverOtherAppsSettings() {
        val packageName = requireContext().packageName
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun goToUsageStateManagerSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun goToAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.apply {
            FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPermissionDialog() {
        val accessibilityGranted = requireContext().isAccessibilityEnabled()
        val overlayGranted = Settings.canDrawOverlays(requireContext())
        val usageGranted = isUsageStateManagerEnabled(requireContext())

        val permissionDialog = Dialog(requireContext())
        permissionDialog.setCancelable(false)
        permissionDialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = getString(R.string.permission_required)) },
                        text = {
                            Column {
                                Text(text = getString(R.string.AccessibilityService))
                                Text(text = getString(R.string.DrawOverOtherApps))
                                Text(text = getString(R.string.usageSateManagerSate))
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { goToAccessibilitySettings() }, enabled = !accessibilityGranted) {
                                Text(if (accessibilityGranted) getString(R.string.Granted) else getString(R.string.AccessibilityService))
                            }
                        },
                        dismissButton = {
                            Column {
                                TextButton(onClick = { goToDisplayOverOtherAppsSettings() }, enabled = !overlayGranted) {
                                    Text(if (overlayGranted) getString(R.string.Granted) else getString(R.string.DrawOverOtherApps))
                                }
                                TextButton(onClick = { goToUsageStateManagerSettings() }, enabled = !usageGranted) {
                                    Text(if (usageGranted) getString(R.string.Granted) else getString(R.string.usageSateManagerSate))
                                }
                                TextButton(onClick = {
                                    permissionDialog.dismiss()
                                    showHelpDialog()
                                }) {
                                    Text(getString(R.string.Help))
                                }
                            }
                        },
                    )
                }
            },
        )
        dialog = permissionDialog
        permissionDialog.show()
    }


    private fun checkLastLoggedInDate() {
        lifecycleScope.launch(Dispatchers.Main) {
            val lastLoggedDate = mainViewModel.getLastLoggedDate().first()
            if (lastLoggedDate.isEmpty()) {
                Timber.d("INSIDE CHECKLOGGED IF")
                showEverydayReward()
            } else if (getTodayDate() != lastLoggedDate) {
                Timber.d("INSIDE CHECKLOGGED ELSE")
                showEverydayReward()
            }

        }

    }

    private fun showEverydayReward() {
        val rewardDialog = Dialog(requireContext())
        rewardDialog.setCancelable(false)
        rewardDialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = getString(R.string.reward)) },
                        text = { Text(text = DAILY_CRYPTO_REWARD.toString()) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        val currentCrypto = mainViewModel.getCrypto().first()
                                        mainViewModel.upsertEverydayDate(getTodayDate())
                                        mainViewModel.upsertCrypto(currentCrypto + DAILY_CRYPTO_REWARD)
                                        rewardDialog.dismiss()
                                    }
                                },
                            ) {
                                Text(text = getString(R.string.reward))
                            }
                        },
                    )
                }
            },
        )
        rewardDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showFirstLoginRewardDialog() {
        val rewardDialog = Dialog(requireContext())
        rewardDialog.setCancelable(false)
        rewardDialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = getString(R.string.first_login_reward)) },
                        text = { Text(text = FIRST_LOGIN_REWARD.toString()) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    mainViewModel.upsertCrypto(FIRST_LOGIN_REWARD)
                                    IS_NEW_USER = false
                                    rewardDialog.dismiss()
                                    checkLastLoggedInDate()
                                },
                            ) {
                                Text(text = getString(R.string.reward))
                            }
                        },
                    )
                }
            },
        )
        rewardDialog.show()
    }

    private fun showHelpDialog() {
        val helpDialog = Dialog(requireContext())
        helpDialog.setCancelable(true)
        helpDialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    AlertDialog(
                        onDismissRequest = { helpDialog.dismiss() },
                        title = { Text(text = getString(R.string.help_instructions)) },
                        text = { Text(text = getString(R.string.click_here_for_more_details)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val intent = Intent(requireContext(), VideoActivity::class.java)
                                    intent.flags = FLAG_ACTIVITY_NEW_TASK
                                    intent.putExtra("androidVersion", Status.AndroidVersion.ANDROID_13_LESS)
                                    requireContext().startActivity(intent)
                                },
                            ) { Text(getString(R.string.for_android_version_below_13)) }
                        },
                        dismissButton = {
                            Column {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(requireContext(), VideoActivity::class.java)
                                        intent.flags = FLAG_ACTIVITY_NEW_TASK
                                        intent.putExtra("androidVersion", Status.AndroidVersion.ANDROID_13_PLUS)
                                        requireContext().startActivity(intent)
                                    },
                                ) { Text(getString(R.string.for_android_version_13_and_above)) }
                                TextButton(
                                    onClick = {
                                        goToAccessibilitySettings()
                                        helpDialog.dismiss()
                                    },
                                ) { Text(getString(R.string.grant_permission)) }
                            }
                        },
                    )
                }
            },
        )
        helpDialog.show()
    }
}
