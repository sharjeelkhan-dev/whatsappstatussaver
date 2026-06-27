package com.example.whatsappstatussaver.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.whatsappstatussaver.MainActivity
import com.example.whatsappstatussaver.R
import com.example.whatsappstatussaver.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "onReceive triggered")
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Status Saver Reminder"
        val reminderId = intent.getIntExtra("REMINDER_ID", 0)

        showNotification(context, title, "Time to check and save some new statuses!", reminderId)

        // Reschedule if daily
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val reminders = repository.getAllReminders().first()
            val currentReminder = reminders.find { it.id == reminderId }
            if (currentReminder != null && currentReminder.repeatType == "Daily" && currentReminder.isAlertEnabled && !currentReminder.isCompleted) {
                // Important: scheduleAlarm already adds 1 day if time is in past, 
                // so we just call it again to set it for tomorrow.
                repository.scheduleAlarm(currentReminder)
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, id: Int) {
        Log.d("ReminderReceiver", "Showing notification: $title, id: $id")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Use standard launcher icon for reliability
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}
