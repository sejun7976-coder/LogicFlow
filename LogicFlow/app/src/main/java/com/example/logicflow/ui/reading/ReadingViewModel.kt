package com.example.logicflow.ui.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logicflow.data.local.PassageEntity
import com.example.logicflow.data.repository.LogicFlowRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EvaluationUiState {
    object Idle : EvaluationUiState
    object Loading : EvaluationUiState
    data class Success(val resultId: String) : EvaluationUiState
    data class Error(val message: String) : EvaluationUiState
}

class ReadingViewModel(
    private val repository: LogicFlowRepository
) : ViewModel() {

    private val _selectedPassage = MutableStateFlow<PassageEntity?>(null)
    val selectedPassage: StateFlow<PassageEntity?> = _selectedPassage

    private val _difficultyFilter = MutableStateFlow("하")
    val difficultyFilter: StateFlow<String> = _difficultyFilter

    private val _passages = MutableStateFlow<List<PassageEntity>>(emptyList())
    val filteredPassages: StateFlow<List<PassageEntity>> = _passages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore

    private var currentOffset = 0
    private val PAGE_SIZE = 10
    private var loadJob: Job? = null

    init {
        loadNextPage(reset = true)
    }

    fun setDifficultyFilter(filter: String) {
        if (_difficultyFilter.value == filter && _passages.value.isNotEmpty()) return
        _difficultyFilter.value = filter
        loadNextPage(reset = true)
    }

    fun loadNextPage(reset: Boolean = false) {
        if (!reset && _isLoading.value) return
        if (!reset && !_hasMore.value) return

        if (reset) {
            loadJob?.cancel()
            currentOffset = 0
            _hasMore.value = true
        }

        _isLoading.value = true

        loadJob = viewModelScope.launch {
            try {
                val newItems = repository.getPassagesByDifficultyPaginated(
                    difficulty = _difficultyFilter.value,
                    limit = PAGE_SIZE,
                    offset = currentOffset
                )
                if (reset) {
                    _passages.value = newItems
                } else {
                    _passages.value = (_passages.value + newItems).distinctBy { it.id }
                }
                _hasMore.value = newItems.size >= PAGE_SIZE
                currentOffset += newItems.size
            } catch (e: Exception) {
                // handle exception quietly
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Timer logic
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning

    private var timerJob: Job? = null

    // Summary inputs
    private val _userSummaryText = MutableStateFlow("")
    val userSummaryText: StateFlow<String> = _userSummaryText

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // API submission state
    private val _evaluationState = MutableStateFlow<EvaluationUiState>(EvaluationUiState.Idle)
    val evaluationState: StateFlow<EvaluationUiState> = _evaluationState

    // STT partial text logic
    private val _sttPartialText = MutableStateFlow("")
    val sttPartialText: StateFlow<String> = _sttPartialText

    // STT volume logic for soundwave visualization
    private val _sttVolume = MutableStateFlow(0f)
    val sttVolume: StateFlow<Float> = _sttVolume

    fun selectPassage(passage: PassageEntity?) {
        _selectedPassage.value = passage
        resetTimer()
        _userSummaryText.value = ""
        _sttPartialText.value = ""
        _sttVolume.value = 0f
        _evaluationState.value = EvaluationUiState.Idle
        if (passage != null) {
            startTimer()
        } else {
            stopTimer()
        }
    }

    fun updateSummaryText(text: String) {
        _userSummaryText.value = text
    }

    fun appendSummaryText(text: String) {
        val current = _userSummaryText.value
        _userSummaryText.value = if (current.isEmpty()) text else "$current $text"
    }

    fun updatePartialText(text: String) {
        _sttPartialText.value = text
    }

    fun commitPartialText(text: String) {
        _sttPartialText.value = ""
        _sttVolume.value = 0f
        appendSummaryText(text)
    }

    fun clearPartialText() {
        _sttPartialText.value = ""
        _sttVolume.value = 0f
    }

    fun updateSttVolume(volume: Float) {
        _sttVolume.value = volume
    }

    fun setRecordingState(recording: Boolean) {
        _isRecording.value = recording
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000)
                _timerSeconds.value += 1
            }
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        stopTimer()
        _timerSeconds.value = 0
    }

    fun submitSummary() {
        val passage = _selectedPassage.value ?: return
        val summary = _userSummaryText.value
        if (summary.isBlank()) {
            _evaluationState.value = EvaluationUiState.Error("요약문을 입력해 주세요.")
            return
        }

        stopTimer()
        _evaluationState.value = EvaluationUiState.Loading

        viewModelScope.launch {
            repository.evaluateSummary(passage.id, summary)
                .onSuccess { result ->
                    _evaluationState.value = EvaluationUiState.Success(result.id)
                }
                .onFailure { error ->
                    _evaluationState.value = EvaluationUiState.Error(error.message ?: "평가 생성 실패")
                }
        }
    }

    fun resetEvaluationState() {
        _evaluationState.value = EvaluationUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
