package com.myra.assistant.ui.main

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myra.assistant.R
import com.myra.assistant.ai.AudioEngine
import com.myra.assistant.ai.CommandParser
import com.myra.assistant.ai.GeminiLiveClient
import com.myra.assistant.service.CallMonitorService
import com.myra.assistant.service.MyraOverlayService
import com.myra.assistant.ui.settings.SettingsActivity
import com.myra.assistant.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var redOverlay: View
    private lateinit var batteryText: TextView
    private lateinit var ramText: TextView
    private lateinit var timeText: TextView
    private lateinit var settingsBtn: ImageButton
    private lateinit var orbView: OrbAnimationView
    private lateinit var waveformView: WaveformView
    private lateinit var statusText: TextView
    private lateinit var chatRecycler: RecyclerView
    private lateinit var micButton: ImageButton

    private lateinit var chatAdapter: ChatAdapter
    private val viewModel: MainViewModel by viewModels()

    private var geminiLive: GeminiLiveClient? = null
    private var audioEngine: AudioEngine? = null

    private val inputBuffer = StringBuilder()
    private val outputBuffer = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())

    private var isInCallMode = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isInCallMode = false
            audioEngine?.isMuted = false
            orbView.setState(OrbAnimationView.OrbState.LISTENING)
            statusText.text = "Sun rahi hoon..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        startSystemServices()
        startStatusUpdates()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callEndedReceiver, IntentFilter(CallMonitorService.ACTION_CALL_ENDED), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(callEndedReceiver, IntentFilter(CallMonitorService.ACTION_CALL_ENDED))
        }

        handler.postDelayed({
            initGeminiLive()
        }, 300)

        handleIncomingCallIntent(intent)
        observeViewModel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingCallIntent(intent)
    }

    private fun initViews() {
        redOverlay = findViewById(R.id.redOverlay)
        batteryText = findViewById(R.id.batteryText)
        ramText = findViewById(R.id.ramText)
        timeText = findViewById(R.id.timeText)
        settingsBtn = findViewById(R.id.settingsBtn)
        orbView = findViewById(R.id.orbView)
        waveformView = findViewById(R.id.waveformView)
        statusText = findViewById(R.id.statusText)
        chatRecycler = findViewById(R.id.chatRecycler)
        micButton = findViewById(R.id.micButton)

        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecycler.layoutManager = layoutManager
        chatRecycler.adapter = chatAdapter

        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        micButton.setOnClickListener {
            toggleMute()
        }

        micButton.setOnLongClickListener {
            interruptMyra()
            true
        }
    }

    private fun checkPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        }
    }

    private fun startSystemServices() {
        try {
            val monitorIntent = Intent(this, CallMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(monitorIntent)
            } else {
                startService(monitorIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startStatusUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                updateBatteryAndRam()
                handler.postDelayed(this, 15000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun updateBatteryAndRam() {
        // Time
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeText.text = timeFormat.format(Date())

        // Battery
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryText.text = "BAT: $level%"

        // RAM
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val freeGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        ramText.text = String.format(Locale.US, "RAM: %.1fGB", freeGb)
    }

    private fun initGeminiLive() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("gemini_model", GeminiLiveClient.DEFAULT_MODEL) ?: GeminiLiveClient.DEFAULT_MODEL
        val voice = prefs.getString("gemini_voice", GeminiLiveClient.DEFAULT_VOICE) ?: GeminiLiveClient.DEFAULT_VOICE
        val personality = prefs.getString("personality_mode", "GF") ?: "GF"
        val userName = prefs.getString("user_name", "Boss") ?: "Boss"

        val systemPrompt = buildSystemPrompt(userName, personality)

        geminiLive = GeminiLiveClient(this).apply {
            currentApiKey = apiKey
            currentModel = model
            currentVoice = voice
            currentSystemPrompt = systemPrompt
        }

        audioEngine = AudioEngine(this)

        // Audio callbacks
        audioEngine?.onAudioChunkRecorded = { chunk ->
            if (!isInCallMode) {
                geminiLive?.sendAudioChunk(chunk)
            }
        }

        audioEngine?.onAmplitudeChanged = { rms ->
            runOnUiThread {
                waveformView.setAmplitude(rms)
                orbView.setAmplitude(rms)
            }
        }

        audioEngine?.onSpeakingStarted = {
            runOnUiThread {
                orbView.setState(OrbAnimationView.OrbState.SPEAKING)
                statusText.text = "Bol rahi hoon... 💖"
                animateRedOverlay(0.08f)
            }
        }

        audioEngine?.onSpeakingStopped = {
            runOnUiThread {
                orbView.setState(OrbAnimationView.OrbState.LISTENING)
                statusText.text = "Sun rahi hoon..."
                animateRedOverlay(0f)
            }
        }

        // WebSocket callbacks
        geminiLive?.onConnected = {
            runOnUiThread {
                statusText.text = "MYRA Online 🌟"
                micButton.setImageResource(R.drawable.ic_mic_on)
                audioEngine?.startRecording()
                audioEngine?.startPlayback()

                handler.postDelayed({
                    sendGreeting(userName, personality)
                }, 600)
            }
        }

        geminiLive?.onDisconnected = {
            runOnUiThread {
                statusText.text = "Connecting..."
                micButton.setImageResource(R.drawable.ic_mic_off)
                orbView.setState(OrbAnimationView.OrbState.IDLE)
            }
        }

        geminiLive?.onError = { err ->
            runOnUiThread {
                statusText.text = err
            }
        }

        geminiLive?.onAudioReceived = { pcmBytes ->
            audioEngine?.queueAudio(pcmBytes)
        }

        geminiLive?.onInputTranscript = { text ->
            inputBuffer.append(text)
        }

        geminiLive?.onOutputTranscript = { text ->
            outputBuffer.append(text)
        }

        geminiLive?.onTurnComplete = {
            runOnUiThread {
                val userText = inputBuffer.toString().trim()
                val myraText = outputBuffer.toString().trim()

                if (userText.isNotEmpty()) {
                    chatAdapter.addMessage(ChatMessage(userText, isUser = true))
                    chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)

                    // Parse voice command
                    val command = CommandParser.parse(userText)
                    if (command != null) {
                        viewModel.executeCommand(command)
                    }
                }

                if (myraText.isNotEmpty()) {
                    chatAdapter.addMessage(ChatMessage(myraText, isUser = false))
                    chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
                }

                inputBuffer.clear()
                outputBuffer.clear()
            }
        }

        geminiLive?.connect()
    }

    private fun buildSystemPrompt(userName: String, personality: String): String {
        val now = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val personalityBlock = when (personality) {
            "Professional" -> """
                - Formal English only.
                - Precise, efficient, no emojis.
                - Max 2 sentences per response.
            """.trimIndent()
            "Assistant" -> """
                - Friendly Hinglish or English helper.
                - Helpful and balanced.
                - Max 2-3 sentences.
            """.trimIndent()
            else -> """
                - Name: MYRA (AI Companion)
                - Language: Hinglish (natural Hindi + English mix)
                - Tone: Warm, caring, emotionally expressive
                - Use natural words: "tumhara", "haan", "acha", "bilkul"
                - Expressions: "main yahan hoon ❤️", "tumne yaad kiya? 😊"
                - Max 2-3 sentences per response.
            """.trimIndent()
        }

        return """
            You are MYRA, a production-ready AI voice companion speaking aloud to $userName.
            Current Date/Time: $now.
            $personalityBlock
            You are speaking ALOUD — keep all responses natural, punchy, and conversational (max 2-3 sentences).

            MYRA TOOLS & CAPABILITIES AWARENESS (2026 EDITION):
            1. Communication: WhatsApp messaging, direct SMS, Email inbox/compose, Emergency SOS alert.
            2. Calls: Call contact, number lookup, answer ringing call, end/reject call.
            3. Media: Play music/songs (auto-play YouTube Music/Spotify), media control (pause/next/resume), volume set %.
            4. Device: Alarms, timers, flashlight on/off, battery status, instant screen lock, clipboard, storage cleanup.
            5. Maps & Navigation: Turn-by-turn navigation (Google Maps), location coordinates, parking location memory ("yahan park kiya hai yaad rakho"), nearby ATM/hospitals/fuel.
            6. Notifications: Read recent notifications, read missed calls, explicit OTP reading.
            7. System & Screen Automation: Phone health diagnosis, background task execution and killing ("ruk jao").

            SAFETY RULES:
            - OTP Privacy: Never speak OTPs, passwords, or PINs proactively; only when the user explicitly asks ("OTP batao").
            - Instant Lock: "Lock kar do" or "phone band kar do" executes instantly with no confirmation required.
            - Call-Reject Guard: A ringing call is only rejected if the user clearly said reject within recent seconds.
            - Honesty in Data: If real-time traffic or offline map tiles are unavailable, be honest and state it.
        """.trimIndent()
    }

    private fun sendGreeting(userName: String, personality: String) {
        val greeting = when (personality) {
            "Professional" -> "Good day $userName. MYRA is online and ready to assist you."
            "Assistant" -> "Hello $userName! Main MYRA hoon. Kaise help karun aapki?"
            else -> "Hey $userName! Main aa gayi hoon. Kya help chahiye tumhe? ❤️"
        }
        geminiLive?.sendText(greeting)
    }

    private fun toggleMute() {
        val engine = audioEngine ?: return
        engine.isMuted = !engine.isMuted
        if (engine.isMuted) {
            micButton.setImageResource(R.drawable.ic_mic_off)
            statusText.text = "Mic Muted 🔇"
            orbView.setState(OrbAnimationView.OrbState.IDLE)
        } else {
            micButton.setImageResource(R.drawable.ic_mic_on)
            statusText.text = "Sun rahi hoon..."
            orbView.setState(OrbAnimationView.OrbState.LISTENING)
        }
    }

    private fun interruptMyra() {
        audioEngine?.interruptAndClear()
        geminiLive?.sendInterrupt()
        orbView.setState(OrbAnimationView.OrbState.LISTENING)
        statusText.text = "Interrupted. Sun rahi hoon..."
        animateRedOverlay(0f)
    }

    private fun animateRedOverlay(targetAlpha: Float) {
        val currentAlpha = redOverlay.alpha
        val animator = ValueAnimator.ofFloat(currentAlpha, targetAlpha)
        animator.duration = if (targetAlpha > 0) 300 else 500
        animator.addUpdateListener {
            redOverlay.alpha = it.animatedValue as Float
        }
        animator.start()
    }

    private fun handleIncomingCallIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("INCOMING_CALL", false) == true) {
            val callerName = intent.getStringExtra("CALLER_NAME") ?: "Someone"
            announceCall(callerName)
        }
    }

    private fun announceCall(callerName: String) {
        isInCallMode = true
        audioEngine?.isMuted = true
        orbView.setState(OrbAnimationView.OrbState.SPEAKING)

        val announcement = "Sir, $callerName ka call aa raha hai. Uthau ya reject karu?"
        geminiLive?.sendText(announcement)

        // After announcement -> start STT to listen for user decision
        handler.postDelayed({
            listenForCallDecision()
        }, 4500)
    }

    private fun listenForCallDecision() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.lowercase() ?: ""

                if (text.contains("uthao") || text.contains("haan") || text.contains("accept") || text.contains("pick")) {
                    viewModel.acceptCall()
                    geminiLive?.sendText("Call utha li hai.")
                } else if (text.contains("reject") || text.contains("nahi") || text.contains("mat") || text.contains("cut")) {
                    viewModel.rejectCall()
                    geminiLive?.sendText("Call reject kar di hai.")
                }

                isInCallMode = false
                audioEngine?.isMuted = false
                speechRecognizer?.destroy()
            }

            override fun onError(error: Int) {
                isInCallMode = false
                audioEngine?.isMuted = false
                speechRecognizer?.destroy()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(sttIntent)
    }

    private fun observeViewModel() {
        viewModel.commandResult.observe(this) { result ->
            if (!result.isNullOrBlank()) {
                chatAdapter.addMessage(ChatMessage("MYRA: $result", isUser = false))
                chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
                geminiLive?.sendText(result)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (audioEngine != null && !audioEngine!!.isMuted) {
            audioEngine?.startRecording()
        }
    }

    override fun onPause() {
        super.onPause()
        audioEngine?.stopRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        geminiLive?.disconnect()
        audioEngine?.release()
        try {
            unregisterReceiver(callEndedReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
