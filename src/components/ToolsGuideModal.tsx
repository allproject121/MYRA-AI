import React, { useState } from 'react';
import {
  X,
  Search,
  BookOpen,
  MessageCircle,
  PhoneCall,
  Music,
  Sliders,
  FolderOpen,
  Sparkles,
  Target,
  Bell,
  MapPin,
  Laptop,
  Globe,
  Smartphone,
  Cpu,
  Settings,
  Zap,
  ShieldAlert,
  Copy,
  Check,
  Play,
  Heart,
} from 'lucide-react';

interface ToolsGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRunCommand?: (cmd: string) => void;
}

interface CommandItem {
  toolName?: string;
  kyaBolo: string;
  example: string;
  kyaHoga: string;
  badge?: string;
}

interface SectionCategory {
  id: string;
  sectionNumber: number;
  title: string;
  icon: any;
  description: string;
  commands: CommandItem[];
}

export const ToolsGuideModal: React.FC<ToolsGuideModalProps> = ({ isOpen, onClose, onRunCommand }) => {
  const [activeCategoryId, setActiveCategoryId] = useState<string>('comm');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [copiedCmd, setCopiedCmd] = useState<string | null>(null);

  if (!isOpen) return null;

  const categories: SectionCategory[] = [
    {
      id: 'comm',
      sectionNumber: 1,
      title: 'Communication Tools',
      icon: MessageCircle,
      description: 'WhatsApp, direct SMS, Email inbox/compose aur Emergency SOS alerts',
      commands: [
        {
          toolName: 'send_whatsapp',
          kyaBolo: 'WhatsApp message bhejo',
          example: '"Rahul ko WhatsApp pe bolo main late hoon"',
          kyaHoga: 'Contact ka WhatsApp chat kholta hai, message likhta hai, Send dabata hai aur confirm karta hai.',
          badge: 'WHATSAPP',
        },
        {
          toolName: 'send_sms',
          kyaBolo: 'Seedha SMS bhejo',
          example: '"Mummy ko SMS karo ki pahuch gaya"',
          kyaHoga: 'Kisi contact ya phone number ko direct SMS bhejta hai.',
          badge: 'SMS',
        },
        {
          toolName: 'open_email_inbox',
          kyaBolo: 'Email inbox kholo',
          example: '"Mera inbox kholo"',
          kyaHoga: 'Aapka signed-in Gmail/Outlook inbox browser ya app mein kholta hai.',
        },
        {
          toolName: 'compose_email',
          kyaBolo: 'Email draft karo',
          example: '"Boss ko email likho meeting reschedule ho gayi"',
          kyaHoga: 'Email compose screen kholta hai, To/Subject/Body prefill karta hai — Send aap khud dabate ho.',
        },
        {
          toolName: 'send_emergency_alert',
          kyaBolo: 'Emergency SOS Alert',
          example: '"SOS" / "Main khatre mein hoon" / "Madad chahiye"',
          kyaHoga: 'Aapke trusted contact ko live GPS location ke saath emergency SOS alert bhejta hai.',
          badge: 'EMERGENCY',
        },
      ],
    },
    {
      id: 'calls',
      sectionNumber: 2,
      title: 'Call Tools',
      icon: PhoneCall,
      description: 'Call lagana, contacts lookup, aati hui call uthana aur call reject guard',
      commands: [
        {
          toolName: 'call_contact',
          kyaBolo: 'Contact ko call karo',
          example: '"Papa ko call karo" / "Call 9876543210"',
          kyaHoga: 'Saved contact ya direct number pe call dial karta hai.',
          badge: 'CALL',
        },
        {
          toolName: 'lookup_contact',
          kyaBolo: 'Contact number dhundo',
          example: '"Rahul ka number kya hai"',
          kyaHoga: 'Bina call/message kiye kisi contact ka saved phone number batata hai.',
        },
        {
          toolName: 'answer_call',
          kyaBolo: 'Aati hui call uthao',
          example: '"Utha lo" / "Answer karo" / "Haan"',
          kyaHoga: 'Incoming ringing call attend kar leta hai.',
        },
        {
          toolName: 'end_call',
          kyaBolo: 'Call kaato / reject karo',
          example: '"Call kaat do" / "Reject karo"',
          kyaHoga: 'SAFETY GUARD: Ringing call sirf tabhi kaatega jab aapne clearly reject/kaat do bola ho — galti se hang up nahi karega.',
          badge: 'GUARD',
        },
      ],
    },
    {
      id: 'media',
      sectionNumber: 3,
      title: 'Media Tools',
      icon: Music,
      description: 'Gaana chalana (YouTube/Spotify), media controls aur volume settings',
      commands: [
        {
          toolName: 'play_music',
          kyaBolo: 'Gaana chalao',
          example: '"Arijit Singh ka gaana chalao" / "Shape of You play karo"',
          kyaHoga: 'Song search karke play karta hai (YouTube/Spotify pe stream chalu karta hai).',
          badge: 'STREAM',
        },
        {
          toolName: 'media_control',
          kyaBolo: 'Media Play / Pause / Skip',
          example: '"Agla gaana" / "Pause karo" / "Music rok do"',
          kyaHoga: 'Jo bhi audio/video chal raha ho usko play, pause, next ya previous track pe switch karta hai.',
        },
        {
          toolName: 'set_volume',
          kyaBolo: 'Volume set karo',
          example: '"Volume 50% kar do" / "Volume thoda badha do"',
          kyaHoga: 'Media volume ko exact percentage ya step-up/down level pe adjust karta hai.',
        },
      ],
    },
    {
      id: 'device',
      sectionNumber: 4,
      title: 'Device Tools',
      icon: Sliders,
      description: 'Alarm, timer, flashlight, battery check, instant lock, storage analysis',
      commands: [
        {
          toolName: 'set_alarm',
          kyaBolo: 'Alarm set karo',
          example: '"Subah 6 baje alarm laga do"',
          kyaHoga: 'System clock mein exact time pe alarm schedule karta hai.',
        },
        {
          toolName: 'set_timer',
          kyaBolo: 'Timer shuru karo',
          example: '"10 minute ka timer laga do" / "60 second countdown"',
          kyaHoga: 'Background countdown timer start karta hai, alert chime ke saath.',
        },
        {
          toolName: 'toggle_flashlight',
          kyaBolo: 'Flashlight on / off',
          example: '"Torch on karo" / "Flashlight band karo"',
          kyaHoga: 'Phone hardware torch/flashlight toggle karta hai.',
        },
        {
          toolName: 'get_battery',
          kyaBolo: 'Battery status',
          example: '"Battery kitni hai"',
          kyaHoga: 'Current battery percentage aur charging status bol kar batata hai.',
        },
        {
          toolName: 'lock_device',
          kyaBolo: 'Screen lock karo',
          example: '"Lock kar do" / "Phone band kar do"',
          kyaHoga: 'NO CONFIRMATION NEEDED: Turant screen lock kar deta hai — urgent safety command.',
          badge: 'INSTANT',
        },
        {
          toolName: 'analyze_storage',
          kyaBolo: 'Storage check karo',
          example: '"Storage kitni bhari hai"',
          kyaHoga: 'Cache, temp files aur storage breakdown analyze karke batata hai.',
        },
        {
          toolName: 'clean_storage',
          kyaBolo: 'Storage clean karo',
          example: '"Storage clean kar do" / "Junk files saaf karo"',
          kyaHoga: 'Temporary cache aur junk files safe tarike se clear karta hai.',
          badge: 'CLEAN',
        },
      ],
    },
    {
      id: 'files_photos',
      sectionNumber: 5,
      title: 'Files & Photos',
      icon: FolderOpen,
      description: 'File manager, search, delete, zip, share sheet aur camera vision',
      commands: [
        {
          toolName: 'search_files',
          kyaBolo: 'File dhoondo',
          example: '"Wo PDF dhundo jo kal banayi thi" / "resume.pdf dhoondo"',
          kyaHoga: 'Device storage, Documents aur Downloads mein naam/type se files dhundta hai.',
        },
        {
          toolName: 'delete_photo',
          kyaBolo: 'Photos delete karo',
          example: '"Last 3 photo delete kar do"',
          kyaHoga: 'WhatsApp ya camera folder se sabse recent photos delete karta hai.',
        },
        {
          toolName: 'file_operation',
          kyaBolo: 'Zip / Unzip folders',
          example: '"Is folder ko zip kar do"',
          kyaHoga: 'Files ko zip/unzip ya move/copy karta hai bina file manager khole.',
        },
        {
          toolName: 'take_photo',
          kyaBolo: 'Photo kheecho',
          example: '"Ek photo le lo"',
          kyaHoga: 'Bina camera UI dikhaye snapshot lekar gallery mein save karta hai.',
        },
        {
          toolName: 'camera_vision',
          kyaBolo: 'Barcode / Doc scan',
          example: '"Ye barcode scan karo" / "Mera chehra dikhao"',
          kyaHoga: 'Camera viewfinder open karke visual object detection aur barcode reading karta hai.',
        },
      ],
    },
    {
      id: 'smart_gen',
      sectionNumber: 6,
      title: 'Smart & Generative Tools',
      icon: Sparkles,
      description: 'Website generator, AI image creator, web research, game coach, phone diagnostics',
      commands: [
        {
          toolName: 'generate_project',
          kyaBolo: 'Website / App banao',
          example: '"Ek portfolio website bana do"',
          kyaHoga: 'Voice prompt se poora frontend/coding project generate karke browser mein launch karta hai.',
          badge: 'CODE',
        },
        {
          toolName: 'generate_image',
          kyaBolo: 'AI Image generate karo',
          example: '"Ek sunset ki image banao"',
          kyaHoga: 'AI image model se creative visual artwork banata hai.',
        },
        {
          toolName: 'deep_research',
          kyaBolo: 'Deep Web Research',
          example: '"Electric cars market pe research karo"',
          kyaHoga: 'Multi-source web search karke crisp executive summary present karta hai.',
        },
        {
          toolName: 'system_health',
          kyaBolo: 'Phone health diagnose karo',
          example: '"Phone slow kyun hai" / "Battery drain check karo"',
          kyaHoga: 'RAM, thermal status aur battery consuming apps check karke report deta hai.',
        },
      ],
    },
    {
      id: 'missions',
      sectionNumber: 7,
      title: 'Missions (Bade Multi-Step Goals)',
      icon: Target,
      description: 'Complex multi-step goals with planning and autonomous tracking',
      commands: [
        {
          toolName: 'start_mission',
          kyaBolo: 'Mission shuru karo',
          example: '"Plan my trip to Delhi" / "Meri subah ki routine automate kar do"',
          kyaHoga: 'Multi-step goal shuru karta hai jisme planning + autonomous steps track hote hain.',
          badge: 'MISSION',
        },
        {
          toolName: 'pause_mission',
          kyaBolo: 'Mission roko / cancel karo',
          example: '"Mission pause karo" / "Mission cancel kar do"',
          kyaHoga: 'Active automation mission ko pause karta hai ya poori tarah cancel karta hai.',
        },
      ],
    },
    {
      id: 'notifications',
      sectionNumber: 8,
      title: 'Notification Tools',
      icon: Bell,
      description: 'Recent alerts padhna, direct reply, missed calls, aur explicit OTP reading',
      commands: [
        {
          toolName: 'read_notifications',
          kyaBolo: 'Notifications padho',
          example: '"Notifications padho"',
          kyaHoga: 'Recent important notifications (WhatsApp, Gmail, Telegram) padh kar sunata hai.',
        },
        {
          toolName: 'reply_to_notification',
          kyaBolo: 'Direct notification reply',
          example: '"Rahul ko reply karo \'okay main nikal raha hoon\'"',
          kyaHoga: 'Bina app open kiye notification channel ke zariye seedha reply send karta hai.',
        },
        {
          toolName: 'read_missed_calls',
          kyaBolo: 'Missed calls batao',
          example: '"Missed calls batao"',
          kyaHoga: 'Haal hi ke missed call alerts aur contact names bol kar sunata hai.',
        },
        {
          toolName: 'read_otp',
          kyaBolo: 'OTP padho (Explicit Only)',
          example: '"OTP batao"',
          kyaHoga: 'EXPLICIT SAFETY: Sirf user ke poochne par latest security OTP padhta hai, kabhi proactively nahi bolta.',
          badge: 'PRIVACY',
        },
        {
          toolName: 'clear_notifications',
          kyaBolo: 'Notifications saaf karo',
          example: '"Notifications clear kar do"',
          kyaHoga: 'Status bar ke saare non-persistent active alerts clear karta hai.',
        },
      ],
    },
    {
      id: 'maps_world',
      sectionNumber: 9,
      title: 'Maps & "My World"',
      icon: MapPin,
      description: 'Turn-by-turn navigation, location, parking memory, nearby services, smart driving modes',
      commands: [
        {
          toolName: 'open_map',
          kyaBolo: 'Personal AI Map kholo',
          example: '"Map kholo"',
          kyaHoga: 'MYRA ka interactive satellite & street map viewer launch karta hai.',
        },
        {
          toolName: 'navigate_to',
          kyaBolo: 'Navigation shuru karo',
          example: '"Airport ka rasta dikhao" / "Ghar chalo"',
          kyaHoga: 'Google Maps mein turn-by-turn routing navigation start karta hai.',
          badge: 'GPS',
        },
        {
          toolName: 'save_parking',
          kyaBolo: 'Parking location yaad rakho',
          example: '"Yahan park kiya hai, yaad rakho"',
          kyaHoga: 'Current exact GPS coordinates ko parking spot ke roop mein save kar leta hai.',
        },
        {
          toolName: 'get_parking_location',
          kyaBolo: 'Parking spot dhundo',
          example: '"Gaadi kahan park ki thi"',
          kyaHoga: 'Saved parking location ka address aur distance batata hai.',
        },
        {
          toolName: 'search_nearby',
          kyaBolo: 'Nearby ATM / Hospital',
          example: '"Nearby ATM dikhao" / "Petrol pump dhundo"',
          kyaHoga: 'Aas-paas ke verified essential points-of-interest display karta hai.',
        },
        {
          toolName: 'set_smart_mode',
          kyaBolo: 'Driving / Sleep Mode',
          example: '"Driving mode on kar do" / "Sleep mode laga do"',
          kyaHoga: 'Distraction-free automated mode enable karta hai.',
        },
      ],
    },
    {
      id: 'pc_connect',
      sectionNumber: 10,
      title: 'PC Connect (MYRA Companion)',
      icon: Laptop,
      description: 'Laptop/PC ko phone se control karna via FastAPI local bridge',
      commands: [
        {
          toolName: 'pc_connect',
          kyaBolo: 'PC connect setup',
          example: '"PC Connect pairing"',
          kyaHoga: 'Settings → PC Connect mein desktop IP address aur PIN enter karke pair karta hai.',
        },
        {
          toolName: 'pc_command',
          kyaBolo: 'PC pe command chalao',
          example: '"PC pe Chrome kholo" / "PC lock kar do"',
          kyaHoga: 'Paired desktop companion ko local network HTTP bridge ke zariye control instructions bhejta hai.',
          badge: 'TITAN',
        },
        {
          toolName: 'send_file_to_pc',
          kyaBolo: 'Phone se PC pe file bhejo',
          example: '"Ye file PC pe bhej do"',
          kyaHoga: 'Direct Wi-Fi transfer se files desktop workspace mein bhej deta hai.',
        },
      ],
    },
    {
      id: 'search_apps',
      sectionNumber: 11,
      title: 'Search, Browser & Apps',
      icon: Globe,
      description: 'Google search, browser controls aur installed apps launch',
      commands: [
        {
          toolName: 'search_google',
          kyaBolo: 'Google search',
          example: '"Google pe search karo aaj ka mausam"',
          kyaHoga: 'Web query search karke browser results load karta hai.',
        },
        {
          toolName: 'open_app',
          kyaBolo: 'Koi bhi App kholo',
          example: '"WhatsApp kholo" / "YouTube open karo" / "Settings kholo"',
          kyaHoga: 'Device ke installed apps ko package name intent ke zariye seedha launch karta hai.',
          badge: 'INTENT',
        },
        {
          toolName: 'open_browser',
          kyaBolo: 'Browser control',
          example: '"Browser kholo" / "Peeche jao" / "New tab kholo"',
          kyaHoga: 'Browser window launch, tab switching aur navigation execute karta hai.',
        },
      ],
    },
    {
      id: 'screen_auto',
      sectionNumber: 12,
      title: 'Screen Automation (Background)',
      icon: Smartphone,
      description: 'Accessibility service screen reader aur automated UI gestures',
      commands: [
        {
          toolName: 'start_task',
          kyaBolo: 'Automation task',
          example: '"Zomato se pizza order kar do" / "Ola book karo"',
          kyaHoga: 'Background agent accessibility tree inspect karke automated tap/type actions perform karta hai.',
          badge: 'AUTO',
        },
        {
          toolName: 'kill_task',
          kyaBolo: 'Task roko',
          example: '"Ruk jao" / "Stop task"',
          kyaHoga: 'Chal rahe background UI automation ko turant emergency stop karta hai.',
        },
      ],
    },
    {
      id: 'connectors',
      sectionNumber: 13,
      title: 'Connector Tools (3P Integrations)',
      icon: Cpu,
      description: 'Google Calendar, Drive, GitHub, Canva aur multi-LLM backends',
      commands: [
        {
          toolName: 'google_list_upcoming_events',
          kyaBolo: 'Calendar events',
          example: '"Mere upcoming events dikhao"',
          kyaHoga: 'Google Calendar se aane wale meetings aur schedule fetch karta hai.',
        },
        {
          toolName: 'google_drive_list_recent_files',
          kyaBolo: 'Drive files',
          example: '"Drive ki recent files dikhao"',
          kyaHoga: 'Google Drive cloud storage ki recent documents list karta hai.',
        },
        {
          toolName: 'github_list_repositories',
          kyaBolo: 'GitHub repos',
          example: '"Mere GitHub repos dikhao"',
          kyaHoga: 'Recently updated code repositories aur commits retrieve karta hai.',
        },
      ],
    },
    {
      id: 'settings_screens',
      sectionNumber: 14,
      title: 'App Settings & Screens',
      icon: Settings,
      description: 'Voice models, permissions, wake word, notifications, pro license',
      commands: [
        {
          toolName: 'settings_voice',
          kyaBolo: 'Voice & AI Models',
          example: '"Settings mein voice badlo"',
          kyaHoga: 'Gemini Live Aoede voice, Google TTS, GF / Professional personality switch karna.',
        },
        {
          toolName: 'settings_permissions',
          kyaBolo: 'System Permissions',
          example: '"Permissions check karo"',
          kyaHoga: 'Accessibility, Microphone, Call, SMS, Overlay aur Battery optimization checks.',
        },
        {
          toolName: 'settings_wake_word',
          kyaBolo: 'Wake Word Engine',
          example: '"Wake word settings kholo"',
          kyaHoga: 'Always-listening engine for "Hey MYRA" / "Suno MYRA" customize karna.',
        },
      ],
    },
    {
      id: 'triggers',
      sectionNumber: 15,
      title: 'Automation & Triggers',
      icon: Zap,
      description: '"Agar X ho to Y karo" rules — time, charger, battery, Wi-Fi, habit tracking',
      commands: [
        {
          toolName: 'trigger_schedule',
          kyaBolo: 'Time Trigger',
          example: '"Roz subah 7 baje weather batao"',
          kyaHoga: 'Scheduled clock time par proactive automated actions trigger karta hai.',
          badge: 'TRIGGER',
        },
        {
          toolName: 'trigger_charging',
          kyaBolo: 'Charger & Battery Rules',
          example: '"Charger lagane par battery status bolo"',
          kyaHoga: 'Charger plug/unplug ya battery levels (< 20%) cross hone par automated tasks execute karta hai.',
        },
        {
          toolName: 'habit_tracking',
          kyaBolo: 'Automatic Habit Learning',
          example: '"Automatic habit learning (Always ON)"',
          kyaHoga: 'MYRA aapke regular patterns learn karta hai aur 3+ repeats par proactive assistance deta hai.',
        },
      ],
    },
    {
      id: 'safety',
      sectionNumber: 16,
      title: 'Safety Rules (Non-Negotiable)',
      icon: ShieldAlert,
      description: 'MYRA kya kabhi nahi karega — OTP privacy, reject guard, honest traffic data',
      commands: [
        {
          toolName: 'rule_otp',
          kyaBolo: 'OTP / PIN Privacy',
          example: '"Never speaks OTP proactively"',
          kyaHoga: 'MYRA kabhi khud se proactively OTP ya bank PIN nahi bolti — sirf explicit request par bolti hai.',
          badge: 'STRICT',
        },
        {
          toolName: 'rule_call_reject',
          kyaBolo: 'Call Reject Guard',
          example: '"Prevents accidental hang-ups"',
          kyaHoga: 'Ringing call sirf tabhi reject karegi jab pichle 20s mein clear reject word bola ho.',
        },
        {
          toolName: 'rule_instant_lock',
          kyaBolo: 'Emergency Instant Lock',
          example: '"Lock kar do"',
          kyaHoga: 'Screen lock turant bina kisi confirmation ke execute hota hai as an emergency guard.',
          badge: 'INSTANT',
        },
        {
          toolName: 'rule_data_honesty',
          kyaBolo: 'Data Honesty Guarantee',
          example: '"Honest telemetry"',
          kyaHoga: 'Agar real data unavailable ho to MYRA saaf batayegi, kabhi jhoothi baat nahi banayegi.',
        },
      ],
    },
  ];

  // Search filtering
  const filteredCategories = searchQuery.trim()
    ? categories
        .map((cat) => ({
          ...cat,
          commands: cat.commands.filter(
            (c) =>
              c.kyaBolo.toLowerCase().includes(searchQuery.toLowerCase()) ||
              c.example.toLowerCase().includes(searchQuery.toLowerCase()) ||
              c.kyaHoga.toLowerCase().includes(searchQuery.toLowerCase()) ||
              (c.toolName && c.toolName.toLowerCase().includes(searchQuery.toLowerCase()))
          ),
        }))
        .filter((cat) => cat.commands.length > 0)
    : categories;

  const currentCategory =
    filteredCategories.find((c) => c.id === activeCategoryId) || filteredCategories[0] || categories[0];

  const handleCopy = (text: string) => {
    const match = text.match(/"([^"]+)"/);
    const cleanCmd = match ? match[1] : text;
    navigator.clipboard.writeText(cleanCmd);
    setCopiedCmd(text);
    setTimeout(() => setCopiedCmd(null), 1800);
  };

  const getCleanTestCommand = (cmd: CommandItem): string => {
    const match = cmd.example.match(/"([^"]+)"/);
    return match ? match[1] : cmd.kyaBolo;
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-2 sm:p-5 animate-fadeIn">
      <div className="bg-[#080816] border border-[#1f1f3e] w-full max-w-6xl h-[92vh] rounded-3xl flex flex-col overflow-hidden shadow-2xl relative">
        
        {/* Header */}
        <div className="px-5 py-4 border-b border-[#1A1A2E] bg-gradient-to-r from-[#0c0c24] via-[#141433] to-[#0c0c24] flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-[#FF1744] to-[#FF5252] flex items-center justify-center shadow-lg shadow-[#FF1744]/30">
              <BookOpen className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-base sm:text-lg font-bold font-mono text-white tracking-wide">
                  MYRA Complete Tools & Features Guide
                </h2>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-[#FF1744]/20 text-[#FF5252] border border-[#FF1744]/30">
                  2026 EDITION
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-[#00E676]/20 text-[#00E676] border border-[#00E676]/30">
                  16 Sections • 50+ Tools
                </span>
              </div>
              <p className="text-xs text-zinc-400 font-mono flex items-center gap-1.5 mt-0.5">
                <span>Har voice tool, har command, har setting — ek jagah</span>
                <span className="text-zinc-600">•</span>
                <span className="text-[#00E676] font-semibold">Gemini 2.5 Flash Native Audio</span>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 hover:bg-white/10 rounded-full transition-colors text-zinc-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tip & Philosophy Bar */}
        <div className="px-5 py-2.5 bg-gradient-to-r from-[#FF1744]/10 via-[#00E676]/10 to-[#FF1744]/10 border-b border-[#1A1A2E] flex items-center gap-2 text-xs font-mono text-zinc-300 shrink-0">
          <Sparkles className="w-4 h-4 text-[#00E676] shrink-0" />
          <span className="truncate sm:whitespace-normal">
            <strong className="text-white">💡 NATURAL CONVERSATION:</strong> Exact command yaad rakhne ki zaroorat nahi —{' '}
            <strong className="text-[#00E676]">MYRA</strong> natural Hindi, English aur Hinglish samajhti hai. Bas apna kaam bolo!
          </span>
        </div>

        {/* Search Bar */}
        <div className="px-5 py-3 border-b border-[#1A1A2E] bg-[#0A0A1C] shrink-0">
          <div className="relative">
            <Search className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search across all 16 categories (e.g. WhatsApp, Call, Volume, Alarm, Mission, GPS, OTP, Safety)..."
              className="w-full bg-[#121226] border border-[#1f1f3e] focus:border-[#FF1744] rounded-xl pl-10 pr-4 py-2 text-xs font-mono text-white placeholder-zinc-500 outline-none transition-colors"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-zinc-400 hover:text-white"
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* Main Content Area */}
        <div className="flex-1 flex overflow-hidden">
          
          {/* Categories Sidebar */}
          <div className="w-72 border-r border-[#1A1A2E] bg-[#060612] overflow-y-auto p-2.5 space-y-1 shrink-0 hidden md:block">
            {filteredCategories.map((cat) => {
              const Icon = cat.icon;
              const isActive = currentCategory.id === cat.id;
              return (
                <button
                  key={cat.id}
                  onClick={() => setActiveCategoryId(cat.id)}
                  className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-left transition-all text-xs font-mono ${
                    isActive
                      ? 'bg-gradient-to-r from-[#FF1744]/25 to-transparent border-l-2 border-[#FF1744] text-white font-bold'
                      : 'text-zinc-400 hover:bg-white/5 hover:text-zinc-200'
                  }`}
                >
                  <span className="text-[10px] w-4 text-zinc-500 shrink-0 font-bold">
                    {cat.sectionNumber}.
                  </span>
                  <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-[#FF1744]' : 'text-zinc-400'}`} />
                  <span className="truncate flex-1">{cat.title}</span>
                  <span className="text-[10px] px-1.5 py-0.2 bg-white/5 rounded-full text-zinc-500">
                    {cat.commands.length}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Commands List */}
          <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4 bg-[#080816]">
            {filteredCategories.length === 0 ? (
              <div className="text-center py-16">
                <Search className="w-10 h-10 text-zinc-600 mx-auto mb-3" />
                <p className="text-sm font-mono text-zinc-400">
                  Koi tool match nahi hua "{searchQuery}" ke liye.
                </p>
                <p className="text-xs text-zinc-500 font-mono mt-1">
                  Kuch aur search karein jaise "WhatsApp", "Call", "Storage", "Mission", "OTP", "GPS"
                </p>
              </div>
            ) : (
              <>
                <div className="border-b border-[#1A1A2E] pb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono px-2 py-0.5 rounded bg-[#FF1744]/20 text-[#FF5252] font-bold">
                      Section {currentCategory.sectionNumber}
                    </span>
                    <h3 className="text-base font-bold font-mono text-white">
                      {currentCategory.title}
                    </h3>
                    <span className="text-xs font-mono px-2 py-0.5 rounded-full bg-white/10 text-zinc-300">
                      {currentCategory.commands.length} Tools & Actions
                    </span>
                  </div>
                  <p className="text-xs text-zinc-400 font-mono mt-1">
                    {currentCategory.description}
                  </p>
                </div>

                <div className="space-y-3 pt-1">
                  {currentCategory.commands.map((cmd, idx) => (
                    <div
                      key={idx}
                      className="bg-[#0e0e22] border border-[#1f1f3e] rounded-2xl p-4 hover:border-[#FF1744]/40 transition-colors"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex-1">
                          
                          {/* Tool Name & Badge */}
                          <div className="flex items-center gap-2 flex-wrap">
                            {cmd.toolName && (
                              <code className="text-[11px] font-mono font-bold text-[#FF5252] bg-[#FF1744]/10 px-2 py-0.5 rounded border border-[#FF1744]/20">
                                {cmd.toolName}
                              </code>
                            )}
                            <span className="font-mono font-bold text-sm text-[#00E676]">
                              {cmd.kyaBolo}
                            </span>
                            {cmd.badge && (
                              <span className="px-2 py-0.5 rounded-full text-[9px] font-mono font-bold bg-[#FF1744]/20 text-[#FF6D6D] border border-[#FF1744]/40">
                                {cmd.badge}
                              </span>
                            )}
                          </div>

                          {/* Example Voice Command */}
                          <div className="flex items-start gap-2 mt-2 text-xs font-mono text-zinc-200">
                            <span className="text-zinc-500 font-semibold shrink-0">Command:</span>
                            <span className="italic text-[#FFD54F] font-medium">{cmd.example}</span>
                          </div>

                          {/* Kya Hoga */}
                          <div className="flex items-start gap-2 mt-1.5 text-xs font-mono text-zinc-300">
                            <span className="text-zinc-500 font-semibold shrink-0">Kya karta hai:</span>
                            <span className="leading-relaxed">{cmd.kyaHoga}</span>
                          </div>

                        </div>

                        {/* Action buttons */}
                        <div className="flex items-center gap-2 shrink-0">
                          <button
                            onClick={() => handleCopy(cmd.example)}
                            className="p-2 hover:bg-white/10 rounded-xl text-zinc-400 hover:text-white transition-colors"
                            title="Copy command"
                          >
                            {copiedCmd === cmd.example ? (
                              <Check className="w-4 h-4 text-[#00E676]" />
                            ) : (
                              <Copy className="w-4 h-4" />
                            )}
                          </button>

                          {onRunCommand && (
                            <button
                              onClick={() => {
                                const cleanPrompt = getCleanTestCommand(cmd);
                                onRunCommand(cleanPrompt);
                                onClose();
                              }}
                              className="px-3 py-1.5 bg-[#FF1744]/20 hover:bg-[#FF1744]/30 border border-[#FF1744]/50 rounded-xl text-xs font-mono font-bold text-[#FF6D6D] transition-colors flex items-center gap-1.5 shadow-sm"
                              title="Test command with MYRA"
                            >
                              <Play className="w-3 h-3 fill-current" />
                              <span>Test</span>
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>

        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-[#1A1A2E] bg-[#0a0a18] flex items-center justify-between text-xs text-zinc-400 font-mono">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-bold text-white">MYRA</span>
            <span>— Complete Tools & Features Guide 2026</span>
            <span className="text-zinc-600 hidden sm:inline">•</span>
            <span className="text-[#FF5252] hidden sm:inline">16 Sections</span>
            <span className="text-zinc-600 hidden sm:inline">•</span>
            <span className="hidden sm:inline">Generated from source code inventory</span>
          </div>
          <button
            onClick={onClose}
            className="px-4 py-1.5 bg-white/10 hover:bg-white/15 text-white rounded-xl transition-colors font-mono text-xs"
          >
            Close Guide
          </button>
        </div>

      </div>
    </div>
  );
};
