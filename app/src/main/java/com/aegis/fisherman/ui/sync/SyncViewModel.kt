package com.aegis.fisherman.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aegis.fisherman.sync.OfflinePackSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncUiState { IDLE, RUNNING, SUCCESS, FAILED }

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SyncUiState.IDLE)
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    /**
     * [locationLabel]/[lat]/[lng] should come from a "my usual fishing ground" picker in
     * Settings - hardcode a sensible default here for the scaffold.
     */
    fun runSync(locationLabel: String, lat: Double, lng: Double) {
        _state.value = SyncUiState.RUNNING
        val request = OneTimeWorkRequestBuilder<OfflinePackSyncWorker>()
            .setInputData(
                Data.Builder()
                    .putString(OfflinePackSyncWorker.KEY_LOCATION_LABEL, locationLabel)
                    .putDouble(OfflinePackSyncWorker.KEY_LAT, lat)
                    .putDouble(OfflinePackSyncWorker.KEY_LNG, lng)
                    .build()
            )
            .build()

        val workManager = WorkManager.getInstance(getApplication())
        workManager.enqueue(request)

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { info ->
                _state.value = when (info?.state) {
                    WorkInfo.State.SUCCEEDED -> SyncUiState.SUCCESS
                    WorkInfo.State.FAILED -> SyncUiState.FAILED
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> SyncUiState.RUNNING
                    else -> _state.value
                }
            }
        }
    }
}
