package com.sharjeel.whatsappstatussaver.ui.viewer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.whatsappstatussaver.data.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MagicUiState {
    data object Idle : MagicUiState
    data class Loading(val message: String) : MagicUiState
    data class Success(val result: String, val type: MagicType) : MagicUiState
    data class Error(val message: String) : MagicUiState
}

enum class MagicType {
    ANALYSIS, OCR, CAPTION, TRANSLATE_URDU, SHAYARI, OBJECT_DETECTION, MOOD, RECOMMENDATION, SUMMARY
}

@HiltViewModel
class MagicViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MagicUiState>(MagicUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private fun performMagic(
        bitmap: Bitmap?,
        prompt: String,
        type: MagicType,
        loadingMessage: String,
        videoBytes: ByteArray? = null,
        mimeType: String? = null
    ) {
        _uiState.value = MagicUiState.Loading(loadingMessage)
        viewModelScope.launch {
            try {
                val result = if (videoBytes != null && mimeType != null) {
                    aiRepository.generateFromPrompt(prompt, null, videoBytes, mimeType)
                } else {
                    aiRepository.generateFromPrompt(prompt, bitmap)
                }
                _uiState.value = MagicUiState.Success(result, type)
            } catch (e: Exception) {
                _uiState.value = MagicUiState.Error(e.localizedMessage ?: "AI Magic failed")
            }
        }
    }

    // Generic Dispatcher for both Image and Video
    fun triggerMagic(type: MagicType, bitmap: Bitmap? = null, videoBytes: ByteArray? = null, mimeType: String? = null) {
        val isVideo = videoBytes != null
        val target = if (isVideo) "video" else "image"
        
        val prompt = when (type) {
            MagicType.ANALYSIS -> "Perform a deep visual analysis of this $target. Describe scenes, main subject, and activities."
            MagicType.OCR -> "STRICT: Extract all visible text from this $target word-for-word. Only output the text."
            MagicType.CAPTION -> "Generate 3 catchy WhatsApp status captions and 5 hashtags for this $target."
            MagicType.SHAYARI -> "Generate a beautiful 2-line Urdu Shayari (in Urdu and Roman Urdu) matching the mood of this $target."
            MagicType.OBJECT_DETECTION -> "Identify all objects, items, and presence of people/faces in this $target."
            MagicType.TRANSLATE_URDU -> "Extract text or context from this $target and translate/explain it in Urdu (اردو)."
            MagicType.MOOD -> "Detect the primary emotion and category (Funny, Islamic, Nature, etc.) for this $target."
            MagicType.RECOMMENDATION -> "Provide 3 similar content ideas or music suggestions for this $target."
            MagicType.SUMMARY -> "Provide a concise summary of this $target in bullet points."
        }

        val loadingMsg = when (type) {
            MagicType.ANALYSIS -> "Analyzing $target..."
            MagicType.OCR -> "Extracting text..."
            MagicType.CAPTION -> "Generating captions..."
            MagicType.SHAYARI -> "Writing Shayari..."
            else -> "AI is working..."
        }

        performMagic(bitmap, prompt, type, loadingMsg, videoBytes, mimeType)
    }

    fun resetState() {
        _uiState.value = MagicUiState.Idle
    }
}