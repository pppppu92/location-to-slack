package com.example.location_to_slack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.location_to_slack.data.AppDatabase
import com.example.location_to_slack.data.CheckpointRepository
import com.example.location_to_slack.network.SlackNotifier
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ジオフェンスの進入・退出イベントを受信する BroadcastReceiver
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $errorMessage (Code: ${geofencingEvent.errorCode})")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        Log.d(TAG, "Geofence transition: $transitionType, count: ${triggeringGeofences.size}")

        // 非同期でDB検索およびSlack通知処理を実行
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = CheckpointRepository(AppDatabase.getDatabase(context).checkpointDao())

                for (geofence in triggeringGeofences) {
                    val checkpointId = geofence.requestId.toLongOrNull() ?: continue
                    val checkpoint = repository.getCheckpointById(checkpointId) ?: continue

                    when (transitionType) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            Log.d(TAG, "Entered checkpoint: ${checkpoint.name}")
                            SlackNotifier.notifyCheckpointEntered(context, checkpoint)
                        }
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            Log.d(TAG, "Exited checkpoint: ${checkpoint.name}")
                            SlackNotifier.onCheckpointExited(checkpoint)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
