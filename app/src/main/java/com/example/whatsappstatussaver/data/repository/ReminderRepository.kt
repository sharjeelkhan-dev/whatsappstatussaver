package com.example.whatsappstatussaver.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.whatsappstatussaver.data.local.dao.ReminderDao
import com.example.whatsappstatussaver.data.local.entity.ReminderEntity
import com.example.whatsappstatussaver.utils.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
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

    private fun scheduleAlarm(reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
            timeInMillis = reminder.date
            val timeCal = Calendar.getInstance().apply { timeInMillis = reminder.time }
            set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Log.d("ReminderRepository", "Scheduling alarm for: ${sdf.format(calendar.time)} (id: ${reminder.id})")

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            if (reminder.repeatType == "Daily") {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                Log.d("ReminderRepository", "Time is in past, adjusted to tomorrow: ${sdf.format(calendar.time)}")
            } else {
                Log.d("ReminderRepository", "Time is in past and not repeating, NOT scheduling.")
                return
            }
        }

        if (reminder.repeatType == "Daily") {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
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
