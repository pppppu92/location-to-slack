package com.example.location_to_slack.network

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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Slack Incoming Webhook への通知送信と連続送信防止を管理するシングルトン
 */
object SlackNotifier {

    private const val TAG = "SlackNotifier"

    // 同一エリア滞在中の再通知クールダウン時間 (30分)
    private const val COOLDOWN_MILLIS = 30 * 60 * 1000L

    // 一度退出しても再進入時に即座に連投されるのを防ぐ最小間隔 (5分)
    private const val MIN_RENOTIFICATION_INTERVAL_MILLIS = 5 * 60 * 1000L

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
        val elapsed = currentTime - (lastNotifiedTimeMap[checkpointId] ?: 0L)

        // 連続送信防止制御:
        // 1. すでにエリア内にいて通知済み、かつクールダウン時間（30分）未満の場合は送信しない
        if (isInsideAreaMap[checkpointId] == true && elapsed < COOLDOWN_MILLIS) {
            Log.d(TAG, "Skip Slack notification for [${checkpoint.name}]: already inside and in cooldown.")
            return@withContext
        }

        // 2. エリアから一度出た場合でも、前回の通知から最低クールダウン（5分）未満なら過剰通知を防ぐ
        if (elapsed < MIN_RENOTIFICATION_INTERVAL_MILLIS) {
            Log.d(TAG, "Skip Slack notification for [${checkpoint.name}]: too frequent.")
            return@withContext
        }

        // 【フォーマット】
        // (チェックポイント名)を通過しました。
        // https://www.google.com/maps/search/?api=1&query=(指定した緯度),(指定した経度)
        val messageText = "${checkpoint.name}を通過しました。\n" +
            "https://www.google.com/maps/search/?api=1&query=${checkpoint.latitude},${checkpoint.longitude}"

        val webhookUrl = BuildConfig.SLACK_WEBHOOK_URL
        val sent = if (webhookUrl.isBlank() || webhookUrl.contains("YOUR/WEBHOOK/URL")) {
            // 未設定時は開発・テスト用にログ出力のみ行い、送信済みとして扱う
            Log.w(TAG, "Slack Webhook URL is not configured. Please set SLACK_WEBHOOK_URL in local.properties.")
            Log.i(TAG, "[Preview Slack Message]\n$messageText")
            true
        } else {
            postMessage(webhookUrl, messageText)
        }

        if (sent) {
            lastNotifiedTimeMap[checkpointId] = currentTime
            isInsideAreaMap[checkpointId] = true
        }
    }

    /**
     * チェックポイントから退出したときの処理（エリア内外状態を更新）
     */
    fun onCheckpointExited(checkpoint: Checkpoint) {
        Log.d(TAG, "Exited area for [${checkpoint.name}]. Resetting inside flag.")
        isInsideAreaMap[checkpoint.id] = false
    }

    private fun postMessage(webhookUrl: String, text: String): Boolean = try {
        val request = Request.Builder()
            .url(webhookUrl)
            .post(
                JSONObject().put("text", text).toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Log.i(TAG, "Successfully sent Slack notification.")
            } else {
                Log.e(TAG, "Failed to send Slack notification. HTTP code: ${response.code}, body: ${response.body?.string()}")
            }
            response.isSuccessful
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error when sending Slack notification", e)
        false
    }
}
