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

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "onReceive triggered with action: ${intent.action}")
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Status Saver Reminder"
        val reminderId = intent.getIntExtra("REMINDER_ID", 0)

        showNotification(context, title, "Time to check and save some new statuses!", reminderId)
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
            .setSmallIcon(R.drawable.ic_splash_logo) // Use splash logo or launcher icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}
