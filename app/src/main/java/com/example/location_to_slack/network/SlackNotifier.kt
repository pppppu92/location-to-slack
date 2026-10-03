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
import java.util.concurrent.TimeUnit

/**
 * Slack Incoming Webhook への通知送信と連続送信防止を管理するシングルトン
 */
object SlackNotifier {

    private const val TAG = "SlackNotifier"

    // SharedPreferences 設定
    private const val PREFS_NAME = "notification_state"
    private const val PREF_KEY_LAST_NOTIFIED_PREFIX = "last_"
    private const val PREF_KEY_INSIDE_PREFIX = "inside_"

    // 同一エリア滞在中の再通知クールダウン時間 (30分)
    private const val COOLDOWN_MILLIS = 30 * 60 * 1000L

    // 一度退出しても再進入時に即座に連投されるのを防ぐ最小間隔 (5分)
    private const val MIN_RENOTIFICATION_INTERVAL_MILLIS = 5 * 60 * 1000L

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 【フォーマット】
     * (チェックポイント名)を通過しました。
     * https://www.google.com/maps/search/?api=1&query=(指定した緯度),(指定した経度)
     */
    internal fun buildMessageText(checkpoint: Checkpoint): String =
        "${checkpoint.name}を通過しました。\n" +
            "https://www.google.com/maps/search/?api=1&query=${checkpoint.latitude},${checkpoint.longitude}"

    /**
     * チェックポイントに進入した際の通知処理
     */
    suspend fun notifyCheckpointEntered(context: Context, checkpoint: Checkpoint) = withContext(Dispatchers.IO) {
        val checkpointId = checkpoint.id
        val currentTime = System.currentTimeMillis()
        val lastNotifiedTime = getLastNotifiedTime(context, checkpointId)
        val elapsed = currentTime - lastNotifiedTime
        val isInside = isInsideArea(context, checkpointId)

        // 連続送信防止制御
        if (!shouldNotify(isInside, elapsed)) {
            Log.d(TAG, "Skip Slack notification for [${checkpoint.name}]: duplicate or cooldown.")
            return@withContext
        }

        val messageText = buildMessageText(checkpoint)

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
            setLastNotifiedTime(context, checkpointId, currentTime)
            setInsideArea(context, checkpointId, true)
        }
    }

    /**
     * チェックポイントから退出したときの処理（エリア内外状態を更新）
     */
    fun onCheckpointExited(context: Context, checkpoint: Checkpoint) {
        Log.d(TAG, "Exited area for [${checkpoint.name}]. Resetting inside flag.")
        setInsideArea(context, checkpoint.id, false)
    }

    /**
     * 通知を送信すべきかどうかを判定する
     * @param isInside 現在エリア内かどうか
     * @param elapsedMillis 最後の通知からの経過時間（ミリ秒）
     * @return 通知を送信すべき場合は true
     */
    internal fun shouldNotify(isInside: Boolean, elapsedMillis: Long): Boolean {
        // すでにエリア内にいて通知済み、かつクールダウン時間（30分）未満の場合は送信しない
        if (isInside && elapsedMillis < COOLDOWN_MILLIS) {
            return false
        }
        // エリアから一度出た場合でも、前回の通知から最低クールダウン（5分）未満なら過剰通知を防ぐ
        if (elapsedMillis < MIN_RENOTIFICATION_INTERVAL_MILLIS) {
            return false
        }
        return true
    }

    private fun getLastNotifiedTime(context: Context, checkpointId: Long): Long {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("$PREF_KEY_LAST_NOTIFIED_PREFIX$checkpointId", 0L)
    }

    private fun setLastNotifiedTime(context: Context, checkpointId: Long, time: Long) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("$PREF_KEY_LAST_NOTIFIED_PREFIX$checkpointId", time).apply()
    }

    private fun isInsideArea(context: Context, checkpointId: Long): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$PREF_KEY_INSIDE_PREFIX$checkpointId", false)
    }

    private fun setInsideArea(context: Context, checkpointId: Long, inside: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("$PREF_KEY_INSIDE_PREFIX$checkpointId", inside).apply()
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
