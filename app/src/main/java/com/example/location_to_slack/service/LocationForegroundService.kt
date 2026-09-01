package com.example.location_to_slack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.location_to_slack.MainActivity
import com.example.location_to_slack.R
import com.example.location_to_slack.data.AppDatabase
import com.example.location_to_slack.data.CheckpointRepository
import com.example.location_to_slack.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ジオフェンス監視を維持するためのフォアグラウンドサービス
 */
class LocationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var repository: CheckpointRepository

    override fun onCreate() {
        super.onCreate()
        geofenceManager = GeofenceManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = CheckpointRepository(database.checkpointDao())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                startMonitoring()
            }
            ACTION_STOP -> {
                stopMonitoring()
            }
            ACTION_UPDATE_GEOFENCES -> {
                updateGeofences()
            }
        }

        return START_STICKY
    }

    private fun startMonitoring() {
        _isRunning.value = true

        val notification = createNotification("チェックポイント監視中（ジオフェンス稼働中）")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        updateGeofences()
    }

    private fun updateGeofences() {
        serviceScope.launch {
            try {
                val activeCheckpoints = repository.getActiveCheckpointsList()
                Log.d(TAG, "Registering active checkpoints: ${activeCheckpoints.size}")
                geofenceManager.registerGeofences(activeCheckpoints)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating geofences", e)
            }
        }
    }

    private fun stopMonitoring() {
        _isRunning.value = false
        geofenceManager.removeGeofences()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ジオフェンス監視サービス",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "位置情報の進入検知・Slack通知サービスの稼働状況を表示します"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Slack位置通知アプリ")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "LocationForegroundService"
        const val CHANNEL_ID = "location_monitoring_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_GEOFENCES = "ACTION_UPDATE_GEOFENCES"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}
