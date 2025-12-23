package com.example.llamadroid.data.rag

import kotlin.math.sqrt

data class DocumentChunk(
    val id: String,
    val text: String,
    val source: String,
    val vector: FloatArray? = null
)

class VectorStore {
    // In a real app this would be backed by SQLite or a native vector DB
    private val chunks = mutableListOf<DocumentChunk>()

    fun addChunks(newChunks: List<DocumentChunk>) {
        chunks.addAll(newChunks)
    }

    fun search(queryVector: FloatArray, topK: Int = 3): List<DocumentChunk> {
        if (chunks.isEmpty()) return emptyList()
        
        return chunks.map { chunk ->
            val score = if (chunk.vector != null) cosineSimilarity(queryVector, chunk.vector) else 0f
            chunk to score
        }.sortedByDescending { it.second }
         .take(topK)
         .map { it.first }
    }
    
    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        return dot / (sqrt(norm1) * sqrt(norm2))
    }
    
    fun clear() {
        chunks.clear()
    }
}
