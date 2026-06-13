package com.example.whatsappstatussaver.ui.status

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

data class StatusUiState(
    val statuses: List<StatusMedia> = emptyList(),
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val selectedMedia: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val downloadProgress: Float? = null,
    val isAppInstalled: Boolean = true,
    val debugMessage: String = "",
    val currentPlatform: PlatformType = PlatformType.WHATSAPP // Track the active platform
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: StorageRepository,
    private val notifier: DownloadNotifier,
    private val compressVideoUseCase: com.example.whatsappstatussaver.domain.usecase.CompressVideoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    fun checkPermissionAndLoad(platform: PlatformType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentPlatform = platform) }

            val isInstalled = repository.isAppInstalled(platform)
            if (!isInstalled) {
                _uiState.update { it.copy(
                    isAppInstalled = false,
                    hasPermission = false,
                    statuses = emptyList(),
                    isLoading = false,
                    debugMessage = "${platform.name} is not installed on this device."
                )}
                return@launch
            }

            val hasPerm = repository.hasPermission(platform)

            if (hasPerm) {
                val (statuses, debugMsg) = repository.getStatuses(platform)
                _uiState.update { it.copy(
                    isAppInstalled = true,
                    hasPermission = true,
                    statuses = statuses,
                    isLoading = false,
                    debugMessage = debugMsg
                )}
            } else {
                _uiState.update { it.copy(
                    isAppInstalled = true,
                    hasPermission = false,
                    statuses = emptyList(),
                    isLoading = false,
                    debugMessage = "Permission not granted or invalid URI for ${platform.name}."
                )}
            }
        }
    }

    // Call this function to refresh statuses instantly (e.g., Pull-to-refresh or onResume)
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

    suspend fun compressVideo(uri: android.net.Uri): android.net.Uri? {
        return compressVideoUseCase(uri)
    }

    fun saveMedia(statusMedia: StatusMedia) {
        viewModelScope.launch {
            try {
                val success = repository.saveStatus(statusMedia)
                if (success) {
                    notifier.showDownloadComplete(1)
                } else {
                    // This could be shown via a state flow or a one-time event if needed
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun saveSelectedMedia() {
        viewModelScope.launch {
            val state = _uiState.value
            val mediaToSave = state.statuses.filter { state.selectedMedia.contains(it.uri.toString()) }
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