package com.example.location_to_slack.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.location_to_slack.data.Checkpoint
import com.example.location_to_slack.data.CheckpointRepository
import com.example.location_to_slack.service.LocationForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val context = getApplication<Application>().applicationContext
        if (isServiceRunning.value) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = LocationForegroundService.ACTION_STOP
            }
            context.startService(intent)
        } else {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = LocationForegroundService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    /**
     * チェックポイントの追加
     */
    fun addCheckpoint(name: String, latitude: Double, longitude: Double, radius: Float = 100f) {
        viewModelScope.launch {
            val checkpoint = Checkpoint(
                name = name,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                isEnabled = true
            )
            repository.insert(checkpoint)
            // サービス実行中ならジオフェンスを再登録するよう通知
            updateGeofencesIfRunning()
        }
    }

    /**
     * チェックポイントの更新
     */
    fun updateCheckpoint(checkpoint: Checkpoint) {
        viewModelScope.launch {
            repository.update(checkpoint)
            updateGeofencesIfRunning()
        }
    }

    /**
     * チェックポイントの有効/無効切り替え
     */
    fun toggleCheckpoint(checkpoint: Checkpoint) {
        viewModelScope.launch {
            repository.update(checkpoint.copy(isEnabled = !checkpoint.isEnabled))
            updateGeofencesIfRunning()
        }
    }

    /**
     * チェックポイントの削除
     */
    fun deleteCheckpoint(checkpoint: Checkpoint) {
        viewModelScope.launch {
            repository.delete(checkpoint)
            updateGeofencesIfRunning()
        }
    }

    private fun updateGeofencesIfRunning() {
        if (isServiceRunning.value) {
            val context = getApplication<Application>().applicationContext
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = LocationForegroundService.ACTION_UPDATE_GEOFENCES
            }
            context.startService(intent)
        }
    }
}

class CheckpointViewModelFactory(
    private val application: Application,
    private val repository: CheckpointRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckpointViewModel::class.java)) {
            return CheckpointViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
