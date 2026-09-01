package com.example.location_to_slack.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.location_to_slack.data.Checkpoint
import com.example.location_to_slack.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Geofencing API の登録・解除を管理するヘルパークラス
 */
class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    // ジオフェンスイベントを受け取る PendingIntent
    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * チェックポイントリストからジオフェンスを登録する
     */
    @SuppressLint("MissingPermission")
    fun registerGeofences(
        checkpoints: List<Checkpoint>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (checkpoints.isEmpty()) {
            Log.d(TAG, "No checkpoints to register.")
            removeGeofences()
            return
        }

        val geofenceList = checkpoints.map { checkpoint ->
            Geofence.Builder()
                // requestId として checkpoint.id を文字列化したものを設定
                .setRequestId(checkpoint.id.toString())
                .setCircularRegion(
                    checkpoint.latitude,
                    checkpoint.longitude,
                    checkpoint.radius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // 進入 (ENTER) と 退出 (EXIT) の両方を監視（再通知制御・状態管理用）
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()

        // 既存のジオフェンスをクリアしてから新しく登録
        geofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered ${geofenceList.size} geofences.")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofences", e)
                    onFailure(e)
                }
        }
    }

    /**
     * 登録中のジオフェンスをすべて解除する
     */
    fun removeGeofences(
        onComplete: () -> Unit = {}
    ) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                Log.d(TAG, "Geofences removed.")
                onComplete()
            }
    }

    companion object {
        private const val TAG = "GeofenceManager"
    }
}
