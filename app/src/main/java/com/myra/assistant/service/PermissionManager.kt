package com.myra.assistant.service

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
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
 * Complete, secure Android permission manager for MYRA Assistant.
 *
 * Implements the official Android flow:
 * Check → Explain → Request → User Approves → Verify → Continue
 *
 * Strictly adheres to Google Play policies:
 * - No silent grants, no root exploits, no hidden bypasses
 * - Clear explanation prior to requesting
 * - Graceful degradation when permissions are denied
 * - Direct navigation to system settings with guided instructions
 */
object PermissionManager {

    private const val PREFS_NAME = "myra_permissions_prefs"
    const val REQUEST_CODE_RUNTIME_PERMISSIONS = 2001
    const val REQUEST_CODE_OVERLAY_PERMISSION = 2002
    const val REQUEST_CODE_BATTERY_OPTIMIZATION = 2003
    const val REQUEST_CODE_DEFAULT_ASSISTANT = 2004

    /**
     * Permission group definitions with user-friendly descriptions and affected features.
     */
    enum class PermissionCategory(
        val title: String,
        val description: String,
        val affectedFeatures: String,
        val permissions: Array<String>
    ) {
        MICROPHONE(
            "Microphone Access",
            "Required for voice commands, wake word detection, and real-time bi-directional conversation with Gemini Live.",
            "Voice input, live conversation, wake word detection",
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
        ),
        CALLS_AND_CONTACTS(
            "Calls & Contacts Access",
            "Enables MYRA to call saved contacts by name, announce incoming callers, and dial numbers on voice request.",
            "Voice calling ('Papa ko call karo'), contact lookup, caller name announcement",
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.SEND_SMS
            ).let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it + Manifest.permission.ANSWER_PHONE_CALLS
                } else it
            }
        ),
        NOTIFICATIONS(
            "Notification Posting",
            "Required to deliver background assistant reminders, timer alerts, and incoming message briefings.",
            "Reminders, timer alarms, background status alerts",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        ),
        LOCATION(
            "Location Services",
            "Used for turn-by-turn navigation, nearby essential services (ATMs, hospitals), and Emergency SOS location sharing.",
            "Turn-by-turn navigation, parking spot memory, nearby search, Emergency SOS",
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ),
        CAMERA(
            "Camera & Optical Vision",
            "Allows MYRA to scan documents, read barcodes, and act as a live sentry or visual assistant.",
            "Camera vision, barcode scanning, document reading, optical sentry mode",
            arrayOf(Manifest.permission.CAMERA)
        ),
        CALENDAR(
            "Calendar Access",
            "Enables MYRA to check your upcoming meetings and schedule voice reminders in your personal calendar.",
            "Upcoming schedule briefing, calendar event scheduling",
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        ),
        STORAGE_MEDIA(
            "Files & Media Access",
            "Required for managing saved notes, audio recordings, screenshots, and sharing files on request.",
            "File search, audio recording storage, photo sharing",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        ),
        BLUETOOTH(
            "Bluetooth & Nearby Devices",
            "Enables high-fidelity microphone input from Bluetooth headsets and hands-free vehicle audio.",
            "Bluetooth headset voice input, hands-free vehicle audio",
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                emptyArray()
            }
        )
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if a specific runtime permission is granted.
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if all permissions in a category are granted.
     */
    fun isCategoryGranted(context: Context, category: PermissionCategory): Boolean {
        return category.permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Returns list of missing permissions for a category.
     */
    fun getMissingPermissions(context: Context, category: PermissionCategory): List<String> {
        return category.permissions.filter { !isPermissionGranted(context, it) }
    }

    /**
     * Returns all critical missing permissions required for initial setup.
     */
    fun getCriticalMissingPermissions(context: Context): List<String> {
        val criticalCategories = listOf(
            PermissionCategory.MICROPHONE,
            PermissionCategory.CALLS_AND_CONTACTS,
            PermissionCategory.NOTIFICATIONS
        )
        return criticalCategories.flatMap { getMissingPermissions(context, it) }.distinct()
    }

    /**
     * Displays a clean, reassuring explanation dialog before requesting permissions.
     */
    fun showExplanationDialog(
        activity: Activity,
        category: PermissionCategory,
        onProceed: () -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        AlertDialog.Builder(activity)
            .setTitle(category.title)
            .setMessage("${category.description}\n\nFeatures enabled:\n• ${category.affectedFeatures}")
            .setPositiveButton("Allow Access") { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Requests runtime permissions via standard Android APIs.
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int = REQUEST_CODE_RUNTIME_PERMISSIONS) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Handles permission denial gracefully: explains what feature is affected and provides direct settings link.
     */
    fun handlePermissionDenial(
        activity: Activity,
        category: PermissionCategory,
        wasPermanentlyDenied: Boolean
    ) {
        val message = if (wasPermanentlyDenied) {
            "${category.title} was denied. The following features will be disabled:\n• ${category.affectedFeatures}\n\nYou can enable it anytime in Android App Settings."
        } else {
            "${category.title} permission is required to use:\n• ${category.affectedFeatures}\n\nMYRA will work with limited functionality without this."
        }

        val builder = AlertDialog.Builder(activity)
            .setTitle("Feature Limited")
            .setMessage(message)
            .setNegativeButton("OK", null)

        if (wasPermanentlyDenied) {
            builder.setPositiveButton("Open Settings") { _, _ ->
                openAppSettings(activity)
            }
        }

        builder.show()
    }

    /**
     * Opens Android System Settings for MYRA.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // =========================================================================
    // Special System Access Checks (Accessibility, Notification, Overlay, etc.)
    // =========================================================================

    /**
     * Checks if Display Over Other Apps (Overlay) is granted.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Opens system overlay permission settings screen.
     */
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    /**
     * Checks if Notification Listener Service is enabled for reading notifications/OTP.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }

    /**
     * Opens Notification Listener Settings screen.
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Checks if Accessibility Service is enabled for background screen automation.
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${serviceClass.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any { it.equals(expectedComponentName, ignoreCase = true) }
    }

    /**
     * Opens Accessibility Settings screen.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Checks if Battery Optimization is ignored for reliable background listening.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    /**
     * Requests Battery Optimization exemption.
     */
    fun requestIgnoreBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptimizationIgnored(activity)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                activity.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * Checks if MYRA is set as Default Digital Assistant (RoleManager on Android 10+).
     */
    fun isDefaultAssistant(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) ?: false
        } else {
            false
        }
    }

    /**
     * Requests Default Assistant Role.
     */
    fun requestDefaultAssistantRole(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                activity.startActivityForResult(intent, REQUEST_CODE_DEFAULT_ASSISTANT)
                return
            }
        }

        // Fallback for pre-Android 10
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }

    /**
     * Remembers whether initial permission walkthrough has completed to avoid repeated prompts.
     */
    fun markInitialCheckCompleted(context: Context) {
        getPrefs(context).edit().putBoolean("initial_permissions_checked", true).apply()
    }

    fun isInitialCheckCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean("initial_permissions_checked", false)
    }
}
