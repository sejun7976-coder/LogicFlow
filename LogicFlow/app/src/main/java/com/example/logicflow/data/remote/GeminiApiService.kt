package com.example.logicflow.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>,
    val role: String? = null
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val responseMimeType: String? = null
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: ContentResponse?
)

data class ContentResponse(
    val parts: List<PartResponse>?
)

data class PartResponse(
    val text: String?
)
// Result JSON structure for easy parsing
data class GeminiEvaluationResult(
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
    val passageType: String? = null
)
