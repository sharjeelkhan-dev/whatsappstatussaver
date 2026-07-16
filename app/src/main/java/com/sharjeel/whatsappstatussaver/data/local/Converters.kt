package com.sharjeel.whatsappstatussaver.data.local

import androidx.room.TypeConverter
import com.sharjeel.whatsappstatussaver.data.models.MediaType
import com.sharjeel.whatsappstatussaver.data.models.PlatformType

class Converters {
    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = enumValueOf(value)

    @TypeConverter
    fun fromPlatformType(value: PlatformType): String = value.name

    @TypeConverter
    fun toPlatformType(value: String): PlatformType = enumValueOf(value)
}

