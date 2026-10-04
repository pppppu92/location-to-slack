package com.example.location_to_slack.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.location_to_slack.data.Checkpoint
import com.example.location_to_slack.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Geofencing API の登録・解除を管理するヘルパークラス
 */
class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    // ジオフェンスイベントを受け取る PendingIntent
    private val geofencePendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * チェックポイントリストからジオフェンスを登録する
     * 結果は onResult に通知する（成功時は null、失敗時は画面表示用のエラーメッセージ）
     */
    @SuppressLint("MissingPermission")
    fun registerGeofences(checkpoints: List<Checkpoint>, onResult: (String?) -> Unit = {}) {
        if (checkpoints.isEmpty()) {
            Log.d(TAG, "No checkpoints to register.")
            removeGeofences()
            onResult(null)
            return
        }

        val geofenceList = checkpoints.map { checkpoint ->
            Geofence.Builder()
                // requestId として checkpoint.id を文字列化したものを設定
                .setRequestId(checkpoint.id.toString())
                .setCircularRegion(checkpoint.latitude, checkpoint.longitude, checkpoint.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // 進入 (ENTER) と 退出 (EXIT) の両方を監視（再通知制御・状態管理用）
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        // 既存のジオフェンスをクリアしてから新しく登録
        geofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully registered ${geofenceList.size} geofences.")
                    onResult(null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofences", e)
                    onResult(errorMessage((e as? ApiException)?.statusCode))
                }
        }
    }

    /**
     * 登録中のジオフェンスをすべて解除する
     */
    fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener { Log.d(TAG, "Geofences removed.") }
    }

    companion object {
        private const val TAG = "GeofenceManager"

        /**
         * 登録失敗のステータスコードを画面表示用のメッセージに変換する
         */
        internal fun errorMessage(statusCode: Int?): String = when (statusCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "位置情報サービスが無効のため監視できません"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "チェックポイントが上限を超えているため監視できません"
            GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION -> "位置情報の権限がないため監視できません"
            else -> "ジオフェンスの登録に失敗しました"
        }
    }
}
