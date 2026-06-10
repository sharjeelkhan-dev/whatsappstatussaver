package com.example.whatsappstatussaver.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val date: Long,
    val time: Long,
    val repeatType: String, // "Daily", "Weekly", "None"
    val priority: String, // "High", "Medium", "Low"
    val isAlertEnabled: Boolean,
    val isCompleted: Boolean = false
)
