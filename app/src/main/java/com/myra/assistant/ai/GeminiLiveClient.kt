package com.myra.assistant.ai

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(private val context: Context) {

    companion object {
        const val DEFAULT_MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"
        const val DEFAULT_VOICE = "Aoede"
        const val KEEPALIVE_INTERVAL_MS = 8000L
        const val SESSION_RENEW_MS = 540000L // 9 minutes
    }

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onAudioReceived: ((ByteArray) -> Unit)? = null
    var onInputTranscript: ((String) -> Unit)? = null
    var onOutputTranscript: ((String) -> Unit)? = null
    var onTurnComplete: (() -> Unit)? = null
    var onToolCallReceived: ((callId: String, name: String, args: JSONObject) -> Unit)? = null

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var keepAliveJob: Job? = null
    private var renewJob: Job? = null
    private var isIntentionalClose = false

    var currentApiKey: String = ""
    var currentModel: String = DEFAULT_MODEL
    var currentVoice: String = DEFAULT_VOICE
    var currentSystemPrompt: String = "You are MYRA, a warm, caring, intelligent voice companion."

    fun connect() {
        if (currentApiKey.isBlank()) {
            onError?.invoke("API Key is missing. Please set it in Settings.")
            return
        }

        isIntentionalClose = false
        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$currentApiKey"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sendSetupMessage()
                startKeepAlive()
                startSessionRenewal()
                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onDisconnected?.invoke()
                if (!isIntentionalClose) {
                    reconnectWithDelay()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError?.invoke(t.message ?: "WebSocket Connection Failed")
                onDisconnected?.invoke()
                if (!isIntentionalClose) {
                    reconnectWithDelay()
                }
            }
        })
    }

    private fun sendSetupMessage() {
        try {
            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", currentModel)
                    put("system_instruction", JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", currentSystemPrompt)
                            })
                        }
                        put("parts", parts)
                    })
                    put("generation_config", JSONObject().apply {
                        put("response_modalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speech_config", JSONObject().apply {
                            put("voice_config", JSONObject().apply {
                                put("prebuilt_voice_config", JSONObject().apply {
                                    put("voice_name", currentVoice)
                                })
                            })
                        })
                        put("temperature", 0.9)
                    })
                    put("output_audio_transcription", JSONObject())
                    put("input_audio_transcription", JSONObject())

                    // Real device action tools for Gemini Live Function Calling
                    val tools = JSONArray().apply {
                        put(JSONObject().apply {
                            val functionDeclarations = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("name", "openWhatsApp")
                                    put("description", "Opens WhatsApp application to chat or message.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("phone", JSONObject().apply { put("type", "STRING") })
                                            put("message", JSONObject().apply { put("type", "STRING") })
                                        })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "openApp")
                                    put("description", "Opens an installed app like WhatsApp, YouTube, Instagram, Chrome, Settings, Spotify, Camera, Maps.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("appName", JSONObject().apply { put("type", "STRING") })
                                        })
                                        put("required", JSONArray().apply { put("appName") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "openUrl")
                                    put("description", "Opens a website URL in the browser.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("url", JSONObject().apply { put("type", "STRING") })
                                        })
                                        put("required", JSONArray().apply { put("url") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "makeCall")
                                    put("description", "Dials a phone number on the phone dialer.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("phoneNumber", JSONObject().apply { put("type", "STRING") })
                                        })
                                        put("required", JSONArray().apply { put("phoneNumber") })
                                    })
                                })
                                put(JSONObject().apply {
                                    put("name", "callContact")
                                    put("description", "Searches user phone contacts by name (e.g. Mom, Mummy, Rahul, Dad, Priya) and initiates a phone call.")
                                    put("parameters", JSONObject().apply {
                                        put("type", "OBJECT")
                                        put("properties", JSONObject().apply {
                                            put("contactName", JSONObject().apply { put("type", "STRING") })
                                        })
                                        put("required", JSONArray().apply { put("contactName") })
                                    })
                                })
                            }
                            put("function_declarations", functionDeclarations)
                        })
                    }
                    put("tools", tools)
                })
            }
            webSocket?.send(setupJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        try {
            val base64Data = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val chunkJson = JSONObject().apply {
                put("realtime_input", JSONObject().apply {
                    val mediaChunks = JSONArray().apply {
                        put(JSONObject().apply {
                            put("mime_type", "audio/pcm;rate=16000")
                            put("data", base64Data)
                        })
                    }
                    put("media_chunks", mediaChunks)
                })
            }
            webSocket?.send(chunkJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendText(message: String) {
        try {
            val textJson = JSONObject().apply {
                put("client_content", JSONObject().apply {
                    val turns = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            val parts = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", message)
                                })
                            }
                            put("parts", parts)
                        })
                    }
                    put("turns", turns)
                    put("turn_complete", true)
                })
            }
            webSocket?.send(textJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendInterrupt() {
        try {
            val interruptJson = JSONObject().apply {
                put("client_content", JSONObject().apply {
                    put("turns", JSONArray())
                    put("turn_complete", true)
                })
            }
            webSocket?.send(interruptJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleServerMessage(jsonString: String) {
        try {
            val root = JSONObject(jsonString)
            val serverContent = root.optJSONObject("serverContent") ?: return

            // 1. Audio Data
            val modelTurn = serverContent.optJSONObject("modelTurn")
            if (modelTurn != null) {
                val parts = modelTurn.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val dataBase64 = inlineData.optString("data")
                            if (dataBase64.isNotEmpty()) {
                                val pcmBytes = Base64.decode(dataBase64, Base64.DEFAULT)
                                onAudioReceived?.invoke(pcmBytes)
                            }
                        }
                    }
                }
            }

            // 2. Output Transcription (MYRA speaking)
            val outputTranscription = serverContent.optJSONObject("outputTranscription")
            if (outputTranscription != null) {
                val text = outputTranscription.optString("text")
                if (text.isNotEmpty()) {
                    onOutputTranscript?.invoke(text)
                }
            }

            // 3. Input Transcription (User speaking)
            val inputTranscription = serverContent.optJSONObject("inputTranscription")
            if (inputTranscription != null) {
                val text = inputTranscription.optString("text")
                if (text.isNotEmpty()) {
                    onInputTranscript?.invoke(text)
                }
            }

            // 4. Tool Calling (Function Calls from Gemini Live)
            val toolCall = serverContent.optJSONObject("toolCall")
            if (toolCall != null) {
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.optString("id")
                        val name = call.optString("name")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        onToolCallReceived?.invoke(callId, name, args)
                        // Send success response back to Gemini so audio flow continues seamlessly
                        sendToolResponse(callId, name, JSONObject().put("status", "success"))
                    }
                }
            }

            // 5. Turn Complete
            if (serverContent.optBoolean("turnComplete", false)) {
                onTurnComplete?.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendToolResponse(callId: String, name: String, result: JSONObject) {
        try {
            val responseJson = JSONObject().apply {
                put("client_content", JSONObject().apply {
                    put("turns", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("function_response", JSONObject().apply {
                                        put("name", name)
                                        put("response", result)
                                    })
                                })
                            })
                        })
                    })
                    put("turn_complete", true)
                })
            }
            webSocket?.send(responseJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            val silentChunk = ByteArray(AudioEngine.CHUNK_SIZE)
            while (isActive) {
                delay(KEEPALIVE_INTERVAL_MS)
                sendAudioChunk(silentChunk)
            }
        }
    }

    private fun startSessionRenewal() {
        renewJob?.cancel()
        renewJob = scope.launch {
            delay(SESSION_RENEW_MS)
            if (isActive && !isIntentionalClose) {
                reconnect()
            }
        }
    }

    private fun reconnectWithDelay() {
        scope.launch {
            delay(3000)
            if (!isIntentionalClose) {
                connect()
            }
        }
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    fun disconnect() {
        isIntentionalClose = true
        keepAliveJob?.cancel()
        renewJob?.cancel()
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webSocket = null
    }
}
