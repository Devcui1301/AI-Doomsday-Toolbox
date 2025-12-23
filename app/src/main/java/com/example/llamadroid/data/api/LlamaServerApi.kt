package com.example.llamadroid.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface LlamaServerApi {
    @POST("/completion")
    suspend fun completion(@Body request: CompletionRequest): CompletionResponse
    
    @POST("/embedding")
    suspend fun embedding(@Body request: EmbeddingRequest): EmbeddingResponse
}

data class CompletionRequest(
    val prompt: String,
    val n_predict: Int = 128,
    val temperature: Float = 0.8f,
    val stop: List<String> = emptyList()
)

data class CompletionResponse(
    val content: String
)

data class EmbeddingRequest(
    val content: String
)

data class EmbeddingResponse(
    val embedding: List<Float>
)
