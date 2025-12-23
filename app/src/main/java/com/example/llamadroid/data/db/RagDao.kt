package com.example.llamadroid.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RagDao {
    @Insert
    suspend fun insertKb(kb: KnowledgeBaseEntity): Long

    @Query("SELECT * FROM knowledge_bases")
    fun getAllKbs(): Flow<List<KnowledgeBaseEntity>>

    @Insert
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Query("SELECT * FROM chunks WHERE kbId = :kbId")
    suspend fun getChunksForKb(kbId: Long): List<ChunkEntity>

    @Query("DELETE FROM chunks WHERE kbId = :kbId")
    suspend fun clearChunksForKb(kbId: Long)
    
    @Query("DELETE FROM knowledge_bases WHERE id = :kbId")
    suspend fun deleteKb(kbId: Long)
}
