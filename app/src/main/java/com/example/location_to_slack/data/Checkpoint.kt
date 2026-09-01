package com.example.location_to_slack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * チェックポイントのデータモデル (Room Entity)
 *
 * @param id 一意のID (自動生成)
 * @param name チェックポイントの名前 (例: 自宅、会社、大学など)
 * @param latitude 緯度
 * @param longitude 経度
 * @param radius 進入検知半径（メートル、デフォルト100m）
 * @param isEnabled 監視対象として有効かどうか
 */
@Entity(tableName = "checkpoints")
data class Checkpoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 100f,
    val isEnabled: Boolean = true
)
