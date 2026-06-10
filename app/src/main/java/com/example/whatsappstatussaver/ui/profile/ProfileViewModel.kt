package com.example.whatsappstatussaver.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia
import com.example.whatsappstatussaver.data.repository.StorageRepository
import com.example.whatsappstatussaver.utils.DownloadNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val photos: List<StatusMedia> = emptyList(),
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val selectedMedia: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val downloadProgress: Float? = null,
    val isAppInstalled: Boolean = true,
    val debugMessage: String = "",
    val currentPlatform: PlatformType = PlatformType.WHATSAPP
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: StorageRepository,
    private val notifier: DownloadNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun checkPermissionAndLoad(platform: PlatformType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentPlatform = platform) }

            val isInstalled = repository.isAppInstalled(platform)
            if (!isInstalled) {
                _uiState.update { it.copy(
                    isAppInstalled = false,
                    hasPermission = false,
                    photos = emptyList(),
                    isLoading = false,
                    debugMessage = "${platform.name} is not installed on this device."
                )}
                return@launch
            }

            val hasPerm = repository.hasPermission(platform, true)

            if (hasPerm) {
                val (photos, debugMsg) = repository.getProfilePhotos(platform)
                _uiState.update { it.copy(
                    isAppInstalled = true,
                    hasPermission = true,
                    photos = photos,
                    isLoading = false,
                    debugMessage = debugMsg)}
            } else {
                _uiState.update { it.copy(
                    isAppInstalled = true,
                    hasPermission = false,
                    photos = emptyList(),
                    isLoading = false,
                    debugMessage = "Permission not granted or invalid URI for ${platform.name}."
                )}
            }
        }
    }

    fun refresh() {
        checkPermissionAndLoad(_uiState.value.currentPlatform)
    }

    fun toggleSelection(mediaUri: String) {
        _uiState.update { state ->
            val newSelection = state.selectedMedia.toMutableSet()
            if (newSelection.contains(mediaUri)) {
                newSelection.remove(mediaUri)
            } else {
                newSelection.add(mediaUri)
            }
            state.copy(
                selectedMedia = newSelection,
                isMultiSelectMode = newSelection.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedMedia = emptySet(), isMultiSelectMode = false) }
    }

    fun saveMedia(statusMedia: StatusMedia) {
        viewModelScope.launch {
            val success = repository.saveStatus(statusMedia)
            if (success) {
                notifier.showDownloadComplete(1)
            }
        }
    }

    fun saveSelectedMedia() {
        viewModelScope.launch {
            val state = _uiState.value
            val mediaToSave = state.photos.filter { state.selectedMedia.contains(it.uri.toString()) }
            val total = mediaToSave.size
            if (total == 0) return@launch

            _uiState.update { it.copy(downloadProgress = 0f) }

            var completed = 0
            mediaToSave.forEach { media ->
                repository.saveStatus(media)
                completed++
                _uiState.update { it.copy(downloadProgress = completed.toFloat() / total) }
            }

            notifier.showDownloadComplete(completed)
            clearSelection()
            _uiState.update { it.copy(downloadProgress = null) }
        }
    }
}
