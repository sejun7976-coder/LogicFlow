package com.example.logicflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passages")
data class PassageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val difficulty: String, // "하", "중", "상"
    val category: String,   // "논리학", "철학", "과학" 등
    val recommendTimeSec: Int,
    val modelSummary: String
)

@Entity(tableName = "analysis_results")
data class AnalysisResultEntity(
    @PrimaryKey val id: String, // UUID or Timestamp
    val passageId: String,
    val passageTitle: String,
    val userSummary: String,
    val score: Int,
    val grade: String,
    val semanticMatch: Int,
    val contextPreservation: Int,
    val premiseCheck: Boolean,
    val premiseDetail: String,
    val inferenceCheck: Boolean,
    val inferenceDetail: String,
    val exceptionCheck: Boolean,
    val exceptionDetail: String,
    val aiFeedback: String,
    val correctedText: String,
    val aiSummary: String,
    val chatHistoryJson: String, // Serialized list of chat messages
    val timestamp: Long,
    val passageType: String = "비문학"
)
