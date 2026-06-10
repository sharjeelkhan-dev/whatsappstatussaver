package com.example.whatsappstatussaver.domain.usecase

import com.example.whatsappstatussaver.data.local.dao.SavedMediaDao
import com.example.whatsappstatussaver.data.local.entity.SavedMediaEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageFavoritesUseCase @Inject constructor(
    private val savedMediaDao: SavedMediaDao
) {
    fun getFavorites(): Flow<List<SavedMediaEntity>> = savedMediaDao.getFavorites()
    fun getAllSavedMedia(): Flow<List<SavedMediaEntity>> = savedMediaDao.getAllSavedMedia()
    
    suspend fun toggleFavorite(name: String, isFavorite: Boolean) {
        val entity = savedMediaDao.getMediaByName(name)
        if (entity != null) {
            savedMediaDao.updateFavoriteStatus(entity.id, isFavorite)
        }
    }
}
