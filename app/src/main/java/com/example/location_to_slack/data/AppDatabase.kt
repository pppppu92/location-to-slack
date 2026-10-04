package com.example.location_to_slack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * アプリケーションの Room データベース
 *
 * version を上げるときは Migration（または AutoMigration）を追加すること。
 * スキーマは app/schemas に出力される。
 */
@Database(entities = [Checkpoint::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun checkpointDao(): CheckpointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "geofence_slack_database"
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
