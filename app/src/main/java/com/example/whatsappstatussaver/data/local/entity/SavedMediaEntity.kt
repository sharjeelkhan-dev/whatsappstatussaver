package com.example.whatsappstatussaver.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType

@Entity(tableName = "saved_media")
data class SavedMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uriString: String,
    val name: String,
    val type: MediaType,
    val size: Long,
    val dateSaved: Long,
    val sourcePlatform: PlatformType,
    val isFavorite: Boolean = false,
    val isProfilePhoto: Boolean = false,
    val tags: String = "" // Comma-separated list of tags/contacts
)
