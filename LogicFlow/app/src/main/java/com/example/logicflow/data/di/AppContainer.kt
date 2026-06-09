package com.example.logicflow.data.di

import android.content.Context
import com.example.logicflow.data.local.LogicFlowDatabase
import com.example.logicflow.data.remote.GeminiApiService
import com.example.logicflow.data.repository.LogicFlowRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {
    // Local Room Database
    val database: LogicFlowDatabase by lazy {
        LogicFlowDatabase.getDatabase(context)
    }

    // Shared Preferences for API Key and Settings
    private val sharedPrefs by lazy {
        context.getSharedPreferences("logicflow_prefs", Context.MODE_PRIVATE)
    }

    // Save and retrieve Gemini API Key
    fun getApiKey(): String {
        return sharedPrefs.getString("gemini_api_key", "") ?: ""
    }

    fun saveApiKey(key: String) {
        sharedPrefs.edit().putString("gemini_api_key", key).apply()
    }

    // Save and retrieve AI Hub API Key
    fun getAiHubApiKey(): String {
        return sharedPrefs.getString("aihub_api_key", "") ?: ""
    }

    fun saveAiHubApiKey(key: String) {
        sharedPrefs.edit().putString("aihub_api_key", key).apply()
    }

    // Save and retrieve Gemini Model name
    fun getModelName(): String {
        return sharedPrefs.getString("gemini_model_name", "gemini-2.5-flash") ?: "gemini-2.5-flash"
    }

    fun saveModelName(modelName: String) {
        sharedPrefs.edit().putString("gemini_model_name", modelName).apply()
    }

    // Notification Settings
    fun isLearningNotificationEnabled(): Boolean {
        return sharedPrefs.getBoolean("learning_notification_enabled", false)
    }

    fun saveLearningNotificationEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("learning_notification_enabled", enabled).apply()
    }

    fun getLearningNotificationHour(): Int {
        return sharedPrefs.getInt("learning_notification_hour", 22) // Default 22 (10 PM)
    }

    fun saveLearningNotificationHour(hour: Int) {
        sharedPrefs.edit().putInt("learning_notification_hour", hour).apply()
    }

    // OkHttpClient with interceptors
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // Retrofit service for Gemini API
    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    // Repository
    val repository: LogicFlowRepository by lazy {
        LogicFlowRepository(
            dao = database.logicFlowDao(),
            apiService = apiService,
            getApiKey = { getApiKey() },
            getModelName = { getModelName() },
            getAiHubApiKey = { getAiHubApiKey() },
            isNotificationEnabled = { isLearningNotificationEnabled() },
            saveNotificationEnabled = { saveLearningNotificationEnabled(it) },
            getNotificationHour = { getLearningNotificationHour() },
            saveNotificationHour = { saveLearningNotificationHour(it) }
        ).apply {
            setSaveCallbacks(
                saveApiKey = { saveApiKey(it) },
                saveModelName = { saveModelName(it) },
                saveAiHubApiKey = { saveAiHubApiKey(it) }
            )
        }
    }
}
