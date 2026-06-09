package com.example.logicflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogicFlowDao {
    // Passages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassages(passages: List<PassageEntity>)

    @Query("SELECT * FROM passages")
    fun getAllPassages(): Flow<List<PassageEntity>>

    @Query("SELECT * FROM passages WHERE id = :id LIMIT 1")
    suspend fun getPassageById(id: String): PassageEntity?

    @Query("SELECT * FROM passages WHERE difficulty = :difficulty")
    fun getPassagesByDifficulty(difficulty: String): Flow<List<PassageEntity>>

    @Query("SELECT * FROM passages WHERE difficulty = :difficulty ORDER BY LENGTH(id) ASC, id ASC LIMIT :limit OFFSET :offset")
    suspend fun getPassagesByDifficultyPaginated(difficulty: String, limit: Int, offset: Int): List<PassageEntity>

    @Query("SELECT COUNT(*) FROM passages")
    suspend fun getPassageCount(): Int

    // Analysis Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysisResult(result: AnalysisResultEntity)

    @Query("SELECT * FROM analysis_results ORDER BY timestamp DESC")
    fun getAllAnalysisResults(): Flow<List<AnalysisResultEntity>>

    @Query("SELECT * FROM analysis_results WHERE id = :id LIMIT 1")
    fun getAnalysisResultById(id: String): Flow<AnalysisResultEntity?>

    @Query("DELETE FROM analysis_results WHERE id = :id")
    suspend fun deleteAnalysisResultById(id: String)

    // Notification Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLogEntity)

    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllNotificationLogs(): Flow<List<NotificationLogEntity>>

    @Query("DELETE FROM notification_logs WHERE id = :id")
    suspend fun deleteNotificationLogById(id: String)
}
