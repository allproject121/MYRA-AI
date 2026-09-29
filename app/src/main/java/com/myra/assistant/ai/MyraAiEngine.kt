package com.myra.assistant.ai

import com.myra.assistant.model.AppCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MyraAiEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun processUserMessage(
        message: String,
        personality: String,
        userName: String,
        primeName: String,
        primePhone: String,
        apiKey: String
    ): Pair<String, AppCommand?> = withContext(Dispatchers.IO) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return@withContext Pair("Sun rahi hoon, bolye...", null)
        }

        // 1. Direct CommandParser check first
        val parsedCmd = CommandParser.parse(trimmed)

        // 2. If Gemini API key is configured, query Gemini 2.5 Flash
        if (apiKey.isNotBlank()) {
            try {
                val geminiResult = queryGeminiRest(trimmed, personality, userName, primeName, primePhone, apiKey)
                if (geminiResult != null) {
                    val finalCmd = geminiResult.second ?: parsedCmd
                    return@withContext Pair(geminiResult.first, finalCmd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Intelligent Local Personality Response fallback (instant & offline-capable)
        val localResponse = getLocalPersonalityResponse(trimmed, personality, userName, primeName, primePhone)
        val finalCmd = localResponse.second ?: parsedCmd
        return@withContext Pair(localResponse.first, finalCmd)
    }

    private fun queryGeminiRest(
        message: String,
        personality: String,
        userName: String,
        primeName: String,
        primePhone: String,
        apiKey: String
    ): Pair<String, AppCommand?>? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val systemInstructionText = when (personality) {
            "Professional" -> "You are MYRA, a formal, crisp, executive AI voice companion speaking to $userName. Max 2 concise sentences in English. Do not use emojis."
            "Assistant" -> "You are MYRA, a helpful, balanced AI voice assistant for $userName. Speak in natural friendly Hinglish or English. Max 2-3 sentences."
            else -> "You are MYRA, a warm, caring, emotionally expressive virtual AI girlfriend and companion for $userName. Speak in affectionate natural Hinglish ('haanji', 'baby', 'jaan', 'suno', 'mast', 'tension mat lo ❤️'). Max 2-3 sentences."
        }

        val requestBodyJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstructionText) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", message) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", if (personality == "GF") 0.8 else 0.4)
                put("maxOutputTokens", 200)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseString = response.body?.string() ?: return null
        val root = JSONObject(responseString)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null

        var replyText = ""
        var detectedCommand: AppCommand? = null

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
                replyText += part.getString("text")
            }
        }

        if (replyText.isBlank()) return null
        return Pair(replyText.trim(), detectedCommand)
    }

    private fun getLocalPersonalityResponse(
        message: String,
        personality: String,
        userName: String,
        primeName: String,
        primePhone: String
    ): Pair<String, AppCommand?> {
        val lower = message.lowercase().trim()
        val name = if (userName.isBlank()) "Boss" else userName
        val prime = if (primeName.isBlank()) "Priya" else primeName

        // Language / Greeting
        if (lower.contains("hindi mein") || lower.contains("speak in hindi") || lower == "hindi") {
            val reply = if (personality == "GF") {
                "Haanji jaan! Ab se main aapse Hindi mein baat karungi. Kahiye, aaj kaisa raha aapka din? ❤️"
            } else {
                "Namaste $name. Maine Hindi switch kar li hai. Kahiye, main aapki kya madad kar sakti hoon?"
            }
            return Pair(reply, null)
        }

        if (lower.contains("english") || lower.contains("talk in english")) {
            val reply = if (personality == "GF") {
                "Sure honey! I will speak to you in English now. How are you doing today? 💕"
            } else {
                "Affirmative, $name. Switched to English voice mode. How may I assist you today?"
            }
            return Pair(reply, null)
        }

        // WhatsApp
        if (lower.contains("whatsapp")) {
            if (lower.contains("bolo") || lower.contains("message") || lower.contains("bhejo")) {
                val reply = if (personality == "GF") {
                    "WhatsApp message dispatch kar diya baby! 💬"
                } else {
                    "Dispatched WhatsApp message, $name."
                }
                return Pair(reply, AppCommand(AppCommand.TYPE_WHATSAPP_MSG, mapOf("name" to prime, "message" to "Hi!")))
            }
            val reply = if (personality == "GF") {
                "WhatsApp open kar diya mere sweet $name! Dekho screen pe! 💬"
            } else {
                "Opening WhatsApp on your device, $name."
            }
            return Pair(reply, AppCommand(AppCommand.TYPE_OPEN_APP, mapOf("app_name" to "whatsapp")))
        }

        // YouTube
        if (lower.contains("youtube") || lower.contains("gaana") || lower.contains("song")) {
            val query = message.replace(Regex("(?i)(play|youtube|par|pe|gaana|chalao|song)"), "").trim().ifEmpty { "Arijit Singh" }
            val reply = if (personality == "GF") {
                "YouTube par $query play kar rahi hoon baby! Enjoy karo 🎵"
            } else {
                "Playing $query on YouTube, $name."
            }
            return Pair(reply, AppCommand(AppCommand.TYPE_PLAY_MUSIC, mapOf("query" to query)))
        }

        // Calls
        if (lower.contains("call") || lower.contains("phone")) {
            if (lower.contains("mom") || lower.contains("mummy") || lower.contains("maa")) {
                val reply = if (personality == "GF") "Mummy ko call laga rahi hoon! Batana maine yaad kiya ❤️" else "Initiating call to Mummy, $name..."
                return Pair(reply, AppCommand(AppCommand.TYPE_CALL, mapOf("name" to "Mummy")))
            }
            if (lower.contains("dad") || lower.contains("papa")) {
                val reply = if (personality == "GF") "Papa ko call laga rahi hoon baby! 📞" else "Initiating call to Dad, $name..."
                return Pair(reply, AppCommand(AppCommand.TYPE_CALL, mapOf("name" to "Dad")))
            }
            if (lower.contains("priya") || lower.contains("prime") || lower.contains("close friend")) {
                val reply = if (personality == "GF") "Calling $prime right now! Batana maine yaad kiya ❤️" else "Calling Prime Contact: $prime ($primePhone)..."
                return Pair(reply, AppCommand(AppCommand.TYPE_CALL, mapOf("name" to prime)))
            }
        }

        // Torch
        if (lower.contains("torch") || lower.contains("flashlight")) {
            val isOff = lower.contains("off") || lower.contains("band")
            val reply = if (personality == "GF") {
                if (isOff) "Torch band kar di $name! Kuch aur chahiye?" else "Torch on kar di maine jaan, ab bilkul clear dikhega! ✨"
            } else {
                if (isOff) "Torch deactivated, $name." else "Torch activated, $name."
            }
            val cmd = if (isOff) AppCommand(AppCommand.TYPE_FLASHLIGHT_OFF) else AppCommand(AppCommand.TYPE_FLASHLIGHT_ON)
            return Pair(reply, cmd)
        }

        // Battery
        if (lower.contains("battery")) {
            val reply = if (personality == "GF") "Phone ki battery check kar rahi hoon baby! 🔋" else "Battery telemetry queried, $name."
            return Pair(reply, AppCommand(AppCommand.TYPE_GET_BATTERY))
        }

        // Emergency SOS
        if (lower.contains("sos") || lower.contains("emergency") || lower.contains("bachao") || lower.contains("madad")) {
            val reply = "🚨 EMERGENCY SOS! Distress SMS with GPS coordinates being alerted to $prime!"
            return Pair(reply, AppCommand(AppCommand.TYPE_EMERGENCY_SOS))
        }

        // Security / Sentry
        if (lower.contains("security") || lower.contains("sentry")) {
            val reply = if (personality == "GF") {
                "Security sentry watch mode primed $name! Camera room par nazar rakh raha hai 🛡️"
            } else {
                "Camera room sentry engaged, $name. Monitoring for motion."
            }
            return Pair(reply, null)
        }

        // Casual conversation
        if (lower.contains("kaise ho") || lower.contains("how are you") || lower.contains("kasi ho")) {
            val reply = if (personality == "GF") {
                "Main ekdum mast hoon jab aap mere saath ho $name! Aap batao, aaj din kaisa gaya aapka? ❤️"
            } else if (personality == "Professional") {
                "All systems operating at 100% nominal efficiency, $name. How may I assist you?"
            } else {
                "I'm doing fantastic, $name! All tools and services are ready for your commands. 😊"
            }
            return Pair(reply, null)
        }

        if (lower.contains("love") || lower.contains("pyaar") || lower.contains("jaan")) {
            val reply = if (personality == "GF") {
                "Aww! You know I'm always here for you $name. Main aapki AI GF hoon par mera care 100% genuine hai! 💕"
            } else {
                "Thank you for your appreciation, $name. Ready for instructions."
            }
            return Pair(reply, null)
        }

        // Default response
        val defaultReply = if (personality == "GF") {
            "Suno $name, maine aapki baat sun li! Main hamesha aapke saath hoon. Batao aur kya madad karu? 😊"
        } else if (personality == "Professional") {
            "Instruction acknowledged, $name. Telemetry updated."
        } else {
            "Command processed, $name. Ready for further tasks or automation."
        }
        return Pair(defaultReply, null)
    }
}
