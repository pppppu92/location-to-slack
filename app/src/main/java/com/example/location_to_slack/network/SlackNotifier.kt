package com.example.location_to_slack.network

import android.content.Context
import android.util.Log
import com.example.location_to_slack.BuildConfig
import com.example.location_to_slack.data.Checkpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Slack Incoming Webhook への通知送信と連続送信防止を管理するクラス
 */
class SlackNotifier private constructor(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // チェックポイントIDごとの最終通知時刻 (ミリ秒)
    private val lastNotifiedTimeMap = ConcurrentHashMap<Long, Long>()

    // チェックポイントIDごとの現在エリア滞在フラグ
    private val isInsideAreaMap = ConcurrentHashMap<Long, Boolean>()

    /**
     * チェックポイントに進入した際の通知処理
     */
    suspend fun notifyCheckpointEntered(checkpoint: Checkpoint) = withContext(Dispatchers.IO) {
        val checkpointId = checkpoint.id
        val currentTime = System.currentTimeMillis()
        val lastNotifiedTime = lastNotifiedTimeMap[checkpointId] ?: 0L
        val isAlreadyInside = isInsideAreaMap[checkpointId] ?: false

        // 連続送信防止制御:
        // 1. すでにエリア内にいて通知済み、かつクールダウン時間（30分）未満の場合は送信しない
        if (isAlreadyInside && (currentTime - lastNotifiedTime < COOLDOWN_MILLIS)) {
            Log.d(TAG, "Skip Slack notification for [${checkpoint.name}]: already inside and in cooldown.")
            return@withContext
        }

        // 2. エリアから一度出た場合でも、前回の通知から最低クールダウン（例: 5分）未満なら過剰通知を防ぐ
        if (currentTime - lastNotifiedTime < MIN_RENOTIFICATION_INTERVAL_MILLIS) {
            Log.d(TAG, "Skip Slack notification for [${checkpoint.name}]: too frequent.")
            return@withContext
        }

        val webhookUrl = BuildConfig.SLACK_WEBHOOK_URL
        if (webhookUrl.isBlank() || webhookUrl.contains("YOUR/WEBHOOK/URL")) {
            Log.w(
                TAG,
                "Slack Webhook URL is not configured. Please set SLACK_WEBHOOK_URL in local.properties."
            )
            // 開発・テスト時のログ出力
            Log.i(TAG, "[Preview Slack Message]\n${buildMessageText(checkpoint)}")
            isInsideAreaMap[checkpointId] = true
            lastNotifiedTimeMap[checkpointId] = currentTime
            return@withContext
        }

        val messageText = buildMessageText(checkpoint)
        val jsonPayload = JSONObject().apply {
            put("text", messageText)
        }.toString()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(webhookUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Successfully sent Slack notification for [${checkpoint.name}]")
                    // 送信成功時に状態を記録
                    lastNotifiedTimeMap[checkpointId] = currentTime
                    isInsideAreaMap[checkpointId] = true
                } else {
                    Log.e(TAG, "Failed to send Slack notification. HTTP code: ${response.code}, body: ${response.body?.string()}")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error when sending Slack notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error when sending Slack notification", e)
        }
    }

    /**
     * チェックポイントから退出したときの処理（エリア内外状態を更新）
     */
    fun onCheckpointExited(checkpoint: Checkpoint) {
        val checkpointId = checkpoint.id
        Log.d(TAG, "Exited area for [${checkpoint.name}]. Resetting inside flag.")
        isInsideAreaMap[checkpointId] = false
    }

    /**
     * 要件通りのメッセージフォーマットを生成
     *
     * 【フォーマット】
     * (チェックポイント名)を通過しました。
     * https://www.google.com/maps/search/?api=1&query=(指定した緯度),(指定した経度)
     */
    private fun buildMessageText(checkpoint: Checkpoint): String {
        return "${checkpoint.name}を通過しました。\nhttps://www.google.com/maps/search/?api=1&query=${checkpoint.latitude},${checkpoint.longitude}"
    }

    companion object {
        private const val TAG = "SlackNotifier"

        // 同一エリア滞在中の再通知クールダウン時間 (30分)
        private const val COOLDOWN_MILLIS = 30 * 60 * 1000L

        // 一度退出しても再進入時に即座に連投されるのを防ぐ最小間隔 (5分)
        private const val MIN_RENOTIFICATION_INTERVAL_MILLIS = 5 * 60 * 1000L

        @Volatile
        private var INSTANCE: SlackNotifier? = null

        fun getInstance(context: Context): SlackNotifier {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SlackNotifier(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
