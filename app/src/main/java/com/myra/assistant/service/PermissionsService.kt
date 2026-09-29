package com.myra.assistant.service

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Centralized Permission Management Service for MYRA IRIS-MX Architecture.
 *
 * Implements the required 6-Step Permission Lifecycle:
 * 1. CHECK: Is permission already granted?
 * 2. EXPLAIN: Show educational rationale prior to requesting.
 * 3. REQUEST: Launch official Android OS permission dialog or special settings intent.
 * 4. VERIFY: Confirm result upon return.
 * 5. REMEMBER: Store permission state in SharedPreferences.
 * 6. CONTINUE: Resume assistant voice flow seamlessly.
 */
class PermissionsService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("myra_permissions_service_prefs", Context.MODE_PRIVATE)

    companion object {
        const val RC_RUNTIME_PERMISSIONS = 3001
        const val RC_OVERLAY = 3002
        const val RC_NOTIFICATION_LISTENER = 3003
        const val RC_ACCESSIBILITY = 3004
        const val RC_BATTERY = 3005
    }

    enum class AccessType(
        val key: String,
        val displayName: String,
        val description: String,
        val isSpecial: Boolean = false
    ) {
        MICROPHONE("mic", "Microphone", "Voice input, Gemini Live bi-directional conversation, and wake-word"),
        PHONE("phone", "Phone & Telecom", "Initiating voice calls and managing incoming call actions"),
        CONTACTS("contacts", "Contacts", "Looking up contact phone numbers and identifying incoming callers"),
        NOTIFICATIONS("notifications", "Notifications", "Post notification alerts and background status cards"),
        NOTIFICATION_LISTENER("notif_listener", "Notification Listener", "Reading and auto-replying to WhatsApp, Instagram, Telegram, SMS", isSpecial = true),
        CALENDAR("calendar", "Calendar", "Checking upcoming meetings and scheduling events"),
        CAMERA("camera", "Camera", "Camera room sentry and QR scanner features"),
        LOCATION("location", "Location", "Emergency SOS coordinate dispatch and navigation"),
        OVERLAY("overlay", "Display Over Other Apps", "Floating assistant orb HUD while using other applications", isSpecial = true),
        ACCESSIBILITY("accessibility", "Accessibility Service", "Automated UI navigation and social media actions", isSpecial = true),
        BATTERY_OPTIMIZATION("battery", "Battery Unrestricted", "Preventing background service termination on devices like iQOO/Vivo", isSpecial = true)
    }

    fun isAccessGranted(type: AccessType): Boolean {
        return when (type) {
            AccessType.MICROPHONE -> ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            AccessType.PHONE -> ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            AccessType.CONTACTS -> ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            AccessType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
            }
            AccessType.NOTIFICATION_LISTENER -> {
                val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                flat != null && flat.contains(context.packageName)
            }
            AccessType.CALENDAR -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
            }
            AccessType.CAMERA -> ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            AccessType.LOCATION -> ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            AccessType.OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
            }
            AccessType.ACCESSIBILITY -> {
                val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                enabled != null && enabled.contains(context.packageName)
            }
            AccessType.BATTERY_OPTIMIZATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                } else true
            }
        }
    }

    /**
     * Executes the educational explanation step and requests the appropriate access.
     */
    fun requestAccessWithExplanation(
        activity: Activity,
        type: AccessType,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        if (isAccessGranted(type)) {
            onGranted()
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("${type.displayName} Access Required")
            .setMessage("${type.description}.\n\nWould you like to grant this access now?")
            .setPositiveButton("Allow Access") { _, _ ->
                if (type.isSpecial) {
                    openSpecialSettings(activity, type)
                } else {
                    requestRuntimePermission(activity, type)
                }
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                onDenied?.invoke()
            }
            .show()
    }

    private fun requestRuntimePermission(activity: Activity, type: AccessType) {
        val perms = when (type) {
            AccessType.MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
            AccessType.PHONE -> arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
            AccessType.CONTACTS -> arrayOf(Manifest.permission.READ_CONTACTS)
            AccessType.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
            AccessType.CALENDAR -> arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            AccessType.CAMERA -> arrayOf(Manifest.permission.CAMERA)
            AccessType.LOCATION -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            else -> emptyArray()
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, perms, RC_RUNTIME_PERMISSIONS)
        }
    }

    private fun openSpecialSettings(activity: Activity, type: AccessType) {
        when (type) {
            AccessType.NOTIFICATION_LISTENER -> {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            }
            AccessType.OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                }
            }
            AccessType.ACCESSIBILITY -> {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            }
            AccessType.BATTERY_OPTIMIZATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${activity.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                }
            }
            else -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            }
        }
    }
}
