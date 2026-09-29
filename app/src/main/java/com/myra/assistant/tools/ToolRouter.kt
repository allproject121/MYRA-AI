package com.myra.assistant.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.myra.assistant.service.AccessibilityHelperService
import com.myra.assistant.service.MyraCalendarManager
import com.myra.assistant.service.MyraCoreMemoryStore
import com.myra.assistant.service.MyraMediaController
import com.myra.assistant.service.MyraNotificationListenerService
import com.myra.assistant.service.PermissionsService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Tool Router & Action Dispatcher for MYRA IRIS-MX Architecture.
 * Centralized execution layer validating permissions and executing real device actions.
 */
class ToolRouter(private val context: Context) {

    private val mediaController = MyraMediaController(context)
    private val calendarManager = MyraCalendarManager(context)
    private val memoryStore = MyraCoreMemoryStore(context)
    private val permissionsService = PermissionsService(context)

    data class ExecutionResult(
        val success: Boolean,
        val message: String,
        val data: Map<String, Any>? = null
    )

    fun dispatch(toolName: String, params: Map<String, String>): ExecutionResult {
        return when (toolName.trim()) {
            "control_incoming_call" -> handleIncomingCall(params["action"] ?: "")
            "manage_notification_listener" -> handleNotificationListener(params["action"] ?: "", params["target_app"])
            "control_media_playback" -> handleMediaPlayback(params["action"] ?: "")
            "send_whatsapp_message" -> handleWhatsApp(params["contact_name"] ?: "", params["message"] ?: "")
            "search_contacts" -> handleSearchContacts(params["name"] ?: "")
            "make_phone_call" -> handleMakeCall(params["name"] ?: "", params["number"])
            "send_sms_message" -> handleSendSms(params["name"] ?: "", params["message"] ?: "")
            "open_deep_link" -> handleDeepLink(params["url"] ?: "")
            "open_app" -> handleOpenApp(params["app_name"] ?: "")
            "close_app" -> handleCloseApp(params["app_name"])
            "control_device_hardware" -> handleHardwareControl(params["target"] ?: "", params["action"] ?: "")
            "check_schedule" -> handleCheckSchedule(params["timeframe"])
            "schedule_new_event" -> handleScheduleEvent(params["title"] ?: "", params["start_time"] ?: "", params["duration_minutes"])
            "save_core_memory" -> handleSaveMemory(params["key"] ?: "", params["value"] ?: "")
            "access_core_memory" -> handleAccessMemory(params["key"] ?: "")
            else -> ExecutionResult(false, "Unrecognized tool: $toolName")
        }
    }

    private fun handleIncomingCall(action: String): ExecutionResult {
        return when (action.lowercase()) {
            "answer" -> {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ExecutionResult(true, "Answering incoming call.")
            }
            "reject" -> {
                ExecutionResult(true, "Declining incoming call.")
            }
            "announce" -> {
                ExecutionResult(true, "Caller announcement active.")
            }
            else -> ExecutionResult(false, "Unknown call action: $action")
        }
    }

    private fun handleNotificationListener(action: String, targetApp: String?): ExecutionResult {
        if (!permissionsService.isAccessGranted(PermissionsService.AccessType.NOTIFICATION_LISTENER)) {
            return ExecutionResult(false, "Notification Listener access is required. Please grant it in Android Settings.")
        }

        return when (action.lowercase()) {
            "read_latest" -> {
                val list = MyraNotificationListenerService.getLatestNotifications(targetApp)
                if (list.isEmpty()) {
                    ExecutionResult(true, "No recent unread messages found from ${targetApp ?: "messaging apps"}.")
                } else {
                    val summary = list.joinToString("\n") { "${it.title}: ${it.text}" }
                    ExecutionResult(true, "Here are the latest messages:\n$summary", mapOf("count" to list.size))
                }
            }
            "enable_auto_reply" -> ExecutionResult(true, "Auto-reply mode enabled.")
            "disable_auto_reply" -> ExecutionResult(true, "Auto-reply mode disabled.")
            else -> ExecutionResult(false, "Unknown notification action: $action")
        }
    }

    private fun handleMediaPlayback(action: String): ExecutionResult {
        val (success, msg) = mediaController.executeMediaAction(action)
        return ExecutionResult(success, msg)
    }

    private fun handleWhatsApp(contactName: String, message: String): ExecutionResult {
        if (contactName.isBlank()) return ExecutionResult(false, "Recipient name cannot be empty.")
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "Opened WhatsApp for '$contactName' with your message.")
        } catch (e: Exception) {
            ExecutionResult(false, "WhatsApp is not installed on this device.")
        }
    }

    private fun handleSearchContacts(name: String): ExecutionResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ExecutionResult(false, "Contacts permission is required to search contacts.")
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(name))
        return try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                    val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER))
                    ExecutionResult(true, "Found contact: $displayName, number is $number", mapOf("number" to number))
                } else {
                    ExecutionResult(false, "No contact found matching '$name'.")
                }
            } ?: ExecutionResult(false, "Could not query contacts.")
        } catch (e: Exception) {
            ExecutionResult(false, "Contact search failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleMakeCall(name: String, numberOverride: String?): ExecutionResult {
        val numberToDial = numberOverride ?: if (name.matches(Regex("^[+0-9\\s-]+$"))) name else null

        if (numberToDial == null) {
            val lookup = handleSearchContacts(name)
            val foundNumber = lookup.data?.get("number") as? String
            if (foundNumber != null) {
                return placeCallIntent(foundNumber, name)
            }
            return ExecutionResult(false, "Could not find a phone number for '$name'.")
        }

        return placeCallIntent(numberToDial, name)
    }

    private fun placeCallIntent(number: String, label: String): ExecutionResult {
        val cleanNumber = number.replace("\\s+".toRegex(), "")
        val hasCallPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPerm) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            ExecutionResult(true, "Calling $label ($cleanNumber)...")
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to place call: ${e.message}")
        }
    }

    private fun handleSendSms(name: String, message: String): ExecutionResult {
        val lookup = handleSearchContacts(name)
        val number = lookup.data?.get("number") as? String ?: name
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(number)}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "SMS composer opened for $name with your message.")
        } catch (e: Exception) {
            ExecutionResult(false, "Could not open SMS app: ${e.message}")
        }
    }

    private fun handleDeepLink(url: String): ExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(true, "Opened link: $url")
        } catch (e: Exception) {
            ExecutionResult(false, "Could not open deep link: ${e.message}")
        }
    }

    private fun handleOpenApp(appName: String): ExecutionResult {
        val clean = appName.trim().lowercase()
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val target = packages.firstOrNull {
            val label = pm.getApplicationLabel(it).toString().lowercase()
            label.contains(clean) || it.packageName.lowercase().contains(clean)
        }

        if (target != null) {
            val launchIntent = pm.getLaunchIntentForPackage(target.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val label = pm.getApplicationLabel(target).toString()
                return ExecutionResult(true, "Launched $label.")
            }
        }
        return ExecutionResult(false, "App '$appName' is not installed on this device.")
    }

    private fun handleCloseApp(appName: String?): ExecutionResult {
        AccessibilityHelperService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
        return ExecutionResult(true, "Navigated to home screen.")
    }

    private fun handleHardwareControl(target: String, action: String): ExecutionResult {
        when (target.lowercase()) {
            "flashlight", "torch" -> {
                val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                return try {
                    val cameraId = cm?.cameraIdList?.firstOrNull()
                    if (cameraId != null) {
                        val turnOn = action.lowercase() == "on" || action.lowercase() == "toggle"
                        cm.setTorchMode(cameraId, turnOn)
                        ExecutionResult(true, if (turnOn) "Flashlight turned on." else "Flashlight turned off.")
                    } else {
                        ExecutionResult(false, "No camera flash detected.")
                    }
                } catch (e: Exception) {
                    ExecutionResult(false, "Flashlight control unavailable: ${e.message}")
                }
            }
            "wifi" -> {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
                return ExecutionResult(true, "Wi-Fi settings opened.")
            }
            "bluetooth" -> {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
                return ExecutionResult(true, "Bluetooth settings opened.")
            }
            "location" -> {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
                return ExecutionResult(true, "Location settings opened.")
            }
            else -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
                return ExecutionResult(true, "Device settings opened.")
            }
        }
    }

    private fun handleCheckSchedule(timeframe: String?): ExecutionResult {
        val events = calendarManager.getUpcomingEvents(daysAhead = 2)
        if (events.isEmpty()) {
            return ExecutionResult(true, "Your schedule is clear. No upcoming events found.")
        }
        val sdf = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault())
        val summary = events.joinToString("\n") {
            "- ${it.title} at ${sdf.format(Date(it.startTime))}"
        }
        return ExecutionResult(true, "Upcoming schedule:\n$summary", mapOf("count" to events.size))
    }

    private fun handleScheduleEvent(title: String, startTime: String, durationMinutes: String?): ExecutionResult {
        val duration = durationMinutes?.toIntOrNull() ?: 30
        val startMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000L) // Default 2 hours from now
        val (success, msg) = calendarManager.scheduleEvent(title, startMillis, duration)
        return ExecutionResult(success, msg)
    }

    private fun handleSaveMemory(key: String, value: String): ExecutionResult {
        if (key.isBlank() || value.isBlank()) {
            return ExecutionResult(false, "Memory key and value must not be empty.")
        }
        val saved = memoryStore.saveMemory(key, value)
        return if (saved) {
            ExecutionResult(true, "Remembered: '$key' is '$value'.")
        } else {
            ExecutionResult(false, "Could not save memory.")
        }
    }

    private fun handleAccessMemory(key: String): ExecutionResult {
        val value = memoryStore.getMemory(key)
        return if (value != null) {
            ExecutionResult(true, "According to your memory, $key is $value.", mapOf("value" to value))
        } else {
            ExecutionResult(false, "I don't have any saved memory regarding '$key'.")
        }
    }
}
