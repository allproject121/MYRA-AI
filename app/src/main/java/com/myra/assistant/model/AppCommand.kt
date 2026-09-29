package com.myra.assistant.model

data class AppCommand(
    val type: String,
    val params: Map<String, String> = emptyMap()
) {
    companion object {
        // Communication & SOS
        const val TYPE_OPEN_APP = "OPEN_APP"
        const val TYPE_CLOSE_APP = "CLOSE_APP"
        const val TYPE_CALL = "CALL"
        const val TYPE_LOOKUP_CONTACT = "LOOKUP_CONTACT"
        const val TYPE_ANSWER_CALL = "ANSWER_CALL"
        const val TYPE_END_CALL = "END_CALL"
        const val TYPE_SMS = "SMS"
        const val TYPE_WHATSAPP_MSG = "WHATSAPP_MSG"
        const val TYPE_OPEN_EMAIL = "OPEN_EMAIL"
        const val TYPE_COMPOSE_EMAIL = "COMPOSE_EMAIL"
        const val TYPE_EMERGENCY_SOS = "EMERGENCY_SOS"
        const val TYPE_PRIME_CALL = "PRIME_CALL"
        const val TYPE_PRIME_MSG = "PRIME_MSG"

        // Media Tools
        const val TYPE_PLAY_MUSIC = "PLAY_MUSIC"
        const val TYPE_MEDIA_CONTROL = "MEDIA_CONTROL"
        const val TYPE_SET_VOLUME = "SET_VOLUME"
        const val TYPE_VOLUME_UP = "VOLUME_UP"
        const val TYPE_VOLUME_DOWN = "VOLUME_DOWN"

        // Device Tools
        const val TYPE_SET_ALARM = "SET_ALARM"
        const val TYPE_SET_TIMER = "SET_TIMER"
        const val TYPE_FLASHLIGHT_ON = "FLASHLIGHT_ON"
        const val TYPE_FLASHLIGHT_OFF = "FLASHLIGHT_OFF"
        const val TYPE_GET_BATTERY = "GET_BATTERY"
        const val TYPE_OPEN_URL = "OPEN_URL"
        const val TYPE_SET_CLIPBOARD = "SET_CLIPBOARD"
        const val TYPE_LOCK_DEVICE = "LOCK_DEVICE"
        const val TYPE_ANALYZE_STORAGE = "ANALYZE_STORAGE"
        const val TYPE_CLEAN_STORAGE = "CLEAN_STORAGE"
        const val TYPE_WIFI_ON = "WIFI_ON"
        const val TYPE_WIFI_OFF = "WIFI_OFF"
        const val TYPE_BLUETOOTH_ON = "BLUETOOTH_ON"
        const val TYPE_BLUETOOTH_OFF = "BLUETOOTH_OFF"

        // Notifications & OTP
        const val TYPE_READ_NOTIFICATIONS = "READ_NOTIFICATIONS"
        const val TYPE_READ_MISSED_CALLS = "READ_MISSED_CALLS"
        const val TYPE_READ_OTP = "READ_OTP"
        const val TYPE_CLEAR_NOTIFICATIONS = "CLEAR_NOTIFICATIONS"

        // Maps & Navigation
        const val TYPE_NAVIGATE_TO = "NAVIGATE_TO"
        const val TYPE_GET_LOCATION = "GET_LOCATION"
        const val TYPE_SAVE_PARKING = "SAVE_PARKING"
        const val TYPE_GET_PARKING = "GET_PARKING"
        const val TYPE_SEARCH_NEARBY = "SEARCH_NEARBY"
        const val TYPE_SET_SMART_MODE = "SET_SMART_MODE"

        // Search & Browser
        const val TYPE_SEARCH_GOOGLE = "SEARCH_GOOGLE"
        const val TYPE_OPEN_BROWSER = "OPEN_BROWSER"

        // Screen Automation & Missions
        const val TYPE_KILL_TASK = "KILL_TASK"
        const val TYPE_START_MISSION = "START_MISSION"
        const val TYPE_SYSTEM_HEALTH = "SYSTEM_HEALTH"

        // Visual Social Media Agent
        const val TYPE_SOCIAL_MEDIA_TASK = "SOCIAL_MEDIA_TASK"
        const val TYPE_SOCIAL_MEDIA_CONTROL = "SOCIAL_MEDIA_CONTROL"
    }
}
