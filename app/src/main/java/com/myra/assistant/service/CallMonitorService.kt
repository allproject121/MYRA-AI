package com.myra.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.myra.assistant.R
import com.myra.assistant.ui.main.MainActivity

class CallMonitorService : Service() {

    companion object {
        private const val TAG = "CallMonitorService"
        const val CHANNEL_ID = "myra_call_monitor_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_CALL_ENDED = "com.myra.CALL_ENDED"
        var isRunning = false
            private set
    }

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var lastState = TelephonyManager.CALL_STATE_IDLE

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException starting foreground with phoneCall type, falling back", e)
            try {
                startForeground(NOTIFICATION_ID, buildNotification())
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to start foreground service", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting foreground service", e)
        }
        registerPhoneStateListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MYRA Call Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors incoming calls for voice assistance"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MYRA Call Assistant Active")
            .setContentText("Monitoring calls for smart announcement")
            .setSmallIcon(R.drawable.ic_myra_notif)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun registerPhoneStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                if (state == lastState) return
                lastState = state

                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        val callerName = resolveCallerName(phoneNumber)
                        val intent = Intent(this@CallMonitorService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("INCOMING_CALL", true)
                            putExtra("CALLER_NAME", callerName)
                            putExtra("PHONE_NUMBER", phoneNumber ?: "")
                        }
                        startActivity(intent)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        val endedIntent = Intent(ACTION_CALL_ENDED)
                        sendBroadcast(endedIntent)
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        // Call answered / in progress
                    }
                }
            }
        }

        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resolveCallerName(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return "Unknown Caller"
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor: Cursor? = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return it.getString(nameIndex)
                    }
                }
            }
            phoneNumber
        } catch (e: Exception) {
            phoneNumber
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            phoneStateListener?.let {
                telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
