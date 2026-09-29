# 🤖 MYRA — Complete Tools & Features Guide (2026 EDITION)

> **Har voice tool, har command, har setting — ek jagah.**  
> Har tool ke saath: kya karta hai, konsa command bolna hai, aur kaise use karna hai.

---

## Table of Contents
1. [Communication Tools](#1-communication-tools)
2. [Call Tools](#2-call-tools)
3. [Media Tools](#3-media-tools)
4. [Device Tools](#4-device-tools)
5. [Files & Photos](#5-files--photos-tools)
6. [Smart / Generative Tools](#6-smart--generative-tools)
7. [Missions (Bade Multi-Step Goals)](#7-missions-bade-multi-step-goals)
8. [Notification Tools](#8-notification-tools)
9. [Maps & "My World"](#9-maps--my-world-tools)
10. [PC Connect](#10-pc-connect-tools-myra-companion)
11. [Search, Browser & Apps](#11-search-browser--app-launch-tools)
12. [Screen Automation (Background Tasks)](#12-screen-automation-tools-background-agent)
13. [Connector Tools](#13-connector-tools-third-party-integrations)
14. [App Settings & Screens](#14-app-settings--screens-manual-use)
15. [Automation & Triggers](#15-automation--triggers-agar-x-ho-to-y-karo)
16. [Safety Rules — MYRA Kya Kabhi Nahi Karega](#16-safety-rules--myra-kya-kabhi-nahi-karega)

---

## 1. Communication Tools

| Tool | Kya Karta Hai | Bolne Wala Command (Example) |
| :--- | :--- | :--- |
| `send_whatsapp` | Contact ka WhatsApp chat kholta hai, message likhta hai, Send dabata hai aur confirm karta hai. | *"Rahul ko WhatsApp pe bolo main late hoon"* |
| `send_sms` | Kisi contact ya number ko seedha SMS bhejta hai. | *"Mummy ko SMS karo ki pahuch gaya"* |
| `open_email_inbox` | Aapka signed-in Gmail/Outlook inbox kholta hai. | *"Mera inbox kholo"* |
| `compose_email` | Email likhne ka screen kholta hai, To/Subject/Body bhar deta hai — Send aap khud dabate ho. | *"Boss ko email likho meeting reschedule ho gayi"* |
| `send_emergency_alert` | Aapke trusted contact ko location ke saath SOS alert bhejta hai — sirf emergency ke liye. | *"SOS"*, *"Main khatre mein hoon"*, *"Madad chahiye"* |

---

## 2. Call Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `call_contact` | Saved contact ya number pe call lagata hai. | *"Papa ko call karo"* |
| `lookup_contact` | Bina call/message kiye kisi contact ka number bata deta hai. | *"Rahul ka number kya hai"* |
| `answer_call` | Aati hui call utha leta hai. | *"Utha lo"*, *"Answer karo"*, *"Haan"* |
| `end_call` | **SAFETY GUARD** Call kaatta/reject karta hai. Ringing call sirf tabhi kaatega jab aapne 20 second ke andar clearly "reject/kaat do/mat utha" bola ho — galti se misheard word pe hang up nahi karega. | *"Call kaat do"*, *"Reject karo"* |

---

## 3. Media Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `play_music` | Song/album/artist search karke play karta hai (YouTube Music/Spotify pe auto-play; plain search results). | *"Arijit Singh ka gaana chalao"*, *"Shape of You play karo"* |
| `media_control` | Jo bhi chal raha hai usko play/pause/next/previous/stop karta hai — kisi bhi app mein. | *"Agla gaana"*, *"Pause karo"*, *"Previous track"* |
| `set_volume` | Media volume ko % mein set karta hai ya step increase/decrease karta hai. | *"Volume 50% kar do"*, *"Volume badha do"* |

---

## 4. Device Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `set_alarm` | Alarm laga deta hai (24-hr clock), label ke saath optional. | *"Subah 6 baje alarm laga do"* |
| `set_timer` | Countdown timer start karta hai. | *"10 minute ka timer laga do"* |
| `toggle_flashlight` | Torch on/off karta hai. | *"Torch on karo"*, *"Flashlight band karo"* |
| `get_battery` | Battery level, charging status, power-save status batata hai. | *"Battery kitni hai"* |
| `open_url` | Diya gaya web address browser mein kholta hai. | *"xyz.com kholo"*, *"github.com open karo"* |
| `set_clipboard` | Text ko clipboard mein copy karta hai. | *"Ye copy kar lo"* |
| `lock_device` | **NO CONFIRM** Turant screen lock kar deta hai — urgent safety command, bina poochhe execute hota hai. | *"Lock kar do"*, *"Phone band kar do"* |
| `analyze_storage` | Cache/temp files check karke cleanup breakdown batata hai — kuch delete nahi karta. | *"Storage kitni bhari hai"* |
| `clean_storage` | App cache/temp files clear karta hai (aapki confirmation ke baad). | *"Storage clean kar do"* |

---

## 5. Files & Photos Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `list_files` | Agent workspace ki files list karta hai. | *"Files dikhao"* |
| `search_files` | Naam/type/date se files dhundta hai. | *"Wo PDF dhundo jo kal banayi thi"* |
| `delete_file` | Ek naamed file delete karta hai. | *"Ye file delete kar do"* |
| `share_file` | Agent-workspace file share sheet se ya seedha named app pe bhejta hai (e.g. WhatsApp). | *"Ye report Rahul ko WhatsApp pe bhej do"* |
| `delete_photo` | Ek ya zyada photo delete karta hai, kisi folder se (e.g. WhatsApp) — default: sabse recent photo. | *"Last 3 photo delete kar do"* |
| `file_operation` | Zip / unzip / copy / move files. | *"Is folder ko zip kar do"* |
| `open_file` | File ko sahi system app se kholta hai. | *"Ye file kholo"* |
| `format_code` | Code file ki basic syntax auto-fix/format karta hai. | *"Ye code format kar do"* |
| `manage_file` | Device storage mein kahin bhi file ko rename/copy/move/delete karta hai — file manager kholne ki zaroorat nahi. | *"Is file ka naam change kar do"* |
| `manage_folder` | Device storage mein folder create/delete/rename/list/open karta hai. | *"Naya folder banao 'Office'"* |
| `share_file_to_app` | Storage file ko WhatsApp/Telegram/Gmail etc. pe prefilled share sheet se bhejta hai. | *"Ye photo Telegram pe share karo"* |
| `take_photo` | Bina camera UI dikhaye chup-chaap photo kheenchta hai (front/back) aur gallery mein save karta hai. | *"Ek photo le lo"* |
| `camera_vision` | Camera viewfinder khol kar real duniya dekhta hai — barcode scan, document padhna. | *"Ye barcode scan karo"* |

---

## 6. Smart / Generative Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `generate_project` | Description se poora website/coding project bana kar browser mein khol deta hai. | *"Ek portfolio website bana do"* |
| `generate_image` | Image banata hai (AI generation prompt). | *"Ek sunset ki image banao"* |
| `deep_research` | Web research karke summary deta hai (Tavily search se). | *"Electric cars pe research karo"* |
| `game_coach` | Real-time gaming coach on/off karta hai — game khelte waqt tactical advice deta hai. | *"Game coach on kar do"* |
| `system_health` | Phone slow/hot/battery drain kyun hai — RAM, thermal, sabse zyada battery khane wali app diagnose karta hai. | *"Phone slow kyun hai"* |
| `open_app_settings` | Kisi app ki system settings kholta hai (force-stop/permission change ke liye). | *used automatically after system_health* |
| `read_captured` | Aapne kisi doosri app se Share menu ya text-select karke jo bheja tha, wo wapas padhta hai. | *"Jo maine abhi capture kiya usko summarise karo"* |

---

## 7. Missions (Bade Multi-Step Goals)

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `start_mission` | Complex, multi-step goal shuru karta hai jisme planning + autonomous execution chahiye. Mission Dashboard khul jaata hai jahan progress dikhta hai. | *"Plan my trip to Delhi"*, *"Meri subah ki routine automate kar do"* |
| `pause_mission` / `resume_mission` / `cancel_mission` | Chal rahe mission ko rokna/wapas shuru karna/cancel karna. | *"Mission pause karo"*, *"Mission cancel kar do"* |

---

## 8. Notification Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `read_notifications` | Recent important notifications (WhatsApp, Telegram, Gmail...) padh kar sunata hai. | *"Notifications padho"* |
| `reply_to_notification` | Kisi notification ka seedha reply bhej deta hai — app kholne ki zaroorat nahi. | *"Rahul ko reply karo 'okay'"* |
| `read_missed_calls` | Recent missed calls padh kar sunata hai. | *"Missed calls batao"* |
| `read_otp` | **EXPLICIT ONLY** Latest OTP dhund kar padhta hai — kabhi khud se automatically nahi bolta, sirf poochne pe. | *"OTP batao"* |
| `clear_notifications` | Status bar ke saare active notifications clear kar deta hai. | *"Notifications clear kar do"* |

*Notification settings on/off karne ke liye: Settings → Notification Intelligence mein jaakar Read WhatsApp/Telegram/Gmail, Auto Reply, Spam Filter, Priority Filter, Driving/Sleep/Work/Game Mode toggle karo.*

---

## 9. Maps & "My World" Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `open_map` | MYRA ka Personal AI Map UI kholta hai. | *"Map kholo"* |
| `navigate_to` | Google Maps mein turn-by-turn navigation shuru karta hai. | *"Airport ka rasta dikhao"* |
| `navigate_to_place` | Saved favorite place (Home/Office) tak navigation. | *"Ghar chalo"* |
| `get_location` | Current address + coordinates batata hai. | *"Main kahan hoon"* |
| `get_distance` | Kisi saved favorite place ki distance batata hai. | *"Office kitni door hai"* |
| `get_parking_location` | Aapne gaadi kahan park ki thi, wo bataata hai. | *"Gaadi kahan park ki thi"* |
| `save_parking` | Current location ko parking spot ke roop mein save karta hai. | *"Yahan park kiya hai, yaad rakho"* |
| `search_nearby` | Aas-paas ATM, hospital, petrol pump waghera dhundta hai. | *"Nearby ATM dikhao"* |
| `set_smart_mode` | Driving, Sleep, Work, ya Game mode on/off karta hai. | *"Driving mode on kar do"* |

*Maps settings: Map Settings screen mein Location/Weather/Voice Navigation/Parking Memory toggle hote hain. (Note: Real-time traffic data & offline map tiles download currently stub).*

---

## 10. PC Connect Tools (MYRA Companion)

| Tool | Kya Karta Hai | Command / Setup |
| :--- | :--- | :--- |
| `pc_connect` | MYRA Companion app chal rahe PC se IP + PIN daal kar connect karta hai. | *Settings → PC Connect mein IP address aur PIN daalo* |
| `pc_command` | Connected PC ko control command bhejta hai. | *"PC pe ye kar do..."*, *"PC pe Chrome kholo"* |
| `send_file_to_pc` | Phone se PC pe file bhejta hai. | *"Ye file PC pe bhej do"* |

---

## 11. Search, Browser & App-Launch Tools

| Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- |
| `search_google` | Web search karke results dikhata hai. | *"Google pe search karo aaj ka mausam"* |
| `open_browser` | Default browser kisi URL ya homepage pe kholta hai. | *"Browser kholo"* |
| `browser_search` | Default browser mein query search karta hai. | *"Browser mein search karo..."* |
| `browser_navigation` | Standard browser navigation (Back, Refresh, New Tab, Close Tab). | *"Peeche jao"*, *"New tab kholo"* |
| `open_app` | Koi bhi installed app naam se seedha khol deta hai. | *"WhatsApp kholo"*, *"Instagram open karo"* |
| `launch_intent` | Generic deep-link mechanism se named Android intent chalata hai. | *internal use* |

---

## 12. Screen Automation Tools (Background Agent)

Jab kisi task ke liye seedha koi tool nahi hota (jaise *"Zomato se pizza order karo"*, *"Ola book karo"*), MYRA background mein khud phone chala kar screen tap karta hai via Accessibility Service.

| Tool | Kya Karta Hai |
| :--- | :--- |
| `start_task` | Last resort — complex multi-step kaam ke liye background automation agent ko handoff karta hai. |
| `read_screen` | Accessibility tree se current screen padhta hai — har element ka numeric ID, foreground app, keyboard state deta hai. |
| `visual_check` | Screenshot lekar visually confirm karta hai (e.g. gaana chal raha hai ya nahi). |
| `tap_element` / `long_press_element` | Numeric ID wale element ko tap/long-press karta hai. |
| `tap_point` | Raw x,y coordinate pe tap — sirf last resort. |
| `find_element` | Visible text se element ka numeric ID dhundta hai. |
| `type` / `clear_input_text` | Focused field mein text type karta hai ya clear karta hai. |
| `scroll_down` / `scroll_up` / `scroll_until` | Screen scroll karta hai jab tak target text na dikhe. |
| `press_enter` | Focused field submit karta hai (search/send/go). |
| `wait` / `wait_for_screen` | Screen settle hone ka ya specific text aane ka wait karta hai. |
| `switch_app` / `back` / `home` | App switcher dikhana, OS back/home navigation. |
| `kill_task` | Chal raha background task turant rok deta hai. Command: *"Ruk jao"* |

---

## 13. Connector Tools (Third-Party Integrations)

*Setup Zaroori: Settings → Connectors mein jaakar service authorize karo.*

| Connector | Tool | Kya Karta Hai | Command |
| :--- | :--- | :--- | :--- |
| **Google** | `google_list_upcoming_events` | Google Calendar ke agle events list karta hai. | *"Mere upcoming events dikhao"* |
| **Google Drive** | `google_drive_list_recent_files` | Drive ki recent files list karta hai. | *"Drive ki recent files dikhao"* |
| **GitHub** | `github_list_repositories` | Recently updated GitHub repos list karta hai. | *"Mere GitHub repos dikhao"* |
| **Canva** | `canva_list_designs` | Recent Canva designs list karta hai. | *"Canva designs dikhao"* |

*Backend Power Connectors: Groq (chat speed), OpenAI/Claude/Perplexity (multi-LLM), Tavily (deep research), ElevenLabs (custom speech voices), Telegram bot, Home Assistant.*

---

## 14. App Settings & Screens (Manual Use)

1. **Voice & AI Models:** TTS voice choose karna (Google TTS / Gemini Live Aoede), voice preview, personality mode (GF/romantic, professional, assistant).
2. **Voice Settings:** Low-level pitch, rate, audio buffer size, noise cancellation.
3. **Orb Customization:** Color palettes, pulse sensitivity, neon particle trail.
4. **API & Cloud:** Gemini API keys, custom endpoint, token meters.
5. **Permissions:** Accessibility, Microphone, Overlay, Default Assistant, Call, Notifications, SMS, Location, Contacts, Camera, Storage.
6. **Voice Authentication:** Voiceprint enroll karna (voice biometric authentication).
7. **Wake Word:** Always-listening wake word ("Hey MYRA", "Suno MYRA").
8. **Notification Intelligence:** Read WhatsApp/Telegram/Gmail, speak caller name, auto-reply, spam filter.
9. **PC Connect:** MYRA Companion PC app pairing via IP + PIN.
10. **Call Assistant:** Call announcement + in-call voice commands on/off.
11. **Subscription / Pro:** Plan upgrades, Google Play Billing.
12. **License:** Activation key status.
13. **User Profile:** Name, preferences, prime contacts.
14. **Batch Update:** App software updater.
15. **Chat:** Text chat with MYRA, voice notes, community chat.
16. **Task Logs:** Inspection of recent automation steps.

---

## 15. Automation & Triggers ("Agar X ho to Y karo")

| Trigger Type | Kab Chalta Hai |
| :--- | :--- |
| **Scheduled Time** | Fix time pe (e.g. roz subah 7:00 AM). |
| **Notification Se** | Kisi chuni hui app se notification aane pe (keyword filter ke saath). |
| **Charging State** | Charger plug ya unplug hone pe. |
| **Battery Level** | Battery threshold cross karne pe (e.g. < 20% or 100%). |
| **Screen State** | Screen on/off/unlock hone pe. |
| **Headphones** | 3.5mm jack / Bluetooth audio connect/disconnect hone pe. |
| **App Launch / Close** | Kisi specific app ke open hone ya close hone pe. |
| **Wi-Fi Network** | Specific Wi-Fi SSID connect/disconnect hone pe. |

*Habit Tracking (Automatic): MYRA aapke regular patterns seekh leti hai (e.g. roz 9 baje Maps kholna) aur same weekday pe 3+ baar repeat hone par proactively poochti hai: "Boss, aap roughly 9 baje Maps kholte ho, rasta laga doon?"*

---

## 16. Safety Rules — MYRA Kya Kabhi Nahi Karega

| Rule | Detail |
| :--- | :--- |
| **OTP/Password Privacy** | MYRA kabhi khud se proactively OTP, password ya bank PIN nahi bolti — sirf tab jab aap explicitly poochhein (*"OTP batao"*). |
| **Call-Reject Guard** | Ringing call ko sirf tab reject karegi jab pichle 20 seconds mein aapne clearly reject word bola ho (*"reject"*, *"kaat do"*, *"mat utha"*). Confusion mein hang up nahi karegi. |
| **Instant Lock Command** | *"Lock kar do"* turant bina kisi confirmation ke execute hota hai — kyunki ye urgent safety command hai. |
| **Traffic Data Honesty** | Agar real-time traffic data unavailable ho to MYRA saaf batayegi, kabhi jhoothi "traffic smooth hai" jaisi baat nahi banayegi. |
| **Offline Maps** | Offline map tiles download feature abhi stub hai aur bina internet ke live tile fetch nahi karega. |

---
*MYRA Tools & Features Guide — 2026 Edition · Generated from source code inventory*
