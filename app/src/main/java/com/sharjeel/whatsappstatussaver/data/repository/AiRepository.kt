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
            chatMutex.withLock {
                chatSession = generativeModel.startChat()
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateFromPrompt(prompt: String, bitmap: Bitmap? = null, mediaBytes: ByteArray? = null, mimeType: String? = null): String = withContext(Dispatchers.IO) {
        try {
            val response = if (bitmap != null) {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                generativeModel.generateContent(inputContent)
            } else if (mediaBytes != null && mimeType != null) {
                val inputContent = content {
                    inlineData(mediaBytes, mimeType)
                    text(prompt)
                }
                generativeModel.generateContent(inputContent)
            } else {
                generativeModel.generateContent(prompt)
            }
            response.text ?: "AI could not generate a response."
        } catch (e: Exception) {
            Log.e("AiRepository", "Generation error: ${e.localizedMessage}", e)
            "Error: ${e.localizedMessage}"
        }
    }

    // Specialized features
    suspend fun analyzeStatus(bitmap: Bitmap): String = generateFromPrompt(
        "Analyze this WhatsApp status. Identify scenes, objects, people, colors, and activities. " +
                "Detect emotions (motivational, romantic, funny, sad, etc.). " +
                "Provide a summary in 5 points.", bitmap
    )

    suspend fun extractOCR(bitmap: Bitmap): String = generateFromPrompt(
        "Extract all visible text from this image (OCR). Return ONLY the extracted text.", bitmap
    )

    suspend fun generateCaptionsAndHashtags(bitmap: Bitmap): String = generateFromPrompt(
        "Generate 3 creative WhatsApp captions and 5 trending hashtags for this image.", bitmap
    )

    suspend fun translateText(text: String, targetLanguage: String): String = generateFromPrompt(
        "Translate the following text to $targetLanguage: \n\n$text"
    )

    suspend fun classifyMedia(bitmap: Bitmap): String = generateFromPrompt(
        "AI Automatic Category Classification: Classify this status into exactly ONE category from: " +
                "Funny, Islamic, Nature, Motivational, Sad, Romantic, Travel, Food. " +
                "Also provide 3-5 smart tags. Format: Category: [Name], Tags: [tag1, tag2...]", bitmap
    )

    suspend fun performSmartSearch(query: String, mediaList: List<StatusMedia>): List<String> = withContext(Dispatchers.IO) {
        if (mediaList.isEmpty() || query.isBlank()) return@withContext emptyList()
        try {
            val metadata = mediaList.joinToString("\n") { "${it.name} (Tags: ${it.tags})" }
            val promptText = """
                Based on the following files and their tags, find files that match the intent: "$query".
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