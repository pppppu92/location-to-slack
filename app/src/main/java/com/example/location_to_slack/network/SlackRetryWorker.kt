package com.example.location_to_slack.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.location_to_slack.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 送信に失敗した Slack 通知を、ネットワーク接続時に再送する Worker
 */
class SlackRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val text = inputData.getString(KEY_TEXT) ?: return@withContext Result.failure()
        when {
            SlackNotifier.postMessage(BuildConfig.SLACK_WEBHOOK_URL, text) -> Result.success()
            // 設定ミスなどで成功しない送信を無限に繰り返さない
            runAttemptCount + 1 >= MAX_ATTEMPTS -> Result.failure()
            else -> Result.retry()
        }
    }

    companion object {
        const val KEY_TEXT = "text"
        private const val MAX_ATTEMPTS = 5
    }
}
