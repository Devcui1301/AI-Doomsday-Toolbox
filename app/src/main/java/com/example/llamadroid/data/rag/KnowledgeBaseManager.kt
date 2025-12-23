package com.example.llamadroid.data.rag

import com.example.llamadroid.data.db.ChunkEntity
import com.example.llamadroid.data.db.KnowledgeBaseEntity
import com.example.llamadroid.data.db.RagDao
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class KnowledgeBaseManager(
    private val ragDao: RagDao,
    private val pdfExtractor: PdfExtractor
) {
    
    suspend fun createKnowledgeBase(name: String) {
        ragDao.insertKb(KnowledgeBaseEntity(name = name, createdDate = System.currentTimeMillis(), embeddingModelName = null))
    }

    suspend fun addDocumentToKb(kbId: Long, file: File, embeddingFunction: suspend (String) -> FloatArray) = withContext(Dispatchers.IO) {
        // 1. Extract
        val text = pdfExtractor.extractText(file)
        
        // 2. Chunk
        val chunks = chunkText(text)
        
        // 3. Embed & Link
        val entities = chunks.map { chunkContent ->
            val vector = embeddingFunction(chunkContent)
            ChunkEntity(
                kbId = kbId,
                content = chunkContent,
                sourceFile = file.name,
                embeddingJson = Json.encodeToString(vector.toList())
            )
        }
        
        // 4. Save
        ragDao.insertChunks(entities)
    }
    
    // Simple sliding window chunker
    private fun chunkText(text: String, chunkSize: Int = 500, overlap: Int = 50): List<String> {
        val words = text.split("\\s+".toRegex())
        val chunks = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            val end = min(words.size, i + chunkSize)
            chunks.add(words.subList(i, end).joinToString(" "))
            i += (chunkSize - overlap)
        }
        return chunks
    }
    
    suspend fun performSearch(kbIds: List<Long>, queryVector: FloatArray, topK: Int = 5): List<ChunkEntity> {
        // Naive: fetch all chunks from selected KBs and compute cosine similarity
        // In Prod: Use SQLite-VSS or specialized vector DB
        val candidates = mutableListOf<ChunkEntity>()
        for (id in kbIds) {
            candidates.addAll(ragDao.getChunksForKb(id))
        }
        
        return candidates.map { entity ->
            val vectorList = Json.decodeFromString<List<Float>>(entity.embeddingJson)
            val vector = vectorList.toFloatArray()
            val score = cosineSimilarity(queryVector, vector)
            entity to score
        }.sortedByDescending { it.second }
         .take(topK)
         .map { it.first }
    }
    
    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in v1.indices) {
            val val1 = v1.getOrElse(i) { 0f }
            val val2 = v2.getOrElse(i) { 0f }
            dot += val1 * val2
            norm1 += val1 * val1
            norm2 += val2 * val2
        }
        val denom = kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2)
        return if (denom > 0) dot / denom else 0f
    }
}
