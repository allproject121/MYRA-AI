package com.myra.assistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Native Notification Engine for MYRA Assistant.
 * Captures notifications from WhatsApp, Instagram, Telegram, and SMS with RemoteInput auto-reply support.
 */
class MyraNotificationListenerService : NotificationListenerService() {

    data class NotificationItem(
        val key: String,
        val packageName: String,
        val title: String,
        val text: String,
        val timestamp: Long,
        val isMessagingApp: Boolean,
        val remoteInput: RemoteInput?,
        val replyAction: Notification.Action?
    )

    companion object {
        private const val TAG = "MyraNotificationEngine"
        private val recentNotifications = mutableListOf<NotificationItem>()
        var isConnected: Boolean = false
            private set

        @Synchronized
        fun getLatestNotifications(appFilter: String? = null, limit: Int = 5): List<NotificationItem> {
            val filter = appFilter?.trim()?.lowercase()
            return recentNotifications.filter {
                if (filter.isNullOrEmpty() || filter == "all") true
                else it.packageName.lowercase().contains(filter)
            }.takeLast(limit).reversed()
        }

        @Synchronized
        fun clearNotifications() {
            recentNotifications.clear()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.i(TAG, "MYRA Notification Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.i(TAG, "MYRA Notification Listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        // Skip our own notifications
        if (pkg == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val isMessaging = isMessagingApp(pkg)
        val (remoteInput, action) = extractRemoteReplyAction(sbn.notification)

        val item = NotificationItem(
            key = sbn.key,
            packageName = pkg,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            isMessagingApp = isMessaging,
            remoteInput = remoteInput,
            replyAction = action
        )

        synchronized(recentNotifications) {
            recentNotifications.add(item)
            if (recentNotifications.size > 50) {
                recentNotifications.removeAt(0)
            }
        }
    }

    private fun isMessagingApp(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p.contains("whatsapp") || p.contains("instagram") ||
                p.contains("telegram") || p.contains("messaging") ||
                p.contains("mms") || p.contains("signal")
    }

    private fun extractRemoteReplyAction(notification: Notification): Pair<RemoteInput?, Notification.Action?> {
        val actions = notification.actions ?: return Pair(null, null)
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (ri in remoteInputs) {
                if (ri.allowFreeFormInput) {
                    return Pair(ri, action)
                }
            }
        }
        return Pair(null, null)
    }

    /**
     * Executes an automated inline response via Android RemoteInput if supported by the notification.
     */
    fun sendRemoteReply(notificationKey: String, replyText: String): Boolean {
        val item = synchronized(recentNotifications) {
            recentNotifications.find { it.key == notificationKey }
        } ?: return false

        val ri = item.remoteInput ?: return false
        val action = item.replyAction ?: return false

        val intent = Intent()
        val bundle = Bundle()
        bundle.putCharSequence(ri.resultKey, replyText)
        RemoteInput.addResultsToIntent(arrayOf(ri), intent, bundle)

        return try {
            action.actionIntent.send(this, 0, intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send RemoteInput reply", e)
            false
        }
    }
}
