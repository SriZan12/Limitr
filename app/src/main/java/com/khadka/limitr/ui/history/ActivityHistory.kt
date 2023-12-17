package com.khadka.limitr.ui.history

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import com.khadka.limitr.R
import com.khadka.limitr.data.local.appdatabase.model.historyentities.AppHistoryEntities
import com.khadka.limitr.databinding.ActivityHistoryBinding
import com.khadka.limitr.ui.blocker.vm.BlockedAppVM
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Arrays
import javax.inject.Inject

@AndroidEntryPoint
class ActivityHistory : AppCompatActivity() {

    private val blockedAppVM: BlockedAppVM by viewModels()
    private lateinit var activityHistoryBinding: ActivityHistoryBinding
    private val appHistoryList: MutableList<AppHistoryEntities> = mutableListOf()

    @Inject
    lateinit var activityHistoryListAdapter: AppHistoryListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityHistoryBinding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        setContentView(activityHistoryBinding.root)

        val appName = intent.getStringExtra("appName")

        setSupportActionBar(activityHistoryBinding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = appName


        blockedAppVM.getAppHistory(appName = appName.toString()).observe(this) {
            if (it != null) {
                Timber.d("BLOCKED APP LIST = $it")
                appHistoryList.clear()
                appHistoryList.addAll(it)

                activityHistoryListAdapter.setAppHistoryList(
                    this@ActivityHistory,
                    appHistoryList = appHistoryList
                )

                activityHistoryBinding.rvHistory.adapter = activityHistoryListAdapter
            }
        }

    }
}