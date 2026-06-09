package com.example.logicflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long
)
