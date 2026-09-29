package com.myra.assistant.tools

import org.json.JSONArray
import org.json.JSONObject

/**
 * Master Tool Registry for MYRA IRIS-MX Architecture.
 * Declares all conceptual tools and parameters for Gemini Live and Action Dispatcher.
 */
object ToolRegistry {

    data class ToolParameter(
        val name: String,
        val type: String,
        val description: String,
        val required: Boolean = true,
        val enumValues: List<String>? = null
    )

    data class ToolDefinition(
        val name: String,
        val description: String,
        val parameters: List<ToolParameter>,
        val requiresConfirmation: Boolean = false
    )

    val ALL_TOOLS: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "control_incoming_call",
            description = "Manage incoming calls: answer ringing call, reject incoming call, or announce caller identity via voice.",
            parameters = listOf(
                ToolParameter("action", "string", "Call control action", required = true, enumValues = listOf("answer", "reject", "announce"))
            )
        ),
        ToolDefinition(
            name = "manage_notification_listener",
            description = "Inspect or reply to notifications from messaging apps (WhatsApp, Instagram, Telegram, SMS).",
            parameters = listOf(
                ToolParameter("action", "string", "Action to perform", required = true, enumValues = listOf("read_latest", "enable_auto_reply", "disable_auto_reply")),
                ToolParameter("target_app", "string", "Target messaging app filter", required = false, enumValues = listOf("whatsapp", "instagram", "telegram", "sms", "all"))
            )
        ),
        ToolDefinition(
            name = "control_media_playback",
            description = "Controls system-wide media playback (play, pause, toggle, next, previous track on Spotify, YouTube, etc.).",
            parameters = listOf(
                ToolParameter("action", "string", "Media command", required = true, enumValues = listOf("play", "pause", "toggle", "next", "previous"))
            )
        ),
        ToolDefinition(
            name = "send_whatsapp_message",
            description = "Send a WhatsApp message to a contact name or phone number.",
            parameters = listOf(
                ToolParameter("contact_name", "string", "Name or phone number of the recipient", required = true),
                ToolParameter("message", "string", "The message text to send", required = true)
            ),
            requiresConfirmation = true
        ),
        ToolDefinition(
            name = "search_contacts",
            description = "Search device contacts to look up a phone number without dialing.",
            parameters = listOf(
                ToolParameter("name", "string", "Name of the contact to find", required = true)
            )
        ),
        ToolDefinition(
            name = "make_phone_call",
            description = "Directly dial or place a phone call to a contact name or phone number.",
            parameters = listOf(
                ToolParameter("name", "string", "Contact name or phone number to call", required = true),
                ToolParameter("number", "string", "Optional phone number override", required = false)
            ),
            requiresConfirmation = true
        ),
        ToolDefinition(
            name = "send_sms_message",
            description = "Compose and send an SMS text message to a contact.",
            parameters = listOf(
                ToolParameter("name", "string", "Contact name or phone number", required = true),
                ToolParameter("message", "string", "Message body", required = true)
            ),
            requiresConfirmation = true
        ),
        ToolDefinition(
            name = "open_deep_link",
            description = "Opens a deep-link URI scheme (e.g. YouTube search, Spotify track, Google Maps navigation).",
            parameters = listOf(
                ToolParameter("url", "string", "Deep link URL or URI scheme", required = true),
                ToolParameter("target", "string", "Target app name or service", required = false)
            )
        ),
        ToolDefinition(
            name = "open_app",
            description = "Launch an installed application on the device by name.",
            parameters = listOf(
                ToolParameter("app_name", "string", "Name of the application to launch", required = true)
            )
        ),
        ToolDefinition(
            name = "close_app",
            description = "Simulate navigating back or closing the active application.",
            parameters = listOf(
                ToolParameter("app_name", "string", "Name of the app to dismiss", required = false)
            )
        ),
        ToolDefinition(
            name = "control_device_hardware",
            description = "Controls or opens system settings for hardware: flashlight, wifi, bluetooth, location, hotspot.",
            parameters = listOf(
                ToolParameter("target", "string", "Hardware subsystem", required = true, enumValues = listOf("flashlight", "wifi", "bluetooth", "location", "hotspot")),
                ToolParameter("action", "string", "Hardware action", required = true, enumValues = listOf("on", "off", "toggle", "open"))
            )
        ),
        ToolDefinition(
            name = "check_schedule",
            description = "Read upcoming calendar events and meetings from device calendar.",
            parameters = listOf(
                ToolParameter("timeframe", "string", "Timeframe to inspect (e.g. today, tomorrow, this week)", required = false)
            )
        ),
        ToolDefinition(
            name = "schedule_new_event",
            description = "Schedule a new event in the device calendar.",
            parameters = listOf(
                ToolParameter("title", "string", "Event title or meeting subject", required = true),
                ToolParameter("start_time", "string", "Start date and time (ISO or natural description)", required = true),
                ToolParameter("duration_minutes", "string", "Duration in minutes (default 30)", required = false)
            ),
            requiresConfirmation = true
        ),
        ToolDefinition(
            name = "save_core_memory",
            description = "Saves important personal context or memory for future recall (e.g. parking spot, preferences).",
            parameters = listOf(
                ToolParameter("key", "string", "Memory subject or key (e.g. parking_spot, wifi_password)", required = true),
                ToolParameter("value", "string", "Memory content to store", required = true)
            )
        ),
        ToolDefinition(
            name = "access_core_memory",
            description = "Retrieves previously saved memory or notes by key or topic.",
            parameters = listOf(
                ToolParameter("key", "string", "Subject or keyword to look up", required = true)
            )
        )
    )

    /**
     * Converts master tool definitions to Gemini Live / OpenAI Function Calling JSON Schema.
     */
    fun toGeminiFunctionDeclarations(): JSONArray {
        val array = JSONArray()
        for (tool in ALL_TOOLS) {
            val toolObj = JSONObject()
            toolObj.put("name", tool.name)
            toolObj.put("description", tool.description)

            val paramsObj = JSONObject()
            paramsObj.put("type", "object")

            val propertiesObj = JSONObject()
            val requiredArray = JSONArray()

            for (p in tool.parameters) {
                val prop = JSONObject()
                prop.put("type", p.type)
                prop.put("description", p.description)
                if (p.enumValues != null) {
                    val enumArray = JSONArray()
                    p.enumValues.forEach { enumArray.put(it) }
                    prop.put("enum", enumArray)
                }
                propertiesObj.put(p.name, prop)
                if (p.required) {
                    requiredArray.put(p.name)
                }
            }

            paramsObj.put("properties", propertiesObj)
            paramsObj.put("required", requiredArray)
            toolObj.put("parameters", paramsObj)

            array.put(toolObj)
        }
        return array
    }
}
