package com.sharjeel.whatsappstatussaver.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sharjeel.whatsappstatussaver.data.local.dao.ReminderDao
import com.sharjeel.whatsappstatussaver.data.local.dao.SavedMediaDao
import com.sharjeel.whatsappstatussaver.data.local.entity.ReminderEntity
import com.sharjeel.whatsappstatussaver.data.local.entity.SavedMediaEntity

@Database(entities = [SavedMediaEntity::class, ReminderEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StatusDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
    abstract fun reminderDao(): ReminderDao
}

