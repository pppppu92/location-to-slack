package com.example.location_to_slack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * アプリケーションの Room データベース
 */
@Database(entities = [Checkpoint::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun checkpointDao(): CheckpointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "geofence_slack_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
