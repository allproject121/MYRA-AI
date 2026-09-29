package com.myra.assistant.social

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SharedMediaItem(
    val uriString: String,
    val mimeType: String,
    val timestamp: Long,
    val localFilePath: String? = null
)

class MediaResolver(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("myra_social_media_inbox", Context.MODE_PRIVATE)

    companion object {
        const val MAX_INBOX_SIZE = 5
        const val MAX_AGE_MS = 30 * 60 * 1000L // 30 minutes
    }

    @Synchronized
    fun addMediaToInbox(uriString: String, mimeType: String, localPath: String? = null) {
        if (!mimeType.startsWith("image/") && !mimeType.startsWith("video/")) {
            return
        }
        val items = getInboxItems().toMutableList()
        val newItem = SharedMediaItem(
            uriString = uriString,
            mimeType = mimeType,
            timestamp = System.currentTimeMillis(),
            localFilePath = localPath
        )
        items.add(0, newItem)

        // Keep bounded size and clean stale files
        val pruned = items.take(MAX_INBOX_SIZE).filter {
            if (it.localFilePath != null) {
                File(it.localFilePath).exists()
            } else true
        }

        saveInboxItems(pruned)
    }

    @Synchronized
    fun resolveMedia(requestedUri: String?): SharedMediaItem? {
        val now = System.currentTimeMillis()
        val items = getInboxItems().filter { (now - it.timestamp) <= MAX_AGE_MS }

        if (requestedUri.isNullOrBlank() || requestedUri.equals("latest", ignoreCase = true)) {
            // Return most recent valid media item
            return items.firstOrNull()
        }

        // Must be a valid content:// URI from the inbox
        if (!requestedUri.startsWith("content://")) {
            return null
        }

        return items.firstOrNull { it.uriString == requestedUri }
    }

    private fun getInboxItems(): List<SharedMediaItem> {
        val raw = prefs.getString("inbox_json", "[]") ?: "[]"
        val list = mutableListOf<SharedMediaItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SharedMediaItem(
                        uriString = obj.getString("uri"),
                        mimeType = obj.getString("mime"),
                        timestamp = obj.getLong("time"),
                        localFilePath = obj.optString("path", null)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveInboxItems(items: List<SharedMediaItem>) {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject().apply {
                put("uri", it.uriString)
                put("mime", it.mimeType)
                put("time", it.timestamp)
                if (it.localFilePath != null) put("path", it.localFilePath)
            }
            arr.put(obj)
        }
        prefs.edit().putString("inbox_json", arr.toString()).apply()
    }
}
