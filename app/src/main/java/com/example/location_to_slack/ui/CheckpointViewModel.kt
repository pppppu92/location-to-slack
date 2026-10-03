package com.example.location_to_slack.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.location_to_slack.data.Checkpoint
import com.example.location_to_slack.data.CheckpointRepository
import com.example.location_to_slack.service.LocationForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UIとデータ層・サービスを仲介する ViewModel
 */
class CheckpointViewModel(
    application: Application,
    private val repository: CheckpointRepository
) : AndroidViewModel(application) {

    // 全チェックポイントの監視用 StateFlow
    val checkpoints: StateFlow<List<Checkpoint>> = repository.allCheckpoints
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 監視サービスが起動中かどうかの状態
    val isServiceRunning: StateFlow<Boolean> = LocationForegroundService.isRunning

    /**
     * 監視サービス (フォアグラウンドサービス) の開始 / 停止を切り替える
     */
    fun toggleMonitoringService() {
        val app = getApplication<Application>()
        if (isServiceRunning.value) {
            app.startService(serviceIntent(LocationForegroundService.ACTION_STOP))
        } else {
            app.startForegroundService(serviceIntent(LocationForegroundService.ACTION_START))
        }
    }

    fun addCheckpoint(name: String, latitude: Double, longitude: Double, radius: Float) = saveAndUpdateGeofences {
        repository.insert(Checkpoint(name = name, latitude = latitude, longitude = longitude, radius = radius))
    }

    fun updateCheckpoint(checkpoint: Checkpoint) = saveAndUpdateGeofences {
        repository.update(checkpoint)
    }

    /**
     * チェックポイントの有効/無効切り替え
     */
    fun toggleCheckpoint(checkpoint: Checkpoint) = updateCheckpoint(checkpoint.copy(isEnabled = !checkpoint.isEnabled))

    fun deleteCheckpoint(checkpoint: Checkpoint) = saveAndUpdateGeofences {
        repository.delete(checkpoint)
    }

    // DB を更新し、サービス実行中ならジオフェンスを再登録するよう通知
    private fun saveAndUpdateGeofences(save: suspend () -> Unit) = viewModelScope.launch {
        save()
        if (isServiceRunning.value) {
            getApplication<Application>().startService(serviceIntent(LocationForegroundService.ACTION_UPDATE_GEOFENCES))
        }
    }

    private fun serviceIntent(action: String) =
        Intent(getApplication<Application>(), LocationForegroundService::class.java).setAction(action)
}
