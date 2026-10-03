package com.example.location_to_slack.util

import android.content.Context
import android.content.SharedPreferences

/**
 * ジオフェンス監視の有効/無効状態を永続化するための共有プリファレンス管理クラス
 */
object MonitoringPreferences {
    private const val PREF_NAME = "monitoring_prefs"
    private const val KEY_MONITORING_ENABLED = "is_monitoring_enabled"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 監視有効状態を保存する
     */
    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    /**
     * 監視有効状態を取得する
     */
    fun isMonitoringEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_MONITORING_ENABLED, false)
    }
}
