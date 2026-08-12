package com.sharjeel.whatsappstatussaver.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.whatsappstatussaver.data.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text, true)
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }

        viewModelScope.launch {
            aiRepository.chat(text).collect { response ->
                val aiMessage = ChatMessage(response, false)
                _uiState.update { it.copy(messages = it.messages + aiMessage, isLoading = false) }
            }
        }
    }
}
