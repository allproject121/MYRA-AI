# 🛡️ MYRA — Complete Android Permission Architecture & Security Blueprint

> **Official Flow:** `Check → Explain → Request → User Approves → Verify → Continue`  
> **Security Mandate:** Strict compliance with Google Play Developer Policies and Android Platform Security. Zero root exploits, zero silent grants, zero hidden bypasses.

---

## 1. Architectural Philosophy & Core Rules

### 1.1. Automatic First-Launch Check
When MYRA launches for the first time, `PermissionManager` automatically inspects device permissions and classifies them:
- **Normal Permissions** (e.g. `INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`, `WAKE_LOCK`): Automatically granted by Android at installation.
- **Critical Runtime Permissions** (Microphone, Phone, Contacts): Checked on startup with educational explanations.
- **On-Demand Runtime Permissions** (Camera, Location, Storage, Calendar): Requested only when the user invokes the corresponding voice tool (e.g. *"Main kahan hoon"* triggers Location request, *"Barcode scan karo"* triggers Camera request).
- **Special System Access** (Accessibility, Notification Listener, Display Over Other Apps, Battery Optimization): Requires explicit user navigation to system settings with deep-links and clear instructions.

### 1.2. The 6-Step Permission Lifecycle
```
 ┌────────────────┐
 │ 1. CHECK       │ Is ContextCompat.checkSelfPermission() == GRANTED?
 └───────┬────────┘
         │ No
 ┌───────▼────────┐
 │ 2. EXPLAIN     │ Show educational dialog: why MYRA needs it & what features it unlocks
 └───────┬────────┘
         │ User taps "Allow Access"
 ┌───────▼────────┐
 │ 3. REQUEST     │ Call ActivityCompat.requestPermissions() or RoleManager
 └───────┬────────┘
         │ User accepts in official Android OS Dialog
 ┌───────▼────────┐
 │ 4. VERIFY      │ Validate in onRequestPermissionsResult()
 └───────┬────────┘
         │ Granted
 ┌───────▼────────┐
 │ 5. REMEMBER    │ Store in SharedPreferences so user is never spammed again
 └───────┬────────┘
         │
 ┌───────▼────────┐
 │ 6. CONTINUE    │ Seamlessly resume voice engine / tool action without requiring restart
 └────────────────┘
```

### 1.3. Graceful Degradation (Zero Crashes on Denial)
If any permission is denied:
- **Never Crash**: All guarded methods have safe checks (`isPermissionGranted()`).
- **Feature Breakdown**: MYRA informs the user which specific feature is disabled (e.g. *"Microphone access denied. You can still type commands in the chat bar"*).
- **One-Tap System Settings**: If permanently denied (`shouldShowRequestPermissionRationale == false`), a direct **"Open Settings"** button opens `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

---

## 2. Complete Permissions Matrix

| Category | Android Manifest Permission | Protection Level | Min/Max API | Target Feature & Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Microphone** | `android.permission.RECORD_AUDIO` | Dangerous / Runtime | API 23+ | Voice input, wake word detection, Gemini Live 24kHz bi-directional audio. |
| | `android.permission.MODIFY_AUDIO_SETTINGS` | Normal | All | Echo cancellation, hardware audio routing (Bluetooth vs Speaker). |
| **Notifications** | `android.permission.POST_NOTIFICATIONS` | Dangerous / Runtime | **API 33+ (Android 13)** | System reminder alerts, timer countdown alarms, background status cards. |
| **Calls & Contacts**| `android.permission.READ_CONTACTS` | Dangerous / Runtime | API 23+ | Looking up phone numbers by name (*"Papa ko call karo"*, *"Rahul ka number kya hai"*). |
| | `android.permission.CALL_PHONE` | Dangerous / Runtime | API 23+ | Direct phone dialing on voice request. |
| | `android.permission.READ_PHONE_STATE` | Dangerous / Runtime | API 23+ | Incoming call detection & caller announcement (*"Priya ka call aa raha hai"*). |
| | `android.permission.ANSWER_PHONE_CALLS` | Dangerous / Runtime | **API 26+ (Android 8)** | Voice-activated call pickup (*"Utha lo"*, *"Answer karo"*). |
| | `android.permission.SEND_SMS` | Dangerous / Runtime | API 23+ | Direct emergency SMS and text messaging on user voice command. |
| **Location** | `android.permission.ACCESS_FINE_LOCATION` | Dangerous / Runtime | API 23+ | Turn-by-turn navigation, parking spot memory, nearby emergency hospitals/ATMs. |
| | `android.permission.ACCESS_COARSE_LOCATION` | Dangerous / Runtime | API 23+ | City-level weather briefings and regional context. |
| **Camera & Vision** | `android.permission.CAMERA` | Dangerous / Runtime | API 23+ | Real-time optical sentry, document scanning, barcode reading via Gemini Vision. |
| | `android.permission.FLASHLIGHT` | Normal | All | Voice flashlight toggling (*"Torch on karo"*). |
| **Calendar** | `android.permission.READ_CALENDAR` | Dangerous / Runtime | API 23+ | Briefing upcoming Google Calendar appointments and schedule meetings. |
| | `android.permission.WRITE_CALENDAR` | Dangerous / Runtime | API 23+ | Adding events and reminders through natural speech. |
| **Files & Media** | `android.permission.READ_MEDIA_IMAGES` | Dangerous / Runtime | **API 33+** | Selecting, sharing, or deleting photos via voice command. |
| | `android.permission.READ_MEDIA_AUDIO` | Dangerous / Runtime | **API 33+** | Managing voice recordings and local audio tracks. |
| | `android.permission.READ_MEDIA_VIDEO` | Dangerous / Runtime | **API 33+** | Inspecting downloaded video clips. |
| | `android.permission.READ_EXTERNAL_STORAGE` | Dangerous / Runtime | **maxSdkVersion 32** | Legacy file access on Android 12 and below. |
| **Bluetooth** | `android.permission.BLUETOOTH_CONNECT` | Dangerous / Runtime | **API 31+ (Android 12)** | Routing voice input through Bluetooth headsets & car audio hands-free. |
| | `android.permission.BLUETOOTH` | Normal | **maxSdkVersion 30** | Legacy Bluetooth discovery on Android 11 and below. |

---

## 3. Special System Permissions & Access Setup

These permissions cannot be granted via standard runtime dialogs; Android requires the user to toggle them in designated System Settings screens:

### 3.1. Accessibility Service (`AccessibilityHelperService`)
- **Purpose**: Autonomous UI interactions, reading on-screen text when requested (*"Screen padh kar batao"*), and background multi-step tasks.
- **Verification API**:
  ```kotlin
  PermissionManager.isAccessibilityServiceEnabled(context, AccessibilityHelperService::class.java)
  ```
- **Action Intent**: `Settings.ACTION_ACCESSIBILITY_SETTINGS`
- **User Instruction**: *"Settings → Accessibility → Downloaded Apps → MYRA → Turn ON"*

### 3.2. Notification Listener Service
- **Purpose**: Reading incoming WhatsApp/Gmail alerts and reading one-time passcodes (**Explicit request only**: *"OTP batao"*).
- **Verification API**:
  ```kotlin
  PermissionManager.isNotificationListenerEnabled(context)
  ```
- **Action Intent**: `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`
- **User Instruction**: *"Device Settings → Device & App Notifications → MYRA → Allow Access"*

### 3.3. Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)
- **Purpose**: Floating pulsating particle Orb visualizer, incoming call overlay, and multi-app multitasking.
- **Verification API**:
  ```kotlin
  Settings.canDrawOverlays(context)
  ```
- **Action Intent**: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (`package:com.myra.assistant`)
- **User Instruction**: *"Allow display over other apps so MYRA's Orb can appear while using other apps."*

### 3.4. Battery Optimization Exemption
- **Purpose**: Prevents Android's Doze mode from killing MYRA's background wake-word engine and scheduled timers.
- **Verification API**:
  ```kotlin
  val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
  powerManager.isIgnoringBatteryOptimizations(packageName)
  ```
- **Action Intent**: `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

### 3.5. Default Digital Assistant Role
- **Purpose**: Launching MYRA via home button long-press or power button double-tap.
- **Verification API**:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
  }
  ```
- **Action Intent**: `roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)` (or `Settings.ACTION_VOICE_INPUT_SETTINGS`).

---

## 4. Android Version Compatibility Matrix

| Feature | Android 8–10 (API 26–29) | Android 11–12 (API 30–31) | Android 13–14 (API 33–34) | Android 15 (API 35+) |
| :--- | :--- | :--- | :--- | :--- |
| **Notifications** | Granted at install | Granted at install | `POST_NOTIFICATIONS` runtime dialog | `POST_NOTIFICATIONS` runtime dialog |
| **Media / Storage** | `READ_EXTERNAL_STORAGE` | Scoped storage / `MANAGE_EXTERNAL_STORAGE` (if needed) | Granular `READ_MEDIA_*` permissions | Granular Photo Picker + `READ_MEDIA_*` |
| **Bluetooth Audio**| `BLUETOOTH` & `BLUETOOTH_ADMIN` | `BLUETOOTH` | `BLUETOOTH_CONNECT` runtime dialog | `BLUETOOTH_CONNECT` runtime dialog |
| **Assistant Role** | `ACTION_VOICE_INPUT_SETTINGS` | `RoleManager.ROLE_ASSISTANT` | `RoleManager.ROLE_ASSISTANT` | `RoleManager.ROLE_ASSISTANT` |
| **Foreground Mic** | Standard Service | Foreground service type `microphone` | Foreground service type `microphone` | Foreground service type `microphone` required |

---

## 5. Security & Privacy Guarantees

1. **Explicit OTP Protection**: Even with Notification Listener access granted, MYRA **never** proactively speaks OTPs, PINs, or banking tokens aloud. They are only read when the user explicitly issues the verbal command: *"OTP batao"*.
2. **20-Second Call Reject Guard**: Ringing phone calls are guarded against accidental misheard drops. A call is only rejected if the user clearly spoke a rejection keyword (*"reject"*, *"kaat do"*) within the last 20 seconds.
3. **No Root / No Hidden Exploits**: MYRA does not use shell commands, hidden reflection APIs, or root privileges to grant permissions silently. All grants require transparent user confirmation.
4. **Persistent Remembrance**: Once a permission category is granted, MYRA stores the confirmation state in `SharedPreferences` (`myra_permissions_prefs`) to avoid repeatedly prompting the user on subsequent launches.

---
*MYRA Android Permission Architecture — Built for Google Gemini 2.5 Flash Native Audio*
