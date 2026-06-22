package com.example.whatsappstatussaver.ui.saved

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.data.repository.StorageRepository
import com.example.whatsappstatussaver.domain.usecase.ManageFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedFilesUiState(
    val isLoading: Boolean = true,
    val media: List<StatusMedia> = emptyList(),
    val totalStorageUsed: Long = 0L,
    val errorMessage: String? = null,
    val selectedMedia: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val searchQuery: String = "")

@HiltViewModel
class SavedFilesViewModel @Inject constructor(
    private val repository: StorageRepository,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    private val dao: com.example.whatsappstatussaver.data.local.dao.SavedMediaDao,
    private val compressVideoUseCase: com.example.whatsappstatussaver.domain.usecase.CompressVideoUseCase
) : ViewModel() {

    private val _selectionState = MutableStateFlow(Pair(emptySet<String>(), false))

    private val _searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SavedFilesUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) {
                manageFavoritesUseCase.getAllSavedMedia()
            } else {
                dao.searchMedia(query)
            }
        },
        _selectionState,
        _searchQuery
    ) { entities, selection, query ->
        val mediaList = entities.map { entity ->
            StatusMedia(
                uri = Uri.parse(entity.uriString),
                name = entity.name,
                type = entity.type,
                size = entity.size,
                dateModified = entity.dateSaved,
                platform = entity.sourcePlatform,
                isFavorite = entity.isFavorite,
                tags = entity.tags
            )
        }
        val totalStorage = mediaList.sumOf { it.size }
        SavedFilesUiState(
            isLoading = false,
            media = mediaList,
            totalStorageUsed = totalStorage,
            selectedMedia = selection.first,
            isMultiSelectMode = selection.second,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavedFilesUiState()
    )
    fun toggleSelection(mediaUri: String) {
        val currentSelection = _selectionState.value.first.toMutableSet()
        if (currentSelection.contains(mediaUri)) {
            currentSelection.remove(mediaUri)
        } else {
            currentSelection.add(mediaUri)
        }
        _selectionState.value = Pair(currentSelection, currentSelection.isNotEmpty())
    }
    fun clearSelection() {
        _selectionState.value = Pair(emptySet(), false)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteFile(media: StatusMedia) {
        viewModelScope.launch {
            repository.deleteSavedFile(media)
        }
    }
    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val state = uiState.value
            val mediaToDelete = state.media.filter { state.selectedMedia.contains(it.uri.toString()) }
            mediaToDelete.forEach {
                repository.deleteSavedFile(it)
            }
            clearSelection()
        }
    }

    fun toggleFavorite(media: StatusMedia) {
        viewModelScope.launch {
            manageFavoritesUseCase.toggleFavorite(media.name, !media.isFavorite)
        }
    }

    fun updateTags(media: StatusMedia, tags: String) {
        viewModelScope.launch {
            val entity = dao.getMediaByName(media.name)
            if (entity != null) {
                dao.updateTags(entity.id, tags)
            }
        }
    }

    suspend fun compressVideo(uri: android.net.Uri): android.net.Uri? {
        return compressVideoUseCase(uri)
    }
}
