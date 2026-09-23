package com.sharjeel.whatsappstatussaver.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.whatsappstatussaver.data.billing.BillingManager
import com.sharjeel.whatsappstatussaver.data.local.datastore.AppSettings
import com.sharjeel.whatsappstatussaver.data.repository.CloudBackupRepository
import com.sharjeel.whatsappstatussaver.domain.usecase.ExportMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val billingManager: BillingManager,
    private val cloudBackupRepository: CloudBackupRepository,
    private val exportMediaUseCase: ExportMediaUseCase
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = appSettings.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val customSaveLocation: StateFlow<String?> = appSettings.customSaveLocationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPremium = billingManager.isPremium

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setDarkMode(enabled)
        }
    }

    fun setSaveLocation(uri: Uri?) {
        viewModelScope.launch {
            appSettings.setCustomSaveLocation(uri?.toString())
        }
    }

    fun exportMedia(destinationUri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = exportMediaUseCase.exportToZip(destinationUri)
            onComplete(success)
        }
    }

    fun launchBillingFlow(activity: android.app.Activity) {
        billingManager.launchBillingFlow(activity)
    }

    fun toggleMockPremium() {
        billingManager.toggleMockPremium()
    }
}