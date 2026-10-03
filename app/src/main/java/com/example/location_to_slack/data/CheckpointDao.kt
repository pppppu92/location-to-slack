package com.example.location_to_slack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * チェックポイント用 Data Access Object (DAO)
 */
@Dao
interface CheckpointDao {

    @Query("SELECT * FROM checkpoints ORDER BY id DESC")
    fun getAllCheckpoints(): Flow<List<Checkpoint>>

    @Query("SELECT * FROM checkpoints WHERE isEnabled = 1")
    suspend fun getActiveCheckpointsList(): List<Checkpoint>

    @Query("SELECT * FROM checkpoints WHERE id = :id LIMIT 1")
    suspend fun getCheckpointById(id: Long): Checkpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoint(checkpoint: Checkpoint): Long

    @Update
    suspend fun updateCheckpoint(checkpoint: Checkpoint)

    @Delete
    suspend fun deleteCheckpoint(checkpoint: Checkpoint)
}
