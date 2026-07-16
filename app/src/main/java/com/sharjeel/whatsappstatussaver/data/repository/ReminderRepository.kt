package com.sharjeel.whatsappstatussaver.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sharjeel.whatsappstatussaver.data.local.dao.ReminderDao
import com.sharjeel.whatsappstatussaver.data.local.entity.ReminderEntity
import com.sharjeel.whatsappstatussaver.utils.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    @ApplicationContext private val context: Context
) {
    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun addReminder(reminder: ReminderEntity) {
        val id = reminderDao.insertReminder(reminder)
        Log.d("ReminderRepository", "Reminder added to DB with id: $id")
        if (reminder.isAlertEnabled) {
            scheduleAlarm(reminder.copy(id = id.toInt()))
        }
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        cancelAlarm(reminder)
        reminderDao.deleteReminder(reminder)
    }

    suspend fun updateReminderCompletion(reminder: ReminderEntity, isCompleted: Boolean) {
        reminderDao.updateCompletionStatus(reminder.id, isCompleted)
        if (isCompleted) {
            cancelAlarm(reminder)
        } else if (reminder.isAlertEnabled) {
            scheduleAlarm(reminder.copy(isCompleted = false))
        }
    }

    fun scheduleAlarm(reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check if we can schedule exact alarms on Android 12+
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            val dateCal = Calendar.getInstance().apply { timeInMillis = reminder.date }
            val timeCal = Calendar.getInstance().apply { timeInMillis = reminder.time }
            
            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        Log.d("ReminderRepository", "Scheduling alarm for: ${calendar.time} (ID: ${reminder.id}, Exact: $canScheduleExact)")

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("ReminderRepository", "Failed to schedule alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
  private fun cancelAlarm(reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}

