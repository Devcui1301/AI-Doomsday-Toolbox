package com.example.llamadroid.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
    indices = [Index(value = ["kbId"])]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kbId: Long, // ID of the Knowledge Base this belongs to
    val content: String,
    val sourceFile: String,
    val embeddingJson: String // Serialized float array or use TypeConverter
)

@Entity(tableName = "knowledge_bases")
data class KnowledgeBaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdDate: Long,
    val embeddingModelName: String?
)
