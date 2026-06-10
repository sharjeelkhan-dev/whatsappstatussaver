package com.example.whatsappstatussaver.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.whatsappstatussaver.data.local.dao.ReminderDao
import com.example.whatsappstatussaver.data.local.dao.SavedMediaDao
import com.example.whatsappstatussaver.data.local.entity.ReminderEntity
import com.example.whatsappstatussaver.data.local.entity.SavedMediaEntity

@Database(entities = [SavedMediaEntity::class, ReminderEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class StatusDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
    abstract fun reminderDao(): ReminderDao
}
