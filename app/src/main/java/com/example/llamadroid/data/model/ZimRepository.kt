package com.example.llamadroid.data.model

import android.content.Context
import com.example.llamadroid.data.db.ZimDao
import com.example.llamadroid.data.db.ZimEntity
import com.example.llamadroid.util.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for managing ZIM files.
 * Handles local database operations and file system interactions.
 */
class ZimRepository(
    private val context: Context,
    private val zimDao: ZimDao
) {
    /**
     * Get all installed ZIM files
     */
    fun getInstalledZims(): Flow<List<ZimEntity>> = zimDao.getAllZims()
    
    /**
     * Search installed ZIMs by title or description
     */
    fun searchZims(query: String): Flow<List<ZimEntity>> = zimDao.searchZims(query)
    
    /**
     * Get ZIMs filtered by language
     */
    fun getZimsByLanguage(language: String): Flow<List<ZimEntity>> = zimDao.getZimsByLanguage(language)
    
    /**
     * Import an existing ZIM file into the library (no copy, just register)
     */
    suspend fun importZim(file: File, metadata: ZimMetadata? = null): Result<ZimEntity> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist: ${file.path}"))
            }
            
            if (!file.name.endsWith(".zim")) {
                return@withContext Result.failure(IllegalArgumentException("Not a ZIM file: ${file.name}"))
            }
            
            // Generate UUID if not provided (use file hash as fallback)
            val id = metadata?.id ?: generateZimId(file)
            
            val zim = ZimEntity(
                id = id,
                filename = file.name,
                path = file.absolutePath,
                title = metadata?.title ?: file.nameWithoutExtension,
                description = metadata?.description ?: "",
                language = metadata?.language ?: "en",
                sizeBytes = file.length(),
                articleCount = metadata?.articleCount ?: 0,
                mediaCount = metadata?.mediaCount ?: 0,
                date = metadata?.date ?: "",
                creator = metadata?.creator ?: "",
                publisher = metadata?.publisher ?: "",
                favicon = metadata?.favicon,
                tags = metadata?.tags ?: "",
                downloadUrl = metadata?.downloadUrl,
                catalogEntryId = metadata?.catalogEntryId
            )
            
            zimDao.insertZim(zim)
            DebugLog.log("[ZIM] Imported: ${file.name}")
            Result.success(zim)
        } catch (e: Exception) {
            DebugLog.log("[ZIM] Import error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Remove ZIM from library (file stays on disk)
     */
    suspend fun removeZim(zim: ZimEntity) = withContext(Dispatchers.IO) {
        zimDao.deleteZim(zim)
        DebugLog.log("[ZIM] Removed from library: ${zim.filename}")
    }
    
    /**
     * Delete ZIM file and remove from library
     */
    suspend fun deleteZim(zim: ZimEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(zim.path)
            if (file.exists()) {
                if (!file.delete()) {
                    return@withContext Result.failure(Exception("Failed to delete file"))
                }
            }
            zimDao.deleteZim(zim)
            DebugLog.log("[ZIM] Deleted: ${zim.filename}")
            Result.success(Unit)
        } catch (e: Exception) {
            DebugLog.log("[ZIM] Delete error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Rename a ZIM file
     */
    suspend fun renameZim(zim: ZimEntity, newName: String): Result<ZimEntity> = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(zim.path)
            val extension = oldFile.extension
            val fullNewName = if (newName.endsWith(".$extension")) newName else "$newName.$extension"
            val newFile = File(oldFile.parent, fullNewName)
            
            if (oldFile.renameTo(newFile)) {
                zimDao.renameZim(zim.id, fullNewName, newFile.absolutePath)
                val updated = zim.copy(filename = fullNewName, path = newFile.absolutePath)
                DebugLog.log("[ZIM] Renamed: ${zim.filename} -> $fullNewName")
                Result.success(updated)
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            DebugLog.log("[ZIM] Rename error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Generate a unique ID for a ZIM file (fallback when metadata unavailable)
     */
    private fun generateZimId(file: File): String {
        return "${file.name}-${file.length()}-${file.lastModified()}"
    }
}

/**
 * Metadata extracted from ZIM file or Kiwix catalog
 */
data class ZimMetadata(
    val id: String,
    val title: String,
    val description: String = "",
    val language: String = "en",
    val articleCount: Long = 0,
    val mediaCount: Long = 0,
    val date: String = "",
    val creator: String = "",
    val publisher: String = "",
    val favicon: String? = null,
    val tags: String = "",
    val downloadUrl: String? = null,
    val catalogEntryId: String? = null
)
