package com.example.llamadroid.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZimDao {
    @Query("SELECT * FROM zim_files ORDER BY title ASC")
    fun getAllZims(): Flow<List<ZimEntity>>
    
    @Query("SELECT * FROM zim_files WHERE id = :id LIMIT 1")
    suspend fun getZimById(id: String): ZimEntity?
    
    @Query("SELECT * FROM zim_files WHERE language = :language ORDER BY title ASC")
    fun getZimsByLanguage(language: String): Flow<List<ZimEntity>>
    
    @Query("SELECT * FROM zim_files WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchZims(query: String): Flow<List<ZimEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZim(zim: ZimEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZims(zims: List<ZimEntity>)
    
    @Update
    suspend fun updateZim(zim: ZimEntity)
    
    @Delete
    suspend fun deleteZim(zim: ZimEntity)
    
    @Query("DELETE FROM zim_files WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("UPDATE zim_files SET filename = :newFilename, path = :newPath WHERE id = :id")
    suspend fun renameZim(id: String, newFilename: String, newPath: String)
    
    @Query("SELECT COUNT(*) FROM zim_files")
    suspend fun getZimCount(): Int
}
