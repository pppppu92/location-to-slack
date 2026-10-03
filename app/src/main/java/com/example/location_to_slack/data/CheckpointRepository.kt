package com.example.location_to_slack.data

import kotlinx.coroutines.flow.Flow

/**
 * チェックポイントデータの操作を集約するリポジトリ
 */
class CheckpointRepository(private val checkpointDao: CheckpointDao) {

    val allCheckpoints: Flow<List<Checkpoint>> = checkpointDao.getAllCheckpoints()

    suspend fun getActiveCheckpointsList() = checkpointDao.getActiveCheckpointsList()

    suspend fun getCheckpointById(id: Long) = checkpointDao.getCheckpointById(id)

    suspend fun insert(checkpoint: Checkpoint) = checkpointDao.insertCheckpoint(checkpoint)

    suspend fun update(checkpoint: Checkpoint) = checkpointDao.updateCheckpoint(checkpoint)

    suspend fun delete(checkpoint: Checkpoint) = checkpointDao.deleteCheckpoint(checkpoint)
}
