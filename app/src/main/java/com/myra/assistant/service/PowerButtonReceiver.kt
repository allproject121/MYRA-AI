package com.myra.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class PowerButtonReceiver : BroadcastReceiver() {

    companion object {
        private var lastScreenToggleTime: Long = 0
        private const val DOUBLE_PRESS_INTERVAL_MS = 600L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScreenToggleTime <= DOUBLE_PRESS_INTERVAL_MS) {
                // Double press detected -> launch overlay
                val serviceIntent = Intent(context, MyraOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                lastScreenToggleTime = 0
            } else {
                lastScreenToggleTime = currentTime
            }
        }
    }
}
