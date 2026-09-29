package com.myra.assistant.service

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Native Core Memory & Context Store for MYRA Assistant.
 * Securely persists key-value facts, locations, and personal notes.
 */
class MyraCoreMemoryStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "myra_core_memory_store"
        private const val MEMORY_STORE_KEY = "user_core_memories_json"
    }

    @Synchronized
    fun saveMemory(key: String, value: String): Boolean {
        val cleanKey = key.trim().lowercase().replace("\\s+".toRegex(), "_")
        val json = getMemoriesJson()
        json.put(cleanKey, value.trim())
        return prefs.edit().putString(MEMORY_STORE_KEY, json.toString()).commit()
    }

    @Synchronized
    fun getMemory(key: String): String? {
        val cleanKey = key.trim().lowercase().replace("\\s+".toRegex(), "_")
        val json = getMemoriesJson()
        if (json.has(cleanKey)) {
            return json.optString(cleanKey)
        }
        // Fuzzy search through stored memory keys
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k.contains(cleanKey) || cleanKey.contains(k)) {
                return json.optString(k)
            }
        }
        return null
    }

    @Synchronized
    fun getAllMemories(): Map<String, String> {
        val json = getMemoriesJson()
        val result = mutableMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            result[k] = json.optString(k)
        }
        return result
    }

    private fun getMemoriesJson(): JSONObject {
        val raw = prefs.getString(MEMORY_STORE_KEY, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
    }
}
