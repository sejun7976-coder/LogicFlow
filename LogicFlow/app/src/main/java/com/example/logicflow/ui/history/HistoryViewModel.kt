package com.example.logicflow.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logicflow.data.local.AnalysisResultEntity
import com.example.logicflow.data.repository.LogicFlowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: LogicFlowRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allResults: StateFlow<List<AnalysisResultEntity>> = repository.getAllAnalysisResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredResults: StateFlow<List<AnalysisResultEntity>> = combine(
        allResults,
        _searchQuery
    ) { results, query ->
        if (query.isBlank()) {
            results
        } else {
            results.filter { result ->
                result.passageTitle.contains(query, ignoreCase = true) ||
                result.userSummary.contains(query, ignoreCase = true) ||
                result.aiFeedback.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteResult(id: String) {
        viewModelScope.launch {
            repository.deleteAnalysisResultById(id)
        }
    }
}
