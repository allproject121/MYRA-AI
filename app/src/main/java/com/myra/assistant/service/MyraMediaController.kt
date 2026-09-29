package com.myra.assistant.service

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.view.KeyEvent

/**
 * Native Media Controller for MYRA Assistant.
 * Controls active media playback (Spotify, YouTube, Podcasting apps) via MediaSessionManager and KeyEvent dispatch.
 */
class MyraMediaController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun executeMediaAction(action: String): Pair<Boolean, String> {
        val clean = action.trim().lowercase()

        // 1. Try MediaSessionManager controllers if Notification Listener or permission is active
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                val component = ComponentName(context, MyraNotificationListenerService::class.java)
                val controllers: List<MediaController>? = try {
                    mediaSessionManager?.getActiveSessions(component)
                } catch (e: SecurityException) {
                    null
                }

                if (!controllers.isNullOrEmpty()) {
                    val activeController = controllers.firstOrNull {
                        val state = it.playbackState?.state
                        state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                    } ?: controllers.first()

                    val transport = activeController.transportControls
                    when (clean) {
                        "play" -> {
                            transport.play()
                            return Pair(true, "Resumed playback on ${activeController.packageName}")
                        }
                        "pause" -> {
                            transport.pause()
                            return Pair(true, "Paused playback on ${activeController.packageName}")
                        }
                        "toggle" -> {
                            val isPlaying = activeController.playbackState?.state == PlaybackState.STATE_PLAYING
                            if (isPlaying) transport.pause() else transport.play()
                            return Pair(true, if (isPlaying) "Paused media" else "Resumed media")
                        }
                        "next", "skip" -> {
                            transport.skipToNext()
                            return Pair(true, "Skipped to next track")
                        }
                        "previous", "prev", "back" -> {
                            transport.skipToPrevious()
                            return Pair(true, "Returned to previous track")
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall back to key event dispatch
            }
        }

        // 2. Universal Hardware Media KeyEvent Dispatch Fallback
        val keyCode = when (clean) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        return try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager?.dispatchMediaKeyEvent(downEvent)
            audioManager?.dispatchMediaKeyEvent(upEvent)
            Pair(true, "Media command '$clean' dispatched.")
        } catch (e: Exception) {
            Pair(false, "Could not control media: ${e.message}")
        }
    }
}
