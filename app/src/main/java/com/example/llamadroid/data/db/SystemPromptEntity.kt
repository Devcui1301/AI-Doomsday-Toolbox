package com.example.llamadroid.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Entity for saved system prompts for various AI features
 */
@Entity(tableName = "system_prompts")
data class SystemPromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * DAO for system prompt CRUD operations
 */
@Dao
interface SystemPromptDao {
    @Query("SELECT * FROM system_prompts ORDER BY createdAt DESC")
    fun getAllPrompts(): Flow<List<SystemPromptEntity>>
    
    @Query("SELECT * FROM system_prompts WHERE id = :id")
    suspend fun getPromptById(id: Long): SystemPromptEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: SystemPromptEntity): Long
    
    @Update
    suspend fun updatePrompt(prompt: SystemPromptEntity)
    
    @Delete
    suspend fun deletePrompt(prompt: SystemPromptEntity)
    
    @Query("DELETE FROM system_prompts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
