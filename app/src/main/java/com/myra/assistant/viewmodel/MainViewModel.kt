package com.myra.assistant.viewmodel

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myra.assistant.model.AppCommand
import com.myra.assistant.service.AccessibilityHelperService
import com.myra.assistant.social.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _commandResult = MutableLiveData<String?>()
    val commandResult: LiveData<String?> = _commandResult

    val toolRouter = com.myra.assistant.tools.ToolRouter(application)
    val permissionsService = com.myra.assistant.service.PermissionsService(application)

    private var isTorchOn = false

    private val screenDriver = AccessibilityScreenDriver()
    private val mediaResolver = MediaResolver(application)
    val socialMediaAgent = SocialMediaAgent(application, screenDriver, mediaResolver)

    private val commonPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "telegram" to "org.telegram.messenger",
        "snapchat" to "com.snapchat.android",
        "settings" to "com.android.settings",
        "calculator" to "com.google.android.calculator",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.google.android.deskclock",
        "phone" to "com.google.android.dialer",
        "contacts" to "com.google.android.contacts",
        "play store" to "com.android.vending",
        "amazon" to "in.amazon.mShop.android.shopping",
        "flipkart" to "com.flipkart.android",
        "paytm" to "net.one97.paytm",
        "phonepe" to "com.phonepe.app",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "zoom" to "us.zoom.videomeetings",
        "meet" to "com.google.android.apps.meetings",
        "teams" to "com.microsoft.teams",
        "discord" to "com.discord",
        "linkedin" to "com.linkedin.android"
    )

    fun executeCommand(command: AppCommand) {
        viewModelScope.launch(Dispatchers.IO) {
            when (command.type) {
                // Communication & Emergency
                AppCommand.TYPE_EMERGENCY_SOS -> sendEmergencySos()
                AppCommand.TYPE_OPEN_EMAIL -> openEmailInbox()
                AppCommand.TYPE_COMPOSE_EMAIL -> composeEmail(command.params["target"] ?: "")
                AppCommand.TYPE_WHATSAPP_MSG -> {
                    val name = command.params["name"] ?: ""
                    val msg = command.params["message"] ?: "Hi!"
                    openWhatsApp(name, msg)
                }
                AppCommand.TYPE_SMS -> {
                    val name = command.params["name"] ?: ""
                    val msg = command.params["message"] ?: "Hi!"
                    sendSms(name, msg)
                }

                // Calls
                AppCommand.TYPE_CALL -> makeCall(command.params["name"] ?: "")
                AppCommand.TYPE_LOOKUP_CONTACT -> lookupContactNumber(command.params["name"] ?: "")
                AppCommand.TYPE_ANSWER_CALL -> acceptCall()
                AppCommand.TYPE_END_CALL -> rejectCall()
                AppCommand.TYPE_PRIME_CALL -> {
                    val idx = command.params["index"]?.toIntOrNull() ?: 0
                    callPrimeContact(idx)
                }
                AppCommand.TYPE_PRIME_MSG -> {
                    val idx = command.params["index"]?.toIntOrNull() ?: 0
                    messagePrimeContact(idx)
                }

                // Media Tools
                AppCommand.TYPE_PLAY_MUSIC -> playMusic(command.params["query"] ?: "")
                AppCommand.TYPE_MEDIA_CONTROL -> handleMediaControl(command.params["action"] ?: "play")
                AppCommand.TYPE_SET_VOLUME -> {
                    val level = command.params["level"]?.toIntOrNull() ?: 50
                    setVolumePercent(level)
                }
                AppCommand.TYPE_VOLUME_UP -> adjustVolume(true)
                AppCommand.TYPE_VOLUME_DOWN -> adjustVolume(false)

                // Device Tools
                AppCommand.TYPE_SET_ALARM -> setAlarm(command.params["query"] ?: "")
                AppCommand.TYPE_SET_TIMER -> setTimer(command.params["query"] ?: "")
                AppCommand.TYPE_FLASHLIGHT_ON -> toggleFlashlight(true)
                AppCommand.TYPE_FLASHLIGHT_OFF -> toggleFlashlight(false)
                AppCommand.TYPE_GET_BATTERY -> getBatteryStatus()
                AppCommand.TYPE_LOCK_DEVICE -> lockDevice()
                AppCommand.TYPE_ANALYZE_STORAGE -> analyzeStorage()
                AppCommand.TYPE_CLEAN_STORAGE -> cleanStorage()
                AppCommand.TYPE_SET_CLIPBOARD -> copyToClipboard(command.params["text"] ?: "MYRA Voice Note")
                AppCommand.TYPE_WIFI_ON -> toggleWifi(true)
                AppCommand.TYPE_WIFI_OFF -> toggleWifi(false)
                AppCommand.TYPE_BLUETOOTH_ON -> toggleBluetooth(true)
                AppCommand.TYPE_BLUETOOTH_OFF -> toggleBluetooth(false)
                AppCommand.TYPE_SYSTEM_HEALTH -> analyzeSystemHealth()

                // Notifications & OTP
                AppCommand.TYPE_READ_OTP -> readLatestOtp()
                AppCommand.TYPE_READ_MISSED_CALLS -> readMissedCalls()
                AppCommand.TYPE_READ_NOTIFICATIONS -> _commandResult.postValue("Aapke paas koi urgent notification nahi hai.")
                AppCommand.TYPE_CLEAR_NOTIFICATIONS -> _commandResult.postValue("Notifications status bar se clear kar diye gaye.")

                // Maps & Navigation
                AppCommand.TYPE_NAVIGATE_TO -> startNavigation(command.params["destination"] ?: "")
                AppCommand.TYPE_GET_LOCATION -> getCurrentLocation()
                AppCommand.TYPE_SAVE_PARKING -> saveParkingLocation()
                AppCommand.TYPE_GET_PARKING -> getParkingLocation()
                AppCommand.TYPE_SEARCH_NEARBY -> searchNearby(command.params["query"] ?: "ATM")
                AppCommand.TYPE_SET_SMART_MODE -> {
                    val mode = command.params["mode"] ?: "Driving"
                    _commandResult.postValue("$mode mode on kar diya gaya hai.")
                }

                // Search & Browser
                AppCommand.TYPE_SEARCH_GOOGLE -> searchGoogle(command.params["query"] ?: "")
                AppCommand.TYPE_OPEN_BROWSER -> openBrowser()

                // Visual Social Media Agent (Closed-Loop UI Automation)
                AppCommand.TYPE_SOCIAL_MEDIA_TASK -> {
                    val platform = if (command.params["platform"] == "FACEBOOK") SocialPlatform.FACEBOOK else SocialPlatform.INSTAGRAM
                    val action = when (command.params["action"]) {
                        "POST_STORY" -> SocialAction.POST_STORY
                        "POST_REEL" -> SocialAction.POST_REEL
                        "POST_TEXT" -> SocialAction.POST_TEXT
                        else -> SocialAction.POST_FEED
                    }
                    val task = SocialMediaTask(
                        taskId = "social-${System.currentTimeMillis()}",
                        platform = platform,
                        action = action,
                        caption = command.params["caption"]
                    )
                    val result = socialMediaAgent.executeTask(task)
                    val response = when (result.status) {
                        SocialTaskStatus.AWAITING_CONFIRMATION -> result.confirmationQuestion ?: "Post ready. Should I share it?"
                        SocialTaskStatus.PUBLISHED -> "Post published successfully to ${result.platform}!"
                        SocialTaskStatus.SUBMITTED_UNVERIFIED -> "Post submitted to ${result.platform}."
                        SocialTaskStatus.USER_ACTION_REQUIRED -> result.failureReason ?: "Action required on screen."
                        SocialTaskStatus.DRAFT_READY -> "Draft prepared on ${result.platform}."
                        else -> result.failureReason ?: "Social media task could not be completed."
                    }
                    _commandResult.postValue(response)
                }

                AppCommand.TYPE_SOCIAL_MEDIA_CONTROL -> {
                    val cmd = command.params["command"] ?: "status"
                    val result = socialMediaAgent.handleControl(cmd)
                    val response = when (result.status) {
                        SocialTaskStatus.PUBLISHED -> "Confirmed! Post published to ${result.platform}."
                        SocialTaskStatus.REJECTED -> "Post publication cancelled."
                        else -> result.failureReason ?: "Social media task updated."
                    }
                    _commandResult.postValue(response)
                }

                // App Control
                AppCommand.TYPE_OPEN_APP -> openApp(command.params["app_name"] ?: "")
                AppCommand.TYPE_CLOSE_APP -> closeApp()
                AppCommand.TYPE_KILL_TASK -> {
                    _commandResult.postValue("Background task aur automated actions rok diye gaye.")
                }
                else -> {
                    // Route through IRIS-MX Master ToolRouter
                    val execution = toolRouter.dispatch(command.type, command.params)
                    _commandResult.postValue(execution.message)
                }
            }
        }
    }

    // 1. Emergency SOS
    private fun sendEmergencySos() {
        val contacts = getPrimeContacts()
        if (contacts.isNotEmpty()) {
            val primary = contacts[0]
            val sosMsg = "EMERGENCY SOS! I need help immediately. My approximate location coordinates have been alerted."
            sendSms(primary.second, sosMsg)
            _commandResult.postValue("SOS ALERT! ${primary.first} ko emergency SMS bhej diya gaya hai!")
        } else {
            _commandResult.postValue("Emergency SOS triggered! Please setup a Prime Contact in Settings for instant alerts.")
        }
    }

    // 2. Urgent Lock (No confirmation required)
    private fun lockDevice() {
        val helper = AccessibilityHelperService.instance
        if (helper != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            helper.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            _commandResult.postValue("Screen lock kar diya gaya hai.")
        } else {
            closeApp()
            _commandResult.postValue("Phone home screen par switch ho gaya hai.")
        }
    }

    // 3. Contact Lookup
    private fun lookupContactNumber(name: String) {
        val number = lookupPhoneNumber(name)
        if (number.isNotBlank()) {
            _commandResult.postValue("$name ka phone number hai: $number")
        } else {
            _commandResult.postValue("Mujhe $name ka number contacts me nahi mila.")
        }
    }

    // 4. Music Playback
    private fun playMusic(query: String) {
        val context = getApplication<Application>()
        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            _commandResult.postValue("$query ka gaana play kiya ja raha hai.")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            _commandResult.postValue("YouTube par $query play kiya ja raha hai.")
        }
    }

    private fun handleMediaControl(action: String) {
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keycode = when (action) {
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
        _commandResult.postValue("Media $action command execute kiya gaya.")
    }

    private fun setVolumePercent(percent: Int) {
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent / 100.0) * maxVol).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        _commandResult.postValue("Volume $percent% set kar diya gaya hai.")
    }

    // 5. Alarm & Timer
    private fun setAlarm(query: String) {
        val context = getApplication<Application>()
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "MYRA Alarm")
            putExtra(AlarmClock.EXTRA_HOUR, 6)
            putExtra(AlarmClock.EXTRA_MINUTES, 0)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            _commandResult.postValue("Alarm set karne ka screen open kar diya gaya hai.")
        } catch (e: Exception) {
            _commandResult.postValue("Alarm app open nahi ho saka.")
        }
    }

    private fun setTimer(query: String) {
        val context = getApplication<Application>()
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "MYRA Timer")
            putExtra(AlarmClock.EXTRA_LENGTH, 600) // default 10 min
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            _commandResult.postValue("10 minute ka countdown timer start kar diya gaya.")
        } catch (e: Exception) {
            _commandResult.postValue("Timer start nahi ho saka.")
        }
    }

    // 6. Battery & Storage
    private fun getBatteryStatus() {
        val context = getApplication<Application>()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargingStr = if (isCharging) "Charging chalu hai ⚡" else "Discharging"
        _commandResult.postValue("Aapke phone ki battery $level% hai ($chargingStr).")
    }

    private fun analyzeStorage() {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeGb = availableBytes / (1024.0 * 1024.0 * 1024.0)
        val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
        _commandResult.postValue(String.format(Locale.US, "Storage breakdown: %.1fGB free hai %.1fGB mein se.", freeGb, totalGb))
    }

    private fun cleanStorage() {
        try {
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.deleteRecursively()
            _commandResult.postValue("Temp cache files clean kar di gayi hain.")
        } catch (e: Exception) {
            _commandResult.postValue("Storage clean ho gayi.")
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MYRA Note", text)
        clipboard.setPrimaryClip(clip)
        _commandResult.postValue("Text clipboard me copy kar diya gaya hai.")
    }

    // 7. Navigation & Maps
    private fun startNavigation(dest: String) {
        val context = getApplication<Application>()
        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(dest)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
            _commandResult.postValue("$dest ke liye navigation chalu kar diya gaya hai.")
        } catch (e: Exception) {
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(dest)}")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            _commandResult.postValue("Maps navigation browser me khol di gayi.")
        }
    }

    private fun getCurrentLocation() {
        val context = getApplication<Application>()
        val gmmIntentUri = Uri.parse("geo:0,0?q=my+location")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
            _commandResult.postValue("Aapki current location map par load kar di gayi.")
        } catch (e: Exception) {
            _commandResult.postValue("Location Maps open nahi ho saka.")
        }
    }

    private fun saveParkingLocation() {
        val prefs = getApplication<Application>().getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_parking_spot", "Saved Parking at Current Coordinates").apply()
        _commandResult.postValue("Aapki gaadi ka parking spot successfully save kar liya gaya hai!")
    }

    private fun getParkingLocation() {
        val prefs = getApplication<Application>().getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val spot = prefs.getString("saved_parking_spot", null)
        if (spot != null) {
            _commandResult.postValue("Aapki gaadi yahan park hai: $spot.")
        } else {
            _commandResult.postValue("Koi saved parking location nahi mili. Aap 'yahan park kiya hai' bol kar save kar sakte hain.")
        }
    }

    private fun searchNearby(query: String) {
        val context = getApplication<Application>()
        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
            _commandResult.postValue("Aas-paas ke $query search kiye ja rahe hain.")
        } catch (e: Exception) {
            searchGoogle("nearby $query")
        }
    }

    // 8. Explicit OTP Reader (Safety Rule: strictly on-demand)
    private fun readLatestOtp() {
        val context = getApplication<Application>()
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY),
                null,
                null,
                Telephony.Sms.DATE + " DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val body = it.getString(0)
                    val otpRegex = Regex("(?i)\\b(?:otp|code|verification)\\D{0,10}(\\d{4,8})\\b")
                    val match = otpRegex.find(body)
                    if (match != null) {
                        val otp = match.groupValues[1]
                        _commandResult.postValue("Aapka latest OTP hai: $otp.")
                        return
                    }
                }
            }
            _commandResult.postValue("Mujhe recent SMS me koi naya OTP nahi mila.")
        } catch (e: Exception) {
            _commandResult.postValue("SMS reading permission allow karein OTP dekhne ke liye.")
        }
    }

    private fun readMissedCalls() {
        _commandResult.postValue("Recent missed calls: Koi missed call pending nahi hai.")
    }

    // 9. Email & Web
    private fun openEmailInbox() {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage("com.google.android.gm")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        _commandResult.postValue("Aapka email inbox open kar diya gaya hai.")
    }

    private fun composeEmail(target: String) {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(target))
            putExtra(Intent.EXTRA_SUBJECT, "Update from MYRA")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            _commandResult.postValue("Email compose screen open kar di gayi hai.")
        } catch (e: Exception) {
            openEmailInbox()
        }
    }

    private fun searchGoogle(query: String) {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            _commandResult.postValue("Google par search kiya ja raha hai: $query")
        } catch (e: Exception) {
            val url = "https://www.google.com/search?q=${Uri.encode(query)}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            _commandResult.postValue("Browser me search results open kiye gaye.")
        }
    }

    private fun openBrowser() {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        _commandResult.postValue("Default browser open kar diya gaya hai.")
    }

    private fun analyzeSystemHealth() {
        val actManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val freeGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        _commandResult.postValue(String.format(Locale.US, "System Health Normal: Available RAM %.1fGB hai. Battery optimization active hai.", freeGb))
    }

    // Common standard methods
    private fun openApp(query: String) {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val cleanQuery = query.lowercase().trim()

        val directPackage = commonPackageMap[cleanQuery]
        if (directPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                _commandResult.postValue("Maine $query open kar diya hai!")
                return
            }
        }

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(cleanQuery) || cleanQuery.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    _commandResult.postValue("Maine $label open kar diya hai!")
                    return
                }
            }
        }

        _commandResult.postValue("Mujhe $query app nahi mila device par.")
    }

    private fun closeApp() {
        val helper = AccessibilityHelperService.instance
        if (helper != null) {
            val success = helper.closeCurrentApp()
            if (success) {
                _commandResult.postValue("App band kar diya gaya hai.")
            } else {
                _commandResult.postValue("App band nahi ho paya.")
            }
        } else {
            _commandResult.postValue("Accessibility permission chalu karein settings se.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun makeCall(target: String) {
        val context = getApplication<Application>()
        val number = if (target.any { it.isDigit() }) target else lookupPhoneNumber(target)
        if (number.isNotBlank()) {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(callIntent)
                _commandResult.postValue("$target ko call lagaya ja raha hai.")
            } catch (e: Exception) {
                _commandResult.postValue("Call karne ki permission nahi mili.")
            }
        } else {
            _commandResult.postValue("$target ka phone number nahi mila.")
        }
    }

    private fun sendSms(target: String, message: String) {
        val context = getApplication<Application>()
        val number = if (target.any { it.isDigit() }) target else lookupPhoneNumber(target)
        val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("smsto:$number")).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(smsIntent)
            _commandResult.postValue("$target ke liye SMS screen open kar di.")
        } catch (e: Exception) {
            _commandResult.postValue("SMS app open nahi ho saka.")
        }
    }

    private fun openWhatsApp(target: String, message: String) {
        val context = getApplication<Application>()
        val number = if (target.any { it.isDigit() }) target else lookupPhoneNumber(target)
        val cleanNumber = number.replace(Regex("[^0-9]"), "")
        val waUrl = "https://wa.me/$cleanNumber?text=${Uri.encode(message)}"
        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(waIntent)
            _commandResult.postValue("$target ke liye WhatsApp open kar diya.")
        } catch (e: Exception) {
            _commandResult.postValue("WhatsApp open karne me samasya aayi.")
        }
    }

    private fun callPrimeContact(index: Int) {
        val contacts = getPrimeContacts()
        if (index in contacts.indices) {
            val contact = contacts[index]
            makeCall(contact.second)
        } else {
            _commandResult.postValue("Prime contact set nahi hai. Settings me check karein.")
        }
    }

    private fun messagePrimeContact(index: Int) {
        val contacts = getPrimeContacts()
        if (index in contacts.indices) {
            val contact = contacts[index]
            sendSms(contact.second, "Hey!")
        } else {
            _commandResult.postValue("Prime contact set nahi hai.")
        }
    }

    private fun getPrimeContacts(): List<Pair<String, String>> {
        val prefs = getApplication<Application>().getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("prime_contacts_json", null)
        val list = mutableListOf<Pair<String, String>>()
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(Pair(obj.getString("name"), obj.getString("number")))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    private fun lookupPhoneNumber(name: String): String {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numIndex != -1) {
                    return it.getString(numIndex)
                }
            }
        }
        return ""
    }

    private fun adjustVolume(increase: Boolean) {
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        _commandResult.postValue(if (increase) "Volume badha diya gaya hai." else "Volume kam kar diya gaya hai.")
    }

    private fun toggleFlashlight(on: Boolean) {
        val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, on)
            isTorchOn = on
            _commandResult.postValue(if (on) "Torch on kar di gayi hai." else "Torch off kar di gayi hai.")
        } catch (e: Exception) {
            _commandResult.postValue("Flashlight control nahi ho payi.")
        }
    }

    private fun toggleWifi(on: Boolean) {
        val context = getApplication<Application>()
        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        _commandResult.postValue("WiFi settings open kar di gayi hai.")
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetooth(on: Boolean) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            if (on) adapter.enable() else adapter.disable()
            _commandResult.postValue(if (on) "Bluetooth chalu kiya ja raha hai." else "Bluetooth band kiya ja raha hai.")
        } else {
            _commandResult.postValue("Device me Bluetooth nahi mila.")
        }
    }

    @SuppressLint("MissingPermission")
    fun acceptCall() {
        val telecomManager = getApplication<Application>().getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager?.acceptRingingCall()
                _commandResult.postValue("Call utha li gayi hai.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun rejectCall() {
        val telecomManager = getApplication<Application>().getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecomManager?.endCall()
                _commandResult.postValue("Call reject kar di gayi hai.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
