package com.example.location_to_slack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.location_to_slack.data.AppDatabase
import com.example.location_to_slack.data.CheckpointRepository
import com.example.location_to_slack.geofence.GeofenceManager
import com.example.location_to_slack.util.MonitoringPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * デバイスブート時にジオフェンス監視を再開する BroadcastReceiver
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }

        if (shouldRestart(intent?.action, MonitoringPreferences.isMonitoringEnabled(context))) {
            Log.d(TAG, "Boot completed: Restarting geofence monitoring")
            val pending = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val repository = CheckpointRepository(database.checkpointDao())
                    val activeCheckpoints = repository.getActiveCheckpointsList()

                    if (activeCheckpoints.isEmpty()) {
                        Log.d(TAG, "No active checkpoints to register")
                        pending.finish()
                    } else {
                        val geofenceManager = GeofenceManager(context)
                        geofenceManager.registerGeofences(activeCheckpoints)
                        Log.d(TAG, "Geofences reregistered successfully")
                        pending.finish()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restarting geofence monitoring", e)
                    pending.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"

        /**
         * サービスを再開するべきかを判定する純粋Kotlin関数
         */
        internal fun shouldRestart(action: String?, monitoringEnabled: Boolean): Boolean {
            return action == ACTION_BOOT_COMPLETED && monitoringEnabled
        }
    }
}
