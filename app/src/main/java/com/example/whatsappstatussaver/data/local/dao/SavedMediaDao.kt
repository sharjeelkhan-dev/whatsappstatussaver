package com.example.whatsappstatussaver.data.local.dao

import androidx.room.*
import com.example.whatsappstatussaver.data.local.entity.SavedMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMediaDao {
    @Query("SELECT * FROM saved_media ORDER BY dateSaved DESC")
    fun getAllSavedMedia(): Flow<List<SavedMediaEntity>>

    @Query("SELECT * FROM saved_media WHERE isFavorite = 1 ORDER BY dateSaved DESC")
    fun getFavorites(): Flow<List<SavedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: SavedMediaEntity)

    @Delete
    suspend fun deleteMedia(media: SavedMediaEntity)

    @Query("SELECT * FROM saved_media WHERE name = :name LIMIT 1")
    suspend fun getMediaByName(name: String): SavedMediaEntity?
    
    @Query("UPDATE saved_media SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE saved_media SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)

    @Query("SELECT * FROM saved_media WHERE name LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY dateSaved DESC")
    fun searchMedia(query: String): Flow<List<SavedMediaEntity>>
}
