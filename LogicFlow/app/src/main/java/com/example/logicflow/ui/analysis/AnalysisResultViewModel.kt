package com.example.logicflow.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logicflow.data.local.AnalysisResultEntity
import com.example.logicflow.data.repository.ChatMessage
import com.example.logicflow.data.repository.LogicFlowRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnalysisResultViewModel(
    private val repository: LogicFlowRepository
) : ViewModel() {

    private val gson = Gson()

    private val _currentResult = MutableStateFlow<AnalysisResultEntity?>(null)
    val currentResult: StateFlow<AnalysisResultEntity?> = _currentResult

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadResult(resultId: String) {
        viewModelScope.launch {
            repository.getAnalysisResultById(resultId).collectLatest { result ->
                _currentResult.value = result
                if (result != null) {
                    val type = object : TypeToken<List<ChatMessage>>() {}.type
                    val history: List<ChatMessage> = try {
                        gson.fromJson(result.chatHistoryJson, type) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    _chatHistory.value = history.ifEmpty {
                        listOf(
                            ChatMessage(
                                role = "model",
                                content = "안녕하세요! '${result.passageTitle}' 지문 요약에 대한 채점 결과가 완료되었습니다. 점수나 피드백에 대해 궁금한 점이 있으시다면 언제든 질문해 주세요!"
                            )
                        )
                    }
                }
            }
        }
    }

    fun updateChatInput(input: String) {
        _chatInput.value = input
    }

    fun sendChatMessage() {
        val result = _currentResult.value ?: return
        val question = _chatInput.value.trim()
        if (question.isEmpty()) return

        // Temporarily append user question to history for immediate UI feedback
        val updatedHistory = _chatHistory.value.toMutableList()
        updatedHistory.add(ChatMessage(role = "user", content = question))
        _chatHistory.value = updatedHistory
        
        _chatInput.value = ""
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            repository.chatWithAI(result.id, question)
                .onSuccess {
                    // Automatically updated by Flow collector
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "질문에 답할 수 없습니다. 설정을 확인해 보세요."
                }
            _isGenerating.value = false
        }
    }
}
