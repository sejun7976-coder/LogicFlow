package com.example.logicflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PassageEntity::class, AnalysisResultEntity::class, NotificationLogEntity::class], version = 9, exportSchema = false)
abstract class LogicFlowDatabase : RoomDatabase() {

    abstract fun logicFlowDao(): LogicFlowDao

    companion object {
        @Volatile
        private var INSTANCE: LogicFlowDatabase? = null

        fun getDatabase(context: Context): LogicFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogicFlowDatabase::class.java,
                    "logicflow_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabase(dao: LogicFlowDao) {
            // No passages seeded at startup. Passages are fetched from the API on-demand.
        }
    }
}
