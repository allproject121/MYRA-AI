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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myra.assistant.R
import com.myra.assistant.ai.AudioEngine
import com.myra.assistant.ai.GeminiLiveClient
import com.myra.assistant.ai.MyraAiEngine
import com.myra.assistant.model.AppCommand
import com.myra.assistant.service.CallMonitorService
import com.myra.assistant.service.PermissionManager
import com.myra.assistant.ui.dialogs.*
import com.myra.assistant.ui.settings.SettingsActivity
import com.myra.assistant.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var redOverlay: View
    private lateinit var batteryText: TextView
    private lateinit var ramText: TextView
    private lateinit var timeText: TextView
    private lateinit var personalityBadgeText: TextView
    private lateinit var guideBtn: ImageButton
    private lateinit var settingsBtn: ImageButton

    // Quick Action Bar
    private lateinit var actionToolsBtn: View
    private lateinit var actionSecurityBtn: View
    private lateinit var actionUtilitiesBtn: View
    private lateinit var actionSocialBtn: View
    private lateinit var actionCallBtn: View
    private lateinit var actionSosBtn: View

    // Center Views
    private lateinit var orbView: OrbAnimationView
    private lateinit var waveformView: WaveformView
    private lateinit var statusText: TextView

    // Chat & Input Views
    private lateinit var chatRecycler: RecyclerView
    private lateinit var chatInputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var micButton: ImageButton
    private lateinit var speakerToggleBtn: ImageButton

    // Suggestion Chips
    private lateinit var chipWhatsapp: TextView
    private lateinit var chipYoutube: TextView
    private lateinit var chipCallMom: TextView
    private lateinit var chipTorch: TextView
    private lateinit var chipBattery: TextView
    private lateinit var chipSecurity: TextView

    private lateinit var chatAdapter: ChatAdapter
    private val viewModel: MainViewModel by viewModels()
    private val aiEngine = MyraAiEngine()

    private var tts: TextToSpeech? = null
    private var isTtsMuted = false
    private var isTtsReady = false

    private var geminiLive: GeminiLiveClient? = null
    private var audioEngine: AudioEngine? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val handler = Handler(Looper.getMainLooper())
    private var isInCallMode = false

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isInCallMode = false
            audioEngine?.isMuted = false
            orbView.setState(OrbAnimationView.OrbState.IDLE)
            statusText.text = "Sun rahi hoon..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initTts()
        checkPermissions()
        startSystemServices()
        startStatusUpdates()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callEndedReceiver, IntentFilter(CallMonitorService.ACTION_CALL_ENDED), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(callEndedReceiver, IntentFilter(CallMonitorService.ACTION_CALL_ENDED))
        }

        handler.postDelayed({
            initGeminiLiveIfConfigured()
        }, 500)

        handleIncomingCallIntent(intent)
        observeViewModel()
        sendInitialGreeting()
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
        personalityBadgeText = findViewById(R.id.personalityBadgeText)
        guideBtn = findViewById(R.id.guideBtn)
        settingsBtn = findViewById(R.id.settingsBtn)

        actionToolsBtn = findViewById(R.id.actionToolsBtn)
        actionSecurityBtn = findViewById(R.id.actionSecurityBtn)
        actionUtilitiesBtn = findViewById(R.id.actionUtilitiesBtn)
        actionSocialBtn = findViewById(R.id.actionSocialBtn)
        actionCallBtn = findViewById(R.id.actionCallBtn)
        actionSosBtn = findViewById(R.id.actionSosBtn)

        orbView = findViewById(R.id.orbView)
        waveformView = findViewById(R.id.waveformView)
        statusText = findViewById(R.id.statusText)

        chatRecycler = findViewById(R.id.chatRecycler)
        chatInputField = findViewById(R.id.chatInputField)
        sendButton = findViewById(R.id.sendButton)
        micButton = findViewById(R.id.micButton)
        speakerToggleBtn = findViewById(R.id.speakerToggleBtn)

        chipWhatsapp = findViewById(R.id.chipWhatsapp)
        chipYoutube = findViewById(R.id.chipYoutube)
        chipCallMom = findViewById(R.id.chipCallMom)
        chipTorch = findViewById(R.id.chipTorch)
        chipBattery = findViewById(R.id.chipBattery)
        chipSecurity = findViewById(R.id.chipSecurity)

        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecycler.layoutManager = layoutManager
        chatRecycler.adapter = chatAdapter

        setupListeners()
        updatePersonalityBadge()
    }

    private fun setupListeners() {
        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        guideBtn.setOnClickListener {
            ToolsGuideDialog(this).show()
        }

        // Quick Action Bar
        actionToolsBtn.setOnClickListener {
            ToolsActionCenterDialog(this, viewModel) { notice ->
                statusText.text = notice
            }.show()
        }

        actionSecurityBtn.setOnClickListener {
            SecurityModeDialog(
                this,
                onIntruderAlertTriggered = {
                    triggerIntruderAlert()
                },
                onPinVerified = { success ->
                    if (success) {
                        statusText.text = "Device unlocked!"
                    }
                }
            ).show()
        }

        actionUtilitiesBtn.setOnClickListener {
            PhoneUtilitiesDialog(this) { notice ->
                statusText.text = notice
            }.show()
        }

        actionSocialBtn.setOnClickListener {
            SocialMediaAgentDialog(this, viewModel) { notice ->
                statusText.text = notice
            }.show()
        }

        actionCallBtn.setOnClickListener {
            CallAssistantDialog(
                this,
                viewModel,
                onSpeakRequest = { prompt ->
                    speakOut(prompt)
                },
                onNotice = { notice ->
                    statusText.text = notice
                }
            ).show()
        }

        actionSosBtn.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_EMERGENCY_SOS))
            statusText.text = "🚨 SOS Triggered!"
        }

        // Suggestion Chips
        chipWhatsapp.setOnClickListener { handleUserTextMessage("WhatsApp kholo") }
        chipYoutube.setOnClickListener { handleUserTextMessage("YouTube chalao") }
        chipCallMom.setOnClickListener { handleUserTextMessage("Mummy ko call karo") }
        chipTorch.setOnClickListener { handleUserTextMessage("Torch on karo") }
        chipBattery.setOnClickListener { handleUserTextMessage("Battery kitni hai") }
        chipSecurity.setOnClickListener { handleUserTextMessage("Security sentry watch") }

        // Send text message
        sendButton.setOnClickListener {
            val text = chatInputField.text.toString().trim()
            if (text.isNotEmpty()) {
                chatInputField.setText("")
                handleUserTextMessage(text)
            }
        }

        chatInputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = chatInputField.text.toString().trim()
                if (text.isNotEmpty()) {
                    chatInputField.setText("")
                    handleUserTextMessage(text)
                }
                true
            } else false
        }

        // Speaker TTS toggle
        speakerToggleBtn.setOnClickListener {
            isTtsMuted = !isTtsMuted
            if (isTtsMuted) {
                speakerToggleBtn.setImageResource(R.drawable.ic_volume_mute)
                tts?.stop()
                statusText.text = "Voice Output Muted 🔇"
            } else {
                speakerToggleBtn.setImageResource(R.drawable.ic_volume_up)
                statusText.text = "Voice Output Active 🔊"
            }
        }

        // Mic Button
        micButton.setOnClickListener {
            toggleListening()
        }

        micButton.setOnLongClickListener {
            stopAllSpeechAndListening()
            true
        }
    }

    private fun initTts() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runOnUiThread {
                        orbView.setState(OrbAnimationView.OrbState.SPEAKING)
                        waveformView.setAmplitude(0.7f)
                        statusText.text = "Bol rahi hoon... 💖"
                        animateRedOverlay(0.08f)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        orbView.setState(OrbAnimationView.OrbState.IDLE)
                        waveformView.setAmplitude(0f)
                        statusText.text = "Tap mic ya bol kar batao 💬"
                        animateRedOverlay(0f)
                    }
                }

                override fun onError(utteranceId: String?) {
                    runOnUiThread {
                        orbView.setState(OrbAnimationView.OrbState.IDLE)
                        waveformView.setAmplitude(0f)
                        animateRedOverlay(0f)
                    }
                }
            })
        }
    }

    private fun speakOut(text: String) {
        if (isTtsMuted || !isTtsReady || text.isBlank()) return

        val cleanText = text.replace(Regex("[*_#`❤️💕😊✨💬🚀]"), "").trim()
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val personality = prefs.getString("personality_mode", "GF") ?: "GF"

        when (personality) {
            "Professional" -> {
                tts?.setPitch(0.95f)
                tts?.setSpeechRate(1.0f)
            }
            "Assistant" -> {
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(1.0f)
            }
            else -> {
                // GF Mode
                tts?.setPitch(1.2f)
                tts?.setSpeechRate(1.02f)
            }
        }

        // Multilingual dialect
        val loc = if (cleanText.any { it in '\u0900'..'\u097F' } ||
            personality == "GF" ||
            cleanText.contains(Regex("(?i)\\b(haan|kholo|karo|rahi|raha|suno|kaho|namaste|theek|bhai|priya)\\b"))) {
            Locale("hi", "IN")
        } else {
            Locale.US
        }

        tts?.language = loc
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "myra_utterance_${System.currentTimeMillis()}")
    }

    private fun handleUserTextMessage(userText: String) {
        chatAdapter.addMessage(ChatMessage(userText, isUser = true))
        chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)

        orbView.setState(OrbAnimationView.OrbState.THINKING)
        statusText.text = "Soch rahi hoon... 💭"

        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        val personality = prefs.getString("personality_mode", "GF") ?: "GF"
        val userName = prefs.getString("user_name", "Boss") ?: "Boss"

        val jsonStr = prefs.getString("prime_contacts_json", null)
        var primeName = "Priya"
        var primeNumber = "+919876543210"
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    primeName = obj.optString("name", "Priya")
                    primeNumber = obj.optString("number", "+919876543210")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        lifecycleScope.launch {
            val result = aiEngine.processUserMessage(userText, personality, userName, primeName, primeNumber, apiKey)
            val replyText = result.first
            val command = result.second

            chatAdapter.addMessage(ChatMessage(replyText, isUser = false))
            chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)

            // Speak response
            speakOut(replyText)

            // Execute action command if detected
            if (command != null) {
                viewModel.executeCommand(command)
            }
        }
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                orbView.setState(OrbAnimationView.OrbState.LISTENING)
                waveformView.setAmplitude(0.4f)
                statusText.text = "Sun rahi hoon... Bolye 🎙️"
                micButton.setImageResource(R.drawable.ic_mic_on)
            }

            override fun onBeginningOfSpeech() {
                waveformView.setAmplitude(0.8f)
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = (rmsdB / 10f).coerceIn(0.1f, 1f)
                waveformView.setAmplitude(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                waveformView.setAmplitude(0f)
            }

            override fun onError(error: Int) {
                stopListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                stopListening()
                if (text.isNotBlank()) {
                    handleUserTextMessage(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        micButton.setImageResource(R.drawable.ic_mic_off)
        waveformView.setAmplitude(0f)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        orbView.setState(OrbAnimationView.OrbState.IDLE)
        statusText.text = "Tap mic ya bol kar batao 💬"
    }

    private fun stopAllSpeechAndListening() {
        stopListening()
        tts?.stop()
        audioEngine?.interruptAndClear()
        geminiLive?.sendInterrupt()
        orbView.setState(OrbAnimationView.OrbState.IDLE)
        waveformView.setAmplitude(0f)
        statusText.text = "Stopped. Sun rahi hoon..."
        animateRedOverlay(0f)
    }

    private fun triggerIntruderAlert() {
        animateRedOverlay(0.4f)
        orbView.setState(OrbAnimationView.OrbState.SPEAKING)
        statusText.text = "🚨 INTRUDER DETECTED! SENTRY ALARM TRIGGERED!"
        speakOut("Alert! Unauthorized motion detected in the room! Warning!")
        handler.postDelayed({
            animateRedOverlay(0f)
            orbView.setState(OrbAnimationView.OrbState.IDLE)
        }, 5000)
    }

    private fun sendInitialGreeting() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val personality = prefs.getString("personality_mode", "GF") ?: "GF"
        val userName = prefs.getString("user_name", "Boss") ?: "Boss"

        val greeting = when (personality) {
            "Professional" -> "Good day $userName. MYRA is online and operational. How may I assist you?"
            "Assistant" -> "Hello $userName! Main MYRA hoon. Kaise help karun aapki aaj? 😊"
            else -> "Namaste $userName! MYRA AI Companion online hai 💖. Main aapki kya madad kar sakti hoon?"
        }

        chatAdapter.addMessage(ChatMessage(greeting, isUser = false))
        speakOut(greeting)
    }

    private fun updatePersonalityBadge() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val personality = prefs.getString("personality_mode", "GF") ?: "GF"
        personalityBadgeText.text = when (personality) {
            "Professional" -> "💼 PRO MODE"
            "Assistant" -> "🤖 ASSISTANT"
            else -> "💖 GF MODE"
        }
    }

    private fun checkPermissions() {
        val missing = PermissionManager.getCriticalMissingPermissions(this)
        if (missing.isEmpty()) {
            PermissionManager.markInitialCheckCompleted(this)
            return
        }
        PermissionManager.requestPermissions(
            this,
            missing.toTypedArray(),
            PermissionManager.REQUEST_CODE_RUNTIME_PERMISSIONS
        )
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
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        batteryText.text = if (isCharging) "BAT: $level% ⚡" else "BAT: $level%"

        // RAM
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val freeGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        ramText.text = String.format(Locale.US, "RAM: %.1fGB", freeGb)
    }

    private fun initGeminiLiveIfConfigured() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isBlank()) return

        val model = prefs.getString("gemini_model", GeminiLiveClient.DEFAULT_MODEL) ?: GeminiLiveClient.DEFAULT_MODEL
        val voice = prefs.getString("gemini_voice", GeminiLiveClient.DEFAULT_VOICE) ?: GeminiLiveClient.DEFAULT_VOICE

        geminiLive = GeminiLiveClient(this).apply {
            currentApiKey = apiKey
            currentModel = model
            currentVoice = voice
        }

        audioEngine = AudioEngine(this)
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

        geminiLive?.onAudioReceived = { pcmBytes ->
            audioEngine?.queueAudio(pcmBytes)
        }
        geminiLive?.connect()
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
        orbView.setState(OrbAnimationView.OrbState.SPEAKING)
        val announcement = "Sir, $callerName ka call aa raha hai. Uthau ya reject karu?"
        speakOut(announcement)
    }

    private fun observeViewModel() {
        viewModel.commandResult.observe(this) { result ->
            if (!result.isNullOrBlank()) {
                chatAdapter.addMessage(ChatMessage(result, isUser = false))
                chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
                speakOut(result)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePersonalityBadge()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        geminiLive?.disconnect()
        audioEngine?.release()
        try {
            unregisterReceiver(callEndedReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
