# 🌟 M Y R A

## Personal AI Voice Assistant — Complete Project Roadmap
### Android App + Python Desktop (MYRA TITAN) + Web Companion

**Project:** MYRA / MYRA TITAN — Personal AI Voice Assistant  
**Platforms:** Android (Kotlin) + Windows/Linux Desktop (Python TITAN) + Web Companion  
**AI Core:** Gemini 2.5 Flash Native Audio Preview (Live Bi-directional WebSocket)  
**Status:** Active Development — Phase 1 Complete, Phase 2 in Progress, Phase 3 Planned  
**Voice Quality:** Aoede (Native 24kHz PCM — true natural expressive voice, not robotic TTS)  
**Architecture:** Mic (16kHz PCM) → WebSocket → Gemini Live API (v1alpha) → PCM Audio (24kHz) → Speaker  

### LEGEND
- ● **DONE** Feature complete & tested
- 🟡 **IN PROGRESS** Currently being built / tested
- 🔵 **PLANNED** Upcoming roadmap items
- 🛠️ **FIX NEEDED / PATCHED** Resolved bugs and security hardenings

---

## 🎯 PROJECT OVERVIEW

**MYRA** is a next-generation personal voice AI companion that operates across two primary platforms:
1. **Android Phone (Kotlin)**: Deep device control, camera sentry, phone calls, WhatsApp automations, contacts lookup, and screen reading.
2. **Desktop (Python TITAN)**: System telemetry, app launcher, volume/brightness, camera vision, window management, and FastAPI local bridge.

Both use the **Gemini 2.5 Flash Native Audio** model (`gemini-2.5-flash-native-audio-preview-12-2025`) enabling instant real-time audio interaction with zero latency TTS overhead.

### Android Flow (MYRA)
```text
MainActivity (Compose UI)
  ↓
VoiceAssistantManager / GeminiLiveClient
  ↓ AudioRecord (16kHz PCM, VOICE_COMMUNICATION)
  ↓ OkHttp WebSocket (v1alpha BidiGenerateContent)
Gemini Live API (Aoede Voice)
  ↓ AudioTrack (24kHz PCM Linear16 stream)
Device Speaker
```

### Desktop Flow (MYRA TITAN Python)
```text
TITANWindow (PyQt5 / Overlay UI)
  ↓
GeminiWorker (QObject / AsyncIO)
  ↓ sounddevice InputStream (16kHz PCM)
  ↓ websockets.connect (v1alpha BidiGenerateContent)
Gemini Live API (Aoede Voice)
  ↓ sounddevice OutputStream (24kHz float32/int16)
Desktop Speakers
```

### Desktop Repository Structure (TITAN)
```text
TITAN/
├── backend/
│   ├── bridge_server.py           # FastAPI REST & WebSocket server
│   ├── alarms.json                # Alarm schedule database
│   └── requirements.txt
└── core/
    ├── wake_word_detection.py     # "Hey MYRA" wake word listener
    ├── myra_controller.py         # Central command coordinator
    ├── system_controller.py       # Volume, brightness, instant lock
    ├── app_launcher.py            # Installed app detector & launch
    ├── camera_vision.py           # OpenCV frame capture + Gemini Vision
    ├── desktop_features.py        # Shortcuts, theme, clipboard
    ├── overlay.py                 # Glowing pulsating orb mini-UI
    ├── gnews.py                   # GNews headlines integration
    ├── myra_email.py              # SMTP email dispatcher
    └── mobile_controller.py       # HTTP client to Android APK bridge
```

---

## 🚀 PHASE 1: FOUNDATION — Core Voice Loop
**Android + Python | Status: ● MOSTLY DONE**

### 1.1. Gemini Live WebSocket Connection ● DONE
- **Endpoint:** `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key={API_KEY}`
- **Model:** `models/gemini-2.5-flash-native-audio-preview-12-2025`
- Waits for `setupComplete` message from server before sending user turns or greetings.

### 1.2. Setup Message Structure ● DONE
- `generation_config`: `{ response_modalities: ['AUDIO'] }`
- `speech_config`: `{ voice_config: { prebuilt_voice_config: { voice_name: 'Aoede' } } }`
- `output_audio_config`: `{ audio_encoding: 'LINEAR16', sample_rate_hertz: 24000 }`

### 1.3. Mic Capture → `realtimeInput` ● DONE
- **Sample Rate:** 16,000 Hz, 16-bit mono PCM.
- **RMS Threshold:** 80 (audio below this is categorized as silence).
- **Silence Frames:** 10 frames (~200ms) silence triggers automatic `audioStreamEnd`.
- **Android:** Uses native `AudioRecord` with `MediaRecorder.AudioSource.VOICE_COMMUNICATION` (hardware echo cancellation).

### 1.4. Audio Playback — 24kHz PCM ● DONE
- Direct playback of base64 decoded inline audio bytes from Gemini.
- **Android:** Native `AudioTrack` (`STREAM_MUSIC`, 24000 Hz, `ENCODING_PCM_16BIT`, `MODE_STREAM`) with $4\times$ buffer multiplier.
- **Python:** `sounddevice.OutputStream` (normalized float32/int16).

### 1.5. Keepalive & 9-Minute Session Renewal ● DONE
- Sends 640 zero-bytes (20ms silence @ 16kHz) every 8 seconds to prevent idle disconnects.
- `SESSION_RENEW_AFTER` = 540 seconds (9 minutes). Reconnects transparently and re-injects conversation memory.

### 1.6. Auto Gemini Session Initialization ● DONE
- App launch automatically initializes Gemini Live if API key is present.
- Mic toggle reconnects or restarts session with fresh turn context.

---

## 💻 PHASE 2: DESKTOP FEATURES — Python TITAN
**Python Desktop | Status: 🟡 IN PROGRESS**

### 2.1. Bridge Server (FastAPI) 🟡 IN PROGRESS
- `POST /command`: Handles verbal commands mapped to `myra_controller.py`.
- `POST /launch-app`: Spawns application binaries via `app_launcher.py`.
- `POST /keyboard`: Synthesizes keystrokes (Ctrl+C, Ctrl+V, Win+V).
- `POST /window`: Minimizes, maximizes, or snaps active windows.
- `GET /status`: Returns telemetry, CPU load, memory, and connectivity status.

### 2.2. System Controller (Volume, Brightness, Lock) ● DONE
- Volume Up/Down/Mute via `pyautogui` or system APIs.
- Brightness adjustment via `screen_brightness_control`.
- Instant Lock: `ctypes.windll.user32.LockWorkStation()` on Windows or `loginctl lock-session` on Linux.

### 2.3. App Launcher ● DONE
- Dynamic scanning of Windows Start Menu and Linux `/usr/share/applications`.
- Natural variations supported: `"Open Chrome"`, `"Chrome kholo"`, `"Play music on YouTube"`.

### 2.4. Camera Vision (Gemini Vision) 🟡 IN PROGRESS
- Captures frames using `cv2.VideoCapture(0)`.
- Sends frame base64 to Gemini: *"Describe what you see in front of the screen."*
- Verbal feedback: *"I can see a person sitting at a desk with two monitors."*

### 2.5. Desktop Overlay UI 🟡 IN PROGRESS
- Glowing pulsating orb floating widget in PyQt5 reflecting assistant states:
  - `IDLE`: Subtle gentle blue/purple breathing.
  - `LISTENING`: Reactive red wave pulsing to microphone RMS amplitude.
  - `THINKING`: Revolving dual-orbit rings.
  - `SPEAKING`: Vibrant neon ripple matching speech frequency.

### 2.6. News Service (GNews API) ● DONE
- Real-time headline retrieval across Tech, Business, World, Sports.
- Tool Call: `get_news(category="technology")`.

### 2.7. Email Integration 🟡 IN PROGRESS
- Voice compose and send via SMTP with SSL using Gmail App Passwords.

### 2.8. Wake Word Detection ("Hey MYRA") 🟡 IN PROGRESS
- Background thread monitoring for `"Hey MYRA"` / `"Suno MYRA"`.
- Activates listening state and plays welcome chime.

---

## 📱 PHASE 3: ANDROID ADVANCED FEATURES
**Android Kotlin | Status: 🔵 PLANNED**

### 3.1. WhatsApp Integration (Message + Call) 🔵 PLANNED
- Open WhatsApp chat with phone number or contact name.
- Deep link: `whatsapp://send?phone=+91...&text=...`
- Native Android Bridge via `MyraAndroidBridge.openWhatsApp()`.

### 3.2. Incoming Call Handler (Announce + Answer / Reject) 🟡 IN PROGRESS
- `CallReceiver` captures `TelephonyManager.ACTION_PHONE_STATE_CHANGED`.
- MYRA speaks: *"Sir, [Name] ka call aa raha hai. Uthau ya reject karu?"*
- User voice response: *"Uthao"* (answers) or *"Reject karo"* (ends call).

### 3.3. Accessibility Screen Reading (`ScreenMonitor.kt`) 🔵 PLANNED
- Traverses active `AccessibilityNodeInfo` tree.
- Summarizes on-screen emails, PDFs, news, and social feeds on voice request.

### 3.4. Direct SMS Integration 🔵 PLANNED
- Reads recent SMS messages and sends replies via `SmsManager`.

### 3.5. Android ↔ Desktop Cross-Device Bridge 🔵 PLANNED
- Android app communicates with Desktop FastAPI bridge server over local Wi-Fi.
- Example: Phone voice command *"PC pe Chrome kholo"* launches Chrome on desktop.

---

## 🧠 PHASE 4: MEMORY + INTELLIGENCE UPGRADES
**Both Platforms | Status: 🔵 PLANNED**

### 4.1. Conversation Memory Across Sessions 🟡 PARTIALLY DONE
- Retains last 20 conversation turns and user preferences.
- Injects memory state during 9-minute session renewal reconnects.

### 4.2. User Profile Personal Context 🔵 PLANNED
- Name, relations (Mummy, Papa, Rahul, Priya), work timings, preferences.
- Customized greeting based on time of day.

### 4.3. Proactive Reminders & Timers 🟡 IN PROGRESS
- `"5 minute ka Chai timer lagao"` → Background countdown with alarm chime.
- Stored persistently in local storage / SQLite.

### 4.4. Multi-Language Automatic Detection ● DONE
- Understands and speaks in Hindi, English, and natural Hinglish.
- Automatic language switching mid-conversation without user configuration.

---

## 🛠️ PHASE 5: BUG FIXES & STABILITY PATCHES
**Status: RESOLVED**

| Issue | Root Cause | Solution Applied |
| :--- | :--- | :--- |
| **Robotic TTS Voice** | `SpeechSynthesis` was overriding audio | Switched to native 24kHz PCM stream from Gemini Live using `Aoede` voice. |
| **"Connecting to MYRA" Stuck** | Setup completion event was dropped | Implemented `setupComplete` listener that automatically sends the initial greeting turn. |
| **2-Minute Disconnect** | Server idle timeout dropped WebSocket | Switched to `v1alpha` endpoint + 8-second 640-byte silent audio keepalive frames. |
| **Speech Config Placement** | Placed inside `generation_config` | Moved `speech_config` to root setup object alongside `generation_config`. |
| **API Key Exposure** | Hardcoded secrets in client files | Moved to `.env` server-side proxy and client `SettingsModal` encrypted storage. |

---

## 📦 SETUP & INSTALLATION SUMMARY

### Android (build.gradle.kts)
```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.navigation:navigation-compose:2.7.5")
```

### Python Desktop (TITAN)
```bash
pip install websockets sounddevice numpy PyQt5 opencv-python
pip install google-genai python-dotenv psutil
pip install fastapi uvicorn requests pyautogui screen-brightness-control
```

### Quick Commands
```bash
# Start Python TITAN Companion
cd TITAN/backend
python wake_word_detection.py

# Start FastAPI Bridge Server
cd TITAN/backend
uvicorn bridge_server:app --host 0.0.0.0 --port 8000 --reload

# Start Web Companion Dev Server
npm run dev
```

---
*MYRA AI Project — Powered by Google Gemini 2.5 Flash Native Audio | Identity: MYRA*
