package com.sharjeel.whatsappstatussaver.ui.vault

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.whatsappstatussaver.data.local.dao.SavedMediaDao
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import com.sharjeel.whatsappstatussaver.utils.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val isLoading: Boolean = false,
    val vaultMedia: List<StatusMedia> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val dao: SavedMediaDao,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        observeVaultMedia()
    }

    private fun observeVaultMedia() {
        viewModelScope.launch {
            dao.getVaultMedia().collectLatest { entities ->
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
                _uiState.update { it.copy(vaultMedia = mediaList) }
            }
        }
    }

    fun authenticate(activity: FragmentActivity) {
        if (!biometricAuthManager.canAuthenticate()) {
            // Devices without biometric hardware automatically unlock or set error
            _uiState.update { it.copy(isUnlocked = true) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        biometricAuthManager.promptBiometricAuth(
            activity = activity,
            title = "Unlock Private Vault",
            subtitle = "Verify fingerprint, Face ID, or PIN to access hidden media",
            onSuccess = {
                _uiState.update { it.copy(isUnlocked = true, isLoading = false) }
            },
            onError = { error ->
                _uiState.update { it.copy(isUnlocked = false, isLoading = false, errorMessage = error) }
            }
        )
    }

    fun lockVault() {
        _uiState.update { it.copy(isUnlocked = false) }
    }

    fun moveToVault(media: StatusMedia) {
        viewModelScope.launch {
            val entity = dao.getMediaByName(media.name)
            if (entity != null) {
                dao.updateVaultStatus(entity.id, true)
            }
        }
    }

    fun removeFromVault(media: StatusMedia) {
        viewModelScope.launch {
            val entity = dao.getMediaByName(media.name)
            if (entity != null) {
                dao.updateVaultStatus(entity.id, false)
            }
        }
    }
}