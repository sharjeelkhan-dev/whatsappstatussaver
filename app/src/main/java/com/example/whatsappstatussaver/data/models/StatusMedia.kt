package com.example.whatsappstatussaver.data.models

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class PlatformType {
    WHATSAPP,
    WHATSAPP_BUSINESS,
    SAVED
}

data class StatusMedia(
    val uri: Uri,
    val name: String,
    val type: MediaType,
    val size: Long,
    val dateModified: Long,
    val platform: PlatformType,
    val isFavorite: Boolean = false,
    val isProfilePhoto: Boolean = false,
    val tags: String = ""
)
