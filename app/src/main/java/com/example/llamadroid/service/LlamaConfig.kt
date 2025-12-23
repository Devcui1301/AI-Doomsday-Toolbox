package com.example.llamadroid.service

data class LlamaConfig(
    val modelPath: String,
    val isEmbedding: Boolean = false,
    val contextSize: Int = 8192,
    val threads: Int = 4,
    val port: Int = 8080,
    val temperature: Float = 0.8f,
    val host: String = "0.0.0.0",
    val mmprojPath: String? = null, // Vision model projector path
    // KV Cache quantization settings
    val kvCacheEnabled: Boolean = false,
    val kvCacheTypeK: String = "f16",  // f16, q8_0, q4_0
    val kvCacheTypeV: String = "f16",
    val kvCacheReuse: Int = 0  // 0 = disabled, >0 = number of tokens to reuse
)

sealed class ServerState {
    object Stopped : ServerState()
    object Starting : ServerState()
    data class Loading(val progress: Float, val status: String) : ServerState() // Model loading progress
    data class Running(val port: Int) : ServerState()
    data class Error(val message: String) : ServerState()
}
