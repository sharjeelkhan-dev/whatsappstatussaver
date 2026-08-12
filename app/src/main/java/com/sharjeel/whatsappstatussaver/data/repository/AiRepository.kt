package com.sharjeel.whatsappstatussaver.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import com.sharjeel.whatsappstatussaver.data.models.StatusMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val generativeModel: GenerativeModel
) {
    private val chatMutex = Mutex()

    // Firebase AI Logic me Chat instance model se initialize hota hai
    private var chatSession: Chat = generativeModel.startChat()

    fun chat(prompt: String): Flow<String> = flow {
        try {
            val response = chatMutex.withLock {
                chatSession.sendMessage(prompt)
            }
            emit(response.text ?: "No response from AI")
        } catch (e: Exception) {
            Log.e("AiRepository", "Chat error: ${e.localizedMessage}", e)
            emit("Error: ${e.localizedMessage ?: "Unknown error occurred"}")

            // Failure par chat session re-initialize karein
            chatMutex.withLock {
                chatSession = generativeModel.startChat()
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun analyzeImage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            // Multimodal content creation using Firebase AI content builder
            val promptContent = content {
                image(bitmap)
                text(
                    "Analyze this image for a WhatsApp status saver app. " +
                            "Identify scenes, objects, people, colors, and activities. " +
                            "Extract any visible text (OCR). " +
                            "Suggest 3 engagement-focused captions and 5 trending hashtags. " +
                            "Format clearly."
                )
            }
            val response = generativeModel.generateContent(promptContent)
            response.text ?: "Could not analyze image"
        } catch (e: Exception) {
            Log.e("AiRepository", "AnalyzeImage error: ${e.localizedMessage}", e)
            "Error: ${e.localizedMessage ?: "Unknown error occurred"}"
        }
    }

    suspend fun performSmartSearch(query: String, mediaList: List<StatusMedia>): List<String> = withContext(Dispatchers.IO) {
        if (mediaList.isEmpty() || query.isBlank()) return@withContext emptyList()

        try {
            val metadata = mediaList.joinToString("\n") { "${it.name} (Tags: ${it.tags})" }
            val promptText = """
                Based on the following files and their tags, find files that match: "$query".
                Files:
                $metadata
                
                Return ONLY the exact names of the matching files, separated by commas. Return "NONE" if no matches.
            """.trimIndent()

            val response = generativeModel.generateContent(promptText)
            val resultText = response.text?.trim() ?: return@withContext emptyList()

            if (resultText.equals("NONE", ignoreCase = true)) {
                emptyList()
            } else {
                resultText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.equals("NONE", ignoreCase = true) }
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "SmartSearch error: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun resetChat() {
        chatMutex.withLock {
            chatSession = generativeModel.startChat()
        }
    }
}