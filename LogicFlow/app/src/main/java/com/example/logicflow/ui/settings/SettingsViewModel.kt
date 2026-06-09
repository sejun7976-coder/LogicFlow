package com.example.logicflow.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logicflow.data.repository.LogicFlowRepository
import com.example.logicflow.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// API 테스트 상태
sealed class ApiTestState {
    object Idle : ApiTestState()
    object Loading : ApiTestState()
    data class Success(val message: String) : ApiTestState()
    data class Error(val message: String) : ApiTestState()
}

class SettingsViewModel(
    private val repository: LogicFlowRepository,
    private val context: Context
) : ViewModel() {
    private val _apiKey = MutableStateFlow(repository.getApiKeyString())
    val apiKey: StateFlow<String> = _apiKey

    private val _aiHubApiKey = MutableStateFlow(repository.getAiHubApiKeyString())
    val aiHubApiKey: StateFlow<String> = _aiHubApiKey

    private val _modelName = MutableStateFlow(repository.getModelNameString())
    val modelName: StateFlow<String> = _modelName

    private val _notificationEnabled = MutableStateFlow(repository.isLearningNotificationEnabled())
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled

    private val _notificationHour = MutableStateFlow(repository.getLearningNotificationHour())
    val notificationHour: StateFlow<Int> = _notificationHour

    private val _apiTestState = MutableStateFlow<ApiTestState>(ApiTestState.Idle)
    val apiTestState: StateFlow<ApiTestState> = _apiTestState

    fun saveSettings(key: String, aiHubKey: String, model: String, notifEnabled: Boolean, notifHour: Int) {
        _apiKey.value = key
        _aiHubApiKey.value = aiHubKey
        _modelName.value = model
        _notificationEnabled.value = notifEnabled
        _notificationHour.value = notifHour

        repository.saveApiKeyString(key)
        repository.saveAiHubApiKeyString(aiHubKey)
        repository.saveModelNameString(model)
        repository.saveLearningNotificationEnabled(notifEnabled)
        repository.saveLearningNotificationHour(notifHour)

        if (notifEnabled) {
            NotificationHelper.scheduleDailyAlarm(context.applicationContext, notifHour)
        } else {
            NotificationHelper.cancelDailyAlarm(context.applicationContext)
        }
    }

    fun testApiConnection(apiKey: String, modelName: String) {
        viewModelScope.launch {
            _apiTestState.value = ApiTestState.Loading
            // 테스트 전 임시 저장
            repository.saveApiKeyString(apiKey)
            repository.saveModelNameString(modelName)
            val result = repository.testApiConnection()
            _apiTestState.value = if (result.isSuccess) {
                ApiTestState.Success(result.getOrDefault("연결 성공"))
            } else {
                ApiTestState.Error(result.exceptionOrNull()?.message ?: "알 수 없는 오류")
            }
        }
    }

    fun resetTestState() {
        _apiTestState.value = ApiTestState.Idle
    }
}
