package com.myra.assistant.ai

import com.myra.assistant.model.AppCommand

object CommandParser {

    fun parse(text: String): AppCommand? {
        val clean = text.trim().lowercase()
        if (clean.isEmpty()) return null

        // 1. Emergency SOS
        if (clean.contains("sos") || clean.contains("khatre mein hoon") || clean.contains("madad chahiye") || clean.contains("bachao") || clean.contains("emergency")) {
            return AppCommand(AppCommand.TYPE_EMERGENCY_SOS)
        }

        // 2. Urgent Safety Lock (Instant, no confirmation required)
        if (clean == "lock kar do" || clean == "phone band kar do" || clean == "lock phone" || clean == "lock device" || clean.contains("screen lock karo")) {
            return AppCommand(AppCommand.TYPE_LOCK_DEVICE)
        }

        // 3. Kill Task / Stop Mission
        if (clean == "ruk jao" || clean.contains("task roko") || clean.contains("mission pause") || clean.contains("mission cancel") || clean.contains("stop task")) {
            return AppCommand(AppCommand.TYPE_KILL_TASK)
        }

        // 4. Call Handling (Answering & Rejecting with Safety Guard)
        if (clean == "utha lo" || clean == "answer karo" || clean == "call uthao" || clean == "haan utha lo" || clean == "accept call") {
            return AppCommand(AppCommand.TYPE_ANSWER_CALL)
        }
        if (clean == "call kaat do" || clean == "reject karo" || clean == "mat utha" || clean == "reject call" || clean == "cut the call") {
            return AppCommand(AppCommand.TYPE_END_CALL)
        }

        // 5. Contact Lookup (bina call kiye number batana)
        if (clean.contains("ka number kya hai") || clean.contains("ka phone number") || clean.contains("number batao")) {
            val name = extractTargetName(clean, listOf("ka number kya hai", "ka phone number", "number batao", "ka", "ki"))
            return AppCommand(AppCommand.TYPE_LOOKUP_CONTACT, mapOf("name" to name))
        }

        // 6. Prime Contact Shortcuts
        if (clean.contains("close friend") || clean.contains("best friend") || clean.contains("first contact")) {
            if (clean.contains("call") || clean.contains("phone")) {
                return AppCommand(AppCommand.TYPE_PRIME_CALL, mapOf("index" to "0"))
            }
            if (clean.contains("msg") || clean.contains("message") || clean.contains("bhejo") || clean.contains("karo")) {
                return AppCommand(AppCommand.TYPE_PRIME_MSG, mapOf("index" to "0"))
            }
        }

        if (clean.contains("meri jaan") || clean.contains("my love") || clean.contains("darling") || clean.contains("second contact")) {
            if (clean.contains("call") || clean.contains("phone")) {
                return AppCommand(AppCommand.TYPE_PRIME_CALL, mapOf("index" to "1"))
            }
            if (clean.contains("msg") || clean.contains("message") || clean.contains("bhejo") || clean.contains("karo")) {
                return AppCommand(AppCommand.TYPE_PRIME_MSG, mapOf("index" to "1"))
            }
        }

        // 7. Notification Tools & Explicit OTP Rule
        if (clean.contains("otp batao") || clean.contains("mera otp kya hai") || clean.contains("read otp")) {
            // Explicit OTP only
            return AppCommand(AppCommand.TYPE_READ_OTP)
        }
        if (clean.contains("missed calls batao") || clean.contains("missed call dikhao") || clean.contains("read missed calls")) {
            return AppCommand(AppCommand.TYPE_READ_MISSED_CALLS)
        }
        if (clean.contains("notifications padho") || clean.contains("read notifications") || clean.contains("kya notification aaya hai")) {
            return AppCommand(AppCommand.TYPE_READ_NOTIFICATIONS)
        }
        if (clean.contains("notifications clear kar do") || clean.contains("clear notifications") || clean.contains("saare notification hatao")) {
            return AppCommand(AppCommand.TYPE_CLEAR_NOTIFICATIONS)
        }

        // 8. Media & Music Tools
        if (clean.contains("gaana chalao") || clean.contains("play song") || clean.contains("play music") || clean.contains("song play karo")) {
            val query = extractTargetName(clean, listOf("gaana chalao", "play song", "play music", "song play karo", "ka", "ki"))
            return AppCommand(AppCommand.TYPE_PLAY_MUSIC, mapOf("query" to query))
        }
        if (clean.contains("agla gaana") || clean.contains("next song") || clean.contains("next track")) {
            return AppCommand(AppCommand.TYPE_MEDIA_CONTROL, mapOf("action" to "next"))
        }
        if (clean.contains("pause karo") || clean.contains("gaana roko") || clean.contains("pause music")) {
            return AppCommand(AppCommand.TYPE_MEDIA_CONTROL, mapOf("action" to "pause"))
        }
        if (clean.contains("resume karo") || clean.contains("play karo")) {
            return AppCommand(AppCommand.TYPE_MEDIA_CONTROL, mapOf("action" to "play"))
        }
        if (clean.contains("volume") && clean.contains("%")) {
            val level = clean.replace(Regex("[^0-9]"), "")
            return AppCommand(AppCommand.TYPE_SET_VOLUME, mapOf("level" to level))
        }
        if (clean.contains("volume badhao") || clean.contains("volume up") || clean.contains("aawaz badhao")) {
            return AppCommand(AppCommand.TYPE_VOLUME_UP)
        }
        if (clean.contains("volume kam karo") || clean.contains("volume down") || clean.contains("aawaz kam karo")) {
            return AppCommand(AppCommand.TYPE_VOLUME_DOWN)
        }

        // 9. Alarm & Timer
        if (clean.contains("alarm laga do") || clean.contains("alarm lagao") || clean.contains("alarm set karo")) {
            return AppCommand(AppCommand.TYPE_SET_ALARM, mapOf("query" to clean))
        }
        if (clean.contains("timer laga do") || clean.contains("timer lagao") || clean.contains("timer start karo")) {
            return AppCommand(AppCommand.TYPE_SET_TIMER, mapOf("query" to clean))
        }

        // 10. Device Tools (Battery, Storage, Torch, Clipboard, Wifi, Bluetooth)
        if (clean.contains("battery kitni hai") || clean.contains("battery percentage") || clean.contains("battery check karo")) {
            return AppCommand(AppCommand.TYPE_GET_BATTERY)
        }
        if (clean.contains("storage kitni bhari hai") || clean.contains("storage check karo")) {
            return AppCommand(AppCommand.TYPE_ANALYZE_STORAGE)
        }
        if (clean.contains("storage clean kar do") || clean.contains("cache clear karo")) {
            return AppCommand(AppCommand.TYPE_CLEAN_STORAGE)
        }
        if (clean.contains("ye copy kar lo") || clean.contains("copy karo")) {
            return AppCommand(AppCommand.TYPE_SET_CLIPBOARD)
        }
        if (clean.contains("torch on") || clean.contains("flashlight on") || clean.contains("torch chalu karo") || clean.contains("torch jalao")) {
            return AppCommand(AppCommand.TYPE_FLASHLIGHT_ON)
        }
        if (clean.contains("torch off") || clean.contains("flashlight off") || clean.contains("torch band karo") || clean.contains("torch bujhao")) {
            return AppCommand(AppCommand.TYPE_FLASHLIGHT_OFF)
        }
        if (clean.contains("wifi on") || clean.contains("wifi chalu karo")) {
            return AppCommand(AppCommand.TYPE_WIFI_ON)
        }
        if (clean.contains("wifi off") || clean.contains("wifi band karo")) {
            return AppCommand(AppCommand.TYPE_WIFI_OFF)
        }
        if (clean.contains("bluetooth on") || clean.contains("bluetooth chalu karo")) {
            return AppCommand(AppCommand.TYPE_BLUETOOTH_ON)
        }
        if (clean.contains("bluetooth off") || clean.contains("bluetooth band karo")) {
            return AppCommand(AppCommand.TYPE_BLUETOOTH_OFF)
        }
        if (clean.contains("phone slow kyun hai") || clean.contains("system health")) {
            return AppCommand(AppCommand.TYPE_SYSTEM_HEALTH)
        }

        // 11. Maps & Navigation
        if (clean.contains("ghar chalo") || clean.contains("navigate home")) {
            return AppCommand(AppCommand.TYPE_NAVIGATE_TO, mapOf("destination" to "Home"))
        }
        if (clean.contains("rasta dikhao") || clean.contains("navigate to") || clean.contains("direction dikhao")) {
            val dest = extractTargetName(clean, listOf("ka rasta dikhao", "rasta dikhao", "navigate to", "direction dikhao", "ko", "par"))
            return AppCommand(AppCommand.TYPE_NAVIGATE_TO, mapOf("destination" to dest))
        }
        if (clean.contains("main kahan hoon") || clean.contains("current location")) {
            return AppCommand(AppCommand.TYPE_GET_LOCATION)
        }
        if (clean.contains("yahan park kiya hai") || clean.contains("parking save karo") || clean.contains("save parking")) {
            return AppCommand(AppCommand.TYPE_SAVE_PARKING)
        }
        if (clean.contains("gaadi kahan park ki thi") || clean.contains("parking kahan hai") || clean.contains("find parking")) {
            return AppCommand(AppCommand.TYPE_GET_PARKING)
        }
        if (clean.contains("nearby ") || clean.contains("aas paas ")) {
            val query = extractTargetName(clean, listOf("nearby", "aas paas", "dikhao", "dhundo"))
            return AppCommand(AppCommand.TYPE_SEARCH_NEARBY, mapOf("query" to query))
        }
        if (clean.contains("driving mode") || clean.contains("sleep mode") || clean.contains("work mode")) {
            val mode = if (clean.contains("driving")) "Driving" else if (clean.contains("sleep")) "Sleep" else "Work"
            return AppCommand(AppCommand.TYPE_SET_SMART_MODE, mapOf("mode" to mode))
        }

        // 12. Communication: WhatsApp, SMS, Email
        if (clean.contains("whatsapp pe bolo") || clean.contains("whatsapp karo") || clean.contains("whatsapp message")) {
            val parts = clean.split("bolo", "ki", "message", limit = 2)
            val name = extractTargetName(parts[0], listOf("ko", "whatsapp", "pe", "karo", "message"))
            val msg = if (parts.size > 1) parts[1].trim() else "Hi!"
            return AppCommand(AppCommand.TYPE_WHATSAPP_MSG, mapOf("name" to name, "message" to msg))
        }

        if (clean.contains("sms karo") || clean.contains("sms bhejo") || clean.contains("message bhejo")) {
            val parts = clean.split("ki", "bhejo", limit = 2)
            val name = extractTargetName(parts[0], listOf("ko", "sms", "karo", "message", "bhejo", "to"))
            val msg = if (parts.size > 1) parts[1].trim() else "Hi!"
            return AppCommand(AppCommand.TYPE_SMS, mapOf("name" to name, "message" to msg))
        }

        if (clean.contains("mera inbox kholo") || clean.contains("email inbox kholo") || clean.contains("check mail")) {
            return AppCommand(AppCommand.TYPE_OPEN_EMAIL)
        }
        if (clean.contains("email likho") || clean.contains("compose email")) {
            val target = extractTargetName(clean, listOf("email likho", "compose email", "ko", "to"))
            return AppCommand(AppCommand.TYPE_COMPOSE_EMAIL, mapOf("target" to target))
        }

        // 13. Search & Browser
        if (clean.contains("google pe search karo") || clean.contains("google search")) {
            val query = extractTargetName(clean, listOf("google pe search karo", "google search", "search"))
            return AppCommand(AppCommand.TYPE_SEARCH_GOOGLE, mapOf("query" to query))
        }
        if (clean == "browser kholo" || clean == "open browser") {
            return AppCommand(AppCommand.TYPE_OPEN_BROWSER)
        }

        // WhatsApp Command Variations
        if (clean.contains("whatsapp kholo") || clean.contains("open whatsapp") || clean.contains("whatsapp open karo") || clean.contains("whatsapp chalao") || clean.contains("open my whatsapp") || clean.contains("can you open whatsapp") || clean == "whatsapp") {
            return AppCommand(AppCommand.TYPE_OPEN_APP, mapOf("app_name" to "whatsapp"))
        }

        // Direct phone number dialing
        val directPhoneMatch = Regex("(?:call|dial|phone lagao|phone karo)\\s+([+]?[0-9]{7,14})").find(clean)
        if (directPhoneMatch != null) {
            val num = directPhoneMatch.groupValues[1]
            return AppCommand(AppCommand.TYPE_CALL_PHONE, mapOf("phone_number" to num))
        }

        // 14. Visual Social Media Agent (Closed-Loop Automation)
        if (clean.contains("instagram") || clean.contains("insta")) {
            val isStory = clean.contains("story")
            val isReel = clean.contains("reel")
            val action = if (isStory) "POST_STORY" else if (isReel) "POST_REEL" else "POST_FEED"
            val caption = clean.substringAfter("caption", "").trim()
            return AppCommand(
                AppCommand.TYPE_SOCIAL_MEDIA_TASK,
                mapOf(
                    "platform" to "INSTAGRAM",
                    "action" to action,
                    "caption" to caption
                )
            )
        }

        if (clean.contains("facebook") || clean.contains("fb")) {
            val isMedia = clean.contains("photo") || clean.contains("video") || clean.contains("image")
            val action = if (isMedia) "POST_FEED" else "POST_TEXT"
            val caption = clean.substringAfter("post", "").replace("karo", "").trim()
            return AppCommand(
                AppCommand.TYPE_SOCIAL_MEDIA_TASK,
                mapOf(
                    "platform" to "FACEBOOK",
                    "action" to action,
                    "caption" to caption
                )
            )
        }

        if (clean == "confirm post" || clean == "yes share it" || clean == "haan post kar do" || clean == "share it" || clean == "post kar do") {
            return AppCommand(AppCommand.TYPE_SOCIAL_MEDIA_CONTROL, mapOf("command" to "confirm"))
        }

        if (clean == "reject post" || clean == "cancel post" || clean == "mat post karo" || clean == "discard post") {
            return AppCommand(AppCommand.TYPE_SOCIAL_MEDIA_CONTROL, mapOf("command" to "reject"))
        }

        // 15. Phone Calls
        if (clean.contains("call karo") || clean.contains("call lagao") || clean.contains("phone karo") || clean.startsWith("call ")) {
            val name = extractTargetName(clean, listOf("call karo", "call lagao", "phone karo", "call", "ko", "to"))
            if (name.isNotEmpty()) {
                return AppCommand(AppCommand.TYPE_CALL, mapOf("name" to name))
            }
        }

        // 15. App Closing & Opening
        if (clean.contains("band karo") || clean.contains("close app") || clean.contains("back jao") || clean.contains("home screen")) {
            return AppCommand(AppCommand.TYPE_CLOSE_APP)
        }
        if (clean.contains("kholo") || clean.contains("open ") || clean.contains("launch ") || clean.contains("start ")) {
            val app = extractTargetName(clean, listOf("kholo", "open", "launch", "start", "app"))
            if (app.isNotEmpty()) {
                return AppCommand(AppCommand.TYPE_OPEN_APP, mapOf("app_name" to app))
            }
        }

        // 16. Core Memory (Remember & Recall)
        if (clean.startsWith("remember ") || clean.startsWith("yaad rakhna ") || clean.startsWith("note down ")) {
            val content = clean.removePrefix("remember ").removePrefix("yaad rakhna ").removePrefix("note down ").trim()
            val parts = if (content.contains(" is ")) content.split(" is ", limit = 2)
            else if (content.contains(" hai ")) content.split(" hai ", limit = 2)
            else listOf("note", content)
            return AppCommand("save_core_memory", mapOf("key" to parts[0].trim(), "value" to (parts.getOrNull(1) ?: parts[0]).trim()))
        }
        if (clean.startsWith("what is my ") || clean.startsWith("mera ") && clean.endsWith(" kya hai")) {
            val key = clean.removePrefix("what is my ").removePrefix("mera ").removeSuffix(" kya hai").trim()
            return AppCommand("access_core_memory", mapOf("key" to key))
        }

        // 17. Calendar & Scheduling
        if (clean.contains("calendar") || clean.contains("schedule check") || clean.contains("aaj ka schedule") || clean.contains("what's on my calendar")) {
            return AppCommand("check_schedule", mapOf("timeframe" to "today"))
        }
        if (clean.contains("schedule meeting") || clean.contains("meeting schedule") || clean.contains("schedule a meeting") || clean.contains("schedule event")) {
            val title = extractTargetName(clean, listOf("schedule", "meeting", "titled", "event", "karo"))
            return AppCommand("schedule_new_event", mapOf("title" to if (title.isNotBlank()) title else "Team Sync", "start_time" to "2 hours from now", "duration_minutes" to "30"))
        }

        // 18. Media Playback Controls
        if (clean == "pause music" || clean == "pause" || clean == "music roko" || clean == "stop music") {
            return AppCommand("control_media_playback", mapOf("action" to "pause"))
        }
        if (clean == "resume music" || clean == "play music" || clean == "chalao" || clean == "play") {
            return AppCommand("control_media_playback", mapOf("action" to "play"))
        }
        if (clean == "next song" || clean == "skip song" || clean == "agla gana" || clean == "next") {
            return AppCommand("control_media_playback", mapOf("action" to "next"))
        }
        if (clean == "previous song" || clean == "pichhla gana" || clean == "previous") {
            return AppCommand("control_media_playback", mapOf("action" to "previous"))
        }

        // 19. Notifications Management
        if (clean.contains("read notifications") || clean.contains("read latest") || clean.contains("notification padho") || clean.contains("messages check karo")) {
            val app = if (clean.contains("whatsapp")) "whatsapp" else if (clean.contains("instagram")) "instagram" else if (clean.contains("telegram")) "telegram" else "all"
            return AppCommand("manage_notification_listener", mapOf("action" to "read_latest", "target_app" to app))
        }

        return null
    }

    private fun extractTargetName(input: String, stopWords: List<String>): String {
        var result = input
        for (word in stopWords) {
            result = result.replace(word, " ")
        }
        return result.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
    }
}
