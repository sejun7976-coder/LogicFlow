package com.example.logicflow.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logicflow.data.local.AnalysisResultEntity
import com.example.logicflow.data.repository.LogicFlowRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsSummary(
    val averageScore: Int,
    val totalCount: Int,
    val categoryCounts: Map<String, Int>,
    val litCount: Int,
    val nonLitCount: Int
)

class StatsViewModel(
    private val repository: LogicFlowRepository
) : ViewModel() {

    val allResults: StateFlow<List<AnalysisResultEntity>> = repository.getAllAnalysisResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsData: StateFlow<StatsSummary> = allResults.map { results ->
        val averageScore = if (results.isEmpty()) 0 else results.map { it.score }.average().toInt()
        val totalCount = results.size
        
        val categoryCounts = mutableMapOf("인식론" to 0, "컴퓨터 과학" to 0, "윤리학" to 0)
        results.forEach { res ->
            val cat = when {
                res.passageTitle.contains("JTB") -> "인식론"
                res.passageTitle.contains("튜링") -> "컴퓨터 과학"
                else -> "윤리학"
            }
            categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1
        }

        val litCount = results.count { it.passageType == "문학" }
        val nonLitCount = results.count { it.passageType == "비문학" }

        StatsSummary(
            averageScore = averageScore,
            totalCount = totalCount,
            categoryCounts = categoryCounts,
            litCount = litCount,
            nonLitCount = nonLitCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsSummary(0, 0, mapOf("인식론" to 0, "컴퓨터 과학" to 0, "윤리학" to 0), 0, 0))
}
