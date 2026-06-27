package com.example.whatsappstatussaver.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.whatsappstatussaver.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Rescheduling alarms on boot...")
            
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val reminders = reminderRepository.getAllReminders().first()
                reminders.forEach { reminder ->
                    if (!reminder.isCompleted && reminder.isAlertEnabled) {
                        reminderRepository.scheduleAlarm(reminder)
                    }
                }
            }
        }
    }
}
