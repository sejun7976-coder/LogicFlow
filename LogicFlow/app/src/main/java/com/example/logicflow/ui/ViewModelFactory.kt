package com.example.logicflow.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.logicflow.data.repository.LogicFlowRepository
import com.example.logicflow.ui.home.HomeViewModel
import com.example.logicflow.ui.history.HistoryViewModel
import com.example.logicflow.ui.reading.ReadingViewModel
import com.example.logicflow.ui.analysis.AnalysisResultViewModel
import com.example.logicflow.ui.settings.SettingsViewModel
import com.example.logicflow.ui.stats.StatsViewModel

class ViewModelFactory(
    private val repository: LogicFlowRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository, context) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ReadingViewModel::class.java) -> {
                ReadingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AnalysisResultViewModel::class.java) -> {
                AnalysisResultViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(repository, context) as T
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                StatsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
