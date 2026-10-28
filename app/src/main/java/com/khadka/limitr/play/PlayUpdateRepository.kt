package com.khadka.limitr.play

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability as PlayAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayUpdateRepository @Inject constructor(private val manager: AppUpdateManager) {
    // Each collector owns its registration; ViewModel cancellation always removes it.
    val installStates = callbackFlow {
        val listener = InstallStateUpdatedListener { trySend(it.installStatus().toInstallState()) }
        manager.registerListener(listener)
        awaitClose { manager.unregisterListener(listener) }
    }

    suspend fun check(): AppUpdateInfo = manager.appUpdateInfo.await()
    suspend fun complete() { manager.completeUpdate().await() }

    fun launch(info: AppUpdateInfo, flow: UpdateFlow, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(
            if (flow == UpdateFlow.IMMEDIATE) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
        ).build())
}

internal fun AppUpdateInfo.snapshot() = UpdateSnapshot(
    availableVersionCode(), updatePriority(), when (updateAvailability()) {
        PlayAvailability.UPDATE_AVAILABLE -> UpdateAvailability.AVAILABLE
        PlayAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> UpdateAvailability.IN_PROGRESS
        else -> UpdateAvailability.NONE
    }, installStatus().toInstallState(),
    isUpdateTypeAllowed(AppUpdateType.FLEXIBLE), isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
)

internal fun Int.toInstallState() = when (this) {
    InstallStatus.PENDING, InstallStatus.DOWNLOADING -> UpdateInstallState.DOWNLOADING
    InstallStatus.DOWNLOADED -> UpdateInstallState.DOWNLOADED
    InstallStatus.INSTALLING -> UpdateInstallState.INSTALLING
    else -> UpdateInstallState.IDLE
}
