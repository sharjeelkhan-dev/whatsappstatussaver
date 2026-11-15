package com.sharjeel.whatsappstatussaver.utils

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class LoggedNotification(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val packageName: String,
    val isDeleted: Boolean = false
)

class WhatsAppNotificationListener : NotificationListenerService() {

    companion object {
        val notificationLogs = mutableListOf<LoggedNotification>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b") {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: "WhatsApp"
            val text = extras.getCharSequence("android.text")?.toString() ?: ""

            if (text.isNotEmpty() && !text.contains("Checking for new messages", ignoreCase = true)) {
                val log = LoggedNotification(
                    id = "${sbn.id}_${sbn.postTime}",
                    sender = title,
                    text = text,
                    timestamp = sbn.postTime,
                    packageName = pkg
                )
                synchronized(notificationLogs) {
                    notificationLogs.removeAll { it.id == log.id }
                    notificationLogs.add(0, log)
                    if (notificationLogs.size > 200) {
                        notificationLogs.removeAt(notificationLogs.size - 1)
                    }
                }
                Log.d("WhatsAppListener", "Captured message from $title: $text")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val pkg = sbn.packageName
        if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b") {
            Log.d("WhatsAppListener", "Notification removed: ${sbn.id}")
        }
    }
}