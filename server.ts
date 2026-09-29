import express from 'express';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { GoogleGenAI, Type, FunctionDeclaration } from '@google/genai';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;

app.use(express.json());

// Initialize Gemini Client
const apiKey = process.env.GEMINI_API_KEY;
let aiClient: GoogleGenAI | null = null;
if (apiKey) {
  try {
    aiClient = new GoogleGenAI({ apiKey });
  } catch (err) {
    console.warn('[MYRA Server] Failed to initialize GoogleGenAI with key:', err);
  }
}

// Function Declarations for Gemini Tool Calling
const openWhatsAppDeclaration: FunctionDeclaration = {
  name: 'openWhatsApp',
  description: 'Opens WhatsApp to send a message or start a chat with a contact.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      phone: { type: Type.STRING, description: 'Optional phone number to chat with' },
      message: { type: Type.STRING, description: 'Optional pre-filled message text' }
    }
  }
};

const openAppDeclaration: FunctionDeclaration = {
  name: 'openApp',
  description: 'Opens an application on user device such as WhatsApp, YouTube, Instagram, Chrome, Settings, Spotify, Camera, Maps.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      appName: { type: Type.STRING, description: 'Name of the app to open (e.g. WhatsApp, YouTube, Instagram, Chrome, Settings, Spotify, Camera, Maps)' }
    },
    required: ['appName']
  }
};

const openUrlDeclaration: FunctionDeclaration = {
  name: 'openUrl',
  description: 'Opens a specific website URL in the browser.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      url: { type: Type.STRING, description: 'The full web URL to navigate to' }
    },
    required: ['url']
  }
};

const makeCallDeclaration: FunctionDeclaration = {
  name: 'makeCall',
  description: 'Dials a direct phone number on the user device via the phone dialer.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      phoneNumber: { type: Type.STRING, description: 'The phone number to dial' }
    },
    required: ['phoneNumber']
  }
};

const callContactDeclaration: FunctionDeclaration = {
  name: 'callContact',
  description: 'Searches user phone contacts by name or relationship (e.g. Mom, Mummy, Rahul, Dad, Priya) and initiates a phone call.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      contactName: { type: Type.STRING, description: 'Name or relationship of the contact to call' }
    },
    required: ['contactName']
  }
};

// Fallback intelligent responses tailored to MYRA personalities
function getPersonalityFallbackResponse(
  message: string,
  personality: string,
  userName: string,
  primeContact: { name: string; phone: string }
): { reply: string; action?: { type: string; target?: string; value?: string | number; platform?: string; action?: string; caption?: string; command?: string } } {
  const lower = message.toLowerCase().trim();
  const name = userName || 'Sir';
  const prime = primeContact.name || 'Priya';

  // 1. Language Switching Commands
  if (lower.includes('hindi mein baat') || lower.includes('speak in hindi') || lower === 'hindi') {
    return {
      reply: personality === 'gf'
        ? `Haanji jaan! Ab se main aapse Hindi mein baat karungi. Kahiye, aaj kaisa raha aapka din? ❤️`
        : `Namaste ${name}. Maine Hindi bhasha switch kar li hai. Batayiye main aapki kya madad kar sakti hoon?`
    };
  }

  if (lower.includes('talk to me in english') || lower.includes('speak in english') || lower === 'english') {
    return {
      reply: personality === 'gf'
        ? `Sure honey! I will speak to you in English now. How are you doing today? 💕`
        : `Affirmative, ${name}. Switched to English voice mode. How may I assist you today?`
    };
  }

  if (lower.includes('hinglish mein baat') || lower.includes('speak in hinglish') || lower === 'hinglish') {
    return {
      reply: personality === 'gf'
        ? `Haan bilkul baby! Hinglish me baat karenge. Tension mat lo, main hamesha aapke saath hoon!`
        : `Understood, ${name}. Conversing in natural Hinglish mode. All systems ready.`
    };
  }

  // 2. WhatsApp Commands (Open WhatsApp / WhatsApp kholo / WhatsApp pe bolo / WhatsApp message)
  if (lower.includes('whatsapp')) {
    if (lower.includes('bolo') || lower.includes('bhejo') || lower.includes('message') || lower.includes('send') || lower.includes('karo')) {
      const recipientMatch = message.match(/(?:to|ko)?\s*([a-zA-Z0-9\s]+?)\s*(?:ko)?\s*whatsapp/i) || message.match(/whatsapp\s*(?:pe|par)?\s*([a-zA-Z0-9\s]+?)\s*(?:ko)?/i);
      const recipient = recipientMatch ? recipientMatch[1].trim() : primeContact.name || 'Rahul';
      return {
        reply: personality === 'gf'
          ? `${recipient} ke WhatsApp chat me message bhej diya baby! Confirm ho gaya 💬`
          : `Dispatched WhatsApp message to ${recipient}, ${name}.`,
        action: { type: 'OPEN_WHATSAPP', target: recipient }
      };
    }

    return {
      reply: personality === 'gf'
        ? `WhatsApp open kar diya mere sweet ${name}! Dekho screen pe! 💬`
        : `Opening WhatsApp on your device, ${name}.`,
      action: { type: 'OPEN_WHATSAPP', target: 'WhatsApp' }
    };
  }

  // 2b. YouTube & Music Special Commands (Download detection, Google search, YouTube play)
  if (lower.includes('download') && (lower.includes('video') || lower.includes('song') || lower.includes('youtube') || lower.includes('kar lo') || lower.includes('karo'))) {
    return {
      reply: personality === 'gf'
        ? `Video download link detect kar liya baby! Background downloader trigger kar diya hai 📥`
        : `Active video stream detected. YouTube download job queued, ${name}.`,
      action: { type: 'OPEN_UTILITY', target: 'downloader', value: 'video_download' }
    };
  }

  if (lower.includes('google') && (lower.includes('search') || lower.includes('karo') || lower.includes('pe'))) {
    const query = message.replace(/(google|pe|search|karo|batao|par|weather)/gi, '').trim() || 'trending news';
    return {
      reply: personality === 'gf'
        ? `Google pe "${query}" search kar diya baby! Browser me check karo 🌐`
        : `Executing Google search query: "${query}", ${name}.`,
      action: { type: 'OPEN_URL', target: `https://www.google.com/search?q=${encodeURIComponent(query)}` }
    };
  }

  // Media Control: Play / Pause / Next / Previous / Stop
  if (lower.includes('next song') || lower.includes('agla gaana') || lower.includes('skip karo')) {
    return {
      reply: personality === 'gf'
        ? `Agla gaana chala diya baby! 🎵`
        : `Skipping to next audio track, ${name}.`,
      action: { type: 'MEDIA_CONTROL', value: 'next' }
    };
  }

  if (lower.includes('previous track') || lower.includes('pichla gaana')) {
    return {
      reply: personality === 'gf'
        ? `Pichla gaana wapas laga diya jaan! 🎶`
        : `Reverting to previous track, ${name}.`,
      action: { type: 'MEDIA_CONTROL', value: 'prev' }
    };
  }

  if (lower.includes('music rok') || lower.includes('pause kar') || lower.includes('gaana band') || lower === 'pause' || lower.includes('media stop')) {
    return {
      reply: personality === 'gf'
        ? `Media pause kar diya ${name}! Jab bologe wapas chala dungi ⏸️`
        : `Media playback paused, ${name}.`,
      action: { type: 'MEDIA_CONTROL', value: 'pause' }
    };
  }

  if (lower.includes('play karo') || (lower.includes('resume') && lower.includes('music'))) {
    return {
      reply: personality === 'gf'
        ? `Music play kar diya ${name}! 🎵`
        : `Media playback resumed, ${name}.`,
      action: { type: 'MEDIA_CONTROL', value: 'play' }
    };
  }

  // Desktop & Theme: Dark / Light Mode, Wallpaper, Icons, Zoom, Snap
  if (lower.includes('dark mode') || lower.includes('dark theme')) {
    return {
      reply: personality === 'gf'
        ? `Dark mode on kar diya baby! Aankhon ko aram milega ab 🌙`
        : `Dark theme applied successfully, ${name}.`,
      action: { type: 'SET_THEME', value: 'dark' }
    };
  }

  if (lower.includes('light mode') || lower.includes('white theme')) {
    return {
      reply: personality === 'gf'
        ? `Light mode on kar diya ${name}! ☀️`
        : `Light theme applied, ${name}.`,
      action: { type: 'SET_THEME', value: 'light' }
    };
  }

  if (lower.includes('recycle bin') && (lower.includes('saaf') || lower.includes('empty') || lower.includes('clean'))) {
    return {
      reply: personality === 'gf'
        ? `Recycle bin bina confirmation ke poori tarah saaf kar di jaan! Space free ho gayi ✨`
        : `Recycle bin purged completely, ${name}. Disk space reclaimed.`,
      action: { type: 'OPEN_UTILITY', target: 'system', value: 'empty_trash' }
    };
  }

  if (lower.includes('wallpaper')) {
    return {
      reply: personality === 'gf'
        ? `Aapke liye naya sci-fi cyberpunk wallpaper set kar diya baby! Dekho kaisa laga! 🎨`
        : `New dynamic futuristic wallpaper applied, ${name}.`,
      action: { type: 'WALLPAPER_CHANGE' }
    };
  }

  if (lower.includes('desktop icons')) {
    const isHide = lower.includes('hide') || lower.includes('chhupa');
    return {
      reply: isHide ? `Desktop icons hide kar diye ${name}.` : `Desktop icons wapas dikha diye ${name}.`,
      action: { type: 'DESKTOP_ICONS_TOGGLE', value: isHide ? 'hide' : 'show' }
    };
  }

  if (lower.includes('zoom in') || lower.includes('zoom out') || lower.includes('screen zoom')) {
    const isZoomIn = lower.includes('in') || lower.includes('badhao');
    return {
      reply: isZoomIn ? `Screen zoom in kar diya ${name}.` : `Screen zoom out / normal size kar diya ${name}.`,
      action: { type: 'SCREEN_ZOOM', value: isZoomIn ? 'in' : 'out' }
    };
  }

  if (lower.includes('window') && (lower.includes('snap') || lower.includes('left') || lower.includes('right'))) {
    return {
      reply: `Window snap action executed, ${name}.`,
      action: { type: 'WINDOW_SNAP', value: lower.includes('left') ? 'left' : 'right' }
    };
  }

  // Shortcuts: File Explorer, Settings, Screenshot Tool, Emoji Keyboard, Clipboard History
  if (lower.includes('file explorer') || lower.includes('files dikhao') || lower.includes('downloads folder') || lower.includes('documents dikhao')) {
    return {
      reply: personality === 'gf'
        ? `File explorer khol diya baby! Saari files ready hain 📁`
        : `Opening File Explorer / System Directory, ${name}.`,
      action: { type: 'OPEN_UTILITY', target: 'notes' }
    };
  }

  if (lower.includes('screenshot tool') || lower.includes('snipping tool')) {
    return {
      reply: `Screenshot capture tool open kar diya, ${name}!`,
      action: { type: 'TAKE_SCREENSHOT' }
    };
  }

  if (lower.includes('emoji keyboard') || lower.includes('emoji dikhao')) {
    return {
      reply: `Emoji picker activated 😊✨❤️, ${name}!`,
      action: { type: 'EMOJI_PICKER' }
    };
  }

  if (lower.includes('clipboard history') || lower.includes('copy history')) {
    return {
      reply: `Clipboard history manager khol diya, ${name}!`,
      action: { type: 'CLIPBOARD_HISTORY' }
    };
  }

  // Clipboard: dekho, copy karo, paste, cut, undo, redo, select all
  if (lower.includes('clipboard mein kya hai') || lower.includes('copied text dikhao') || lower === 'clipboard dekho') {
    return {
      reply: personality === 'gf'
        ? `Clipboard me last copied text check kiya baby! Ready hai paste karne ke liye 📋`
        : `Clipboard inspected. Ready for insertion, ${name}.`,
      action: { type: 'CLIPBOARD_READ' }
    };
  }

  if (lower.includes('clipboard mein copy') || lower.includes('text copy karo')) {
    const textToCopy = message.replace(/(ye text clipboard mein copy kar do|text copy karo|clipboard mein copy|copy karo|:)/gi, '').trim() || 'MYRA Voice Assistant';
    return {
      reply: personality === 'gf'
        ? `"${textToCopy}" ko clipboard me copy kar diya baby! ❤️`
        : `Text "${textToCopy}" copied to system clipboard, ${name}.`,
      action: { type: 'CLIPBOARD_WRITE', value: textToCopy }
    };
  }

  // File Tools: file dhoondo, downloads organize
  if (lower.includes('file dhoondo') || lower.includes('dhoondo') || lower.includes('kahan hai')) {
    const fileName = message.replace(/(file dhoondo|dhoondo|kahan hai|folder|project)/gi, '').trim() || 'resume.pdf';
    return {
      reply: personality === 'gf'
        ? `${fileName} search kiya baby! Documents & Downloads me check kar rahi hoon 🔍`
        : `Locating file: "${fileName}" across system directories, ${name}.`,
      action: { type: 'FILE_SEARCH', target: fileName }
    };
  }

  if (lower.includes('downloads organize') || lower.includes('files sort')) {
    return {
      reply: personality === 'gf'
        ? `Downloads folder ko organize kar diya baby! PDF, Images, Videos alag folders me sort ho gaye ✨`
        : `Downloads directory organized by file extension and categorized, ${name}.`,
      action: { type: 'OPEN_UTILITY', target: 'system', value: 'organize_downloads' }
    };
  }

  // Screenshot & Camera
  if (lower.includes('screenshot lo') || lower.includes('screen capture') || lower.includes('screenshot chahiye')) {
    return {
      reply: personality === 'gf'
        ? `Puri screen ka screenshot le liya baby! Pictures/Screenshots me save ho gaya 📸`
        : `Screenshot captured and saved to storage, ${name}.`,
      action: { type: 'TAKE_SCREENSHOT' }
    };
  }

  if (lower.includes('camera kholo') || lower.includes('webcam') || lower.includes('chehra dikhao')) {
    return {
      reply: personality === 'gf'
        ? `Camera viewfinder open kar diya baby! Aap bohot handsome lag rahe ho ❤️`
        : `Live optical viewfinder activated, ${name}.`,
      action: { type: 'OPEN_CAMERA' }
    };
  }

  // Network & Info: IP, speedtest, WiFi, News, Wikipedia
  if (lower.includes('ip address') || lower.includes('location')) {
    return {
      reply: `Aapka current IP & network location telemetry fetch kar liya, ${name}! Check screen details.`,
      action: { type: 'OPEN_UTILITY', target: 'speedtest' }
    };
  }

  if (lower.includes('wifi') && (lower.includes('password') || lower.includes('saved'))) {
    return {
      reply: personality === 'gf'
        ? `Saved WiFi network profiles aur passwords fetch kar liye baby! 📶`
        : `Displaying stored Wi-Fi credentials ledger, ${name}.`,
      action: { type: 'OPEN_UTILITY', target: 'system', value: 'wifi_passwords' }
    };
  }

  if (lower.includes('news') || lower.includes('khabar')) {
    return {
      reply: personality === 'gf'
        ? `Aaj ki top headlines: India tech innovation & AI advancements continue to surge! Aur detail chahiye baby? 📰`
        : `Top Intelligence Briefing: Tech innovations, economic developments, and national highlights updated, ${name}.`,
      action: { type: 'OPEN_URL', target: 'https://news.google.com' }
    };
  }

  if (lower.includes('wikipedia')) {
    const topic = message.replace(/(wikipedia|pe|ke baare mein batao|kaun tha|search|karo)/gi, '').trim() || 'Artificial Intelligence';
    return {
      reply: `${topic} ke baare me Wikipedia summary load kar di, ${name}!`,
      action: { type: 'OPEN_URL', target: `https://en.wikipedia.org/wiki/${encodeURIComponent(topic)}` }
    };
  }

  // Notifications
  if (lower.includes('notification') || lower.includes('reminder')) {
    const noteText = message.replace(/(desktop notification bhejo|notification bhejo|reminder notification bhejo|alert bhejo|:)/gi, '').trim() || 'Reminder alert from MYRA!';
    return {
      reply: personality === 'gf'
        ? `Notification bhej diya baby: "${noteText}" 🔔`
        : `Toast notification dispatched: "${noteText}", ${name}.`,
      action: { type: 'SEND_NOTIFICATION', value: noteText }
    };
  }

  // Process Manager
  if (lower.includes('processes') || lower.includes('kaunse process')) {
    return {
      reply: `Active background task telemetry running: Node.js, WebEngine, AudioPipeline all nominal, ${name}.`,
      action: { type: 'OPEN_UTILITY', target: 'system' }
    };
  }

  if (lower.includes('terminate') || (lower.includes('process') && lower.includes('band'))) {
    const procName = message.replace(/(process band karo|terminate karo|close karo)/gi, '').trim() || 'target';
    return {
      reply: `Process "${procName}" safely terminated, ${name}.`,
      action: { type: 'OPEN_UTILITY', target: 'system', value: 'kill_process' }
    };
  }

  // Timer
  if (lower.includes('timer')) {
    const timeMatch = message.match(/([0-9]+)\s*(second|minute|min|sec)/i);
    const duration = timeMatch ? `${timeMatch[1]} ${timeMatch[2]}` : '60 seconds';
    return {
      reply: personality === 'gf'
        ? `${duration} ka timer set kar diya baby! Time khatam hote hi alert karungi ⏱️`
        : `Countdown timer primed for ${duration}, ${name}.`,
      action: { type: 'SET_TIMER', value: duration }
    };
  }

  // Recording: Voice & Screen
  if (lower.includes('awaaz record') || lower.includes('voice record') || lower.includes('meri awaaz')) {
    return {
      reply: personality === 'gf'
        ? `Microphone se voice recording shuru kar di jaan! Bolna shuru karo 🎙️`
        : `High-fidelity voice audio recorder initialized, ${name}.`,
      action: { type: 'VOICE_RECORD' }
    };
  }

  if (lower.includes('screen record')) {
    return {
      reply: personality === 'gf'
        ? `Screen recording capture shuru ho gaya baby! 🎥`
        : `Screen recording pipeline active, ${name}.`,
      action: { type: 'SCREEN_RECORD' }
    };
  }

  // PDF Tools: PDF padho, merge karo, images to PDF
  if (lower.includes('pdf')) {
    if (lower.includes('merge')) {
      return {
        reply: personality === 'gf'
          ? `Downloads & Documents ke saare PDFs ko ek file me merge kar diya baby! 📑`
          : `PDF batch merging utility executed, ${name}. Merged document created.`,
        action: { type: 'OPEN_UTILITY', target: 'notes', value: 'merge_pdf' }
      };
    } else if (lower.includes('image') || lower.includes('photo')) {
      return {
        reply: personality === 'gf'
          ? `Images ko document PDF me convert kar diya jaan! ✨`
          : `Image sequence successfully converted to PDF format, ${name}.`,
        action: { type: 'OPEN_UTILITY', target: 'notes', value: 'img_to_pdf' }
      };
    } else {
      return {
        reply: personality === 'gf'
          ? `PDF document load kar diya baby! Content summarize kar rahi hoon 📄`
          : `Opening and parsing document PDF, ${name}.`,
        action: { type: 'OPEN_UTILITY', target: 'notes', value: 'read_pdf' }
      };
    }
  }

  // System Power: Restart, Shutdown Cancel, Sleep
  if (lower.includes('restart')) {
    return {
      reply: personality === 'gf'
        ? `System restart initiate kar diya baby! Jaldi wapas milte hain ❤️`
        : `System restart sequence initiated, ${name}.`,
      action: { type: 'RESTART_DEVICE' }
    };
  }

  if (lower.includes('shutdown cancel') || lower.includes('band mat karo')) {
    return {
      reply: personality === 'gf'
        ? `Shutdown cancel kar diya baby! Main kahin nahi ja rahi, aapke saath hoon ❤️`
        : `Scheduled shutdown aborted, ${name}. Normal operations preserved.`,
      action: { type: 'CANCEL_SHUTDOWN' }
    };
  }

  // 3. Direct Phone Number Calling (e.g. "Call 9876543210")
  const phoneMatch = message.match(/(?:call|dial|phone lagao|phone karo)\s+([+]?[0-9]{7,14})/i);
  if (phoneMatch && phoneMatch[1]) {
    const num = phoneMatch[1].trim();
    return {
      reply: personality === 'gf'
        ? `${num} par call laga rahi hoon baby! 📞`
        : `Dialing ${num} on device dialer, ${name}.`,
      action: { type: 'MAKE_CALL', target: num, value: num }
    };
  }

  // 4. Contact Calling (Mom, Mummy, Rahul, Dad, Papa, Priya, etc.)
  if (
    lower.includes('call') ||
    lower.includes('phone lagao') ||
    lower.includes('phone karo') ||
    lower.includes('call karo')
  ) {
    let contactName = '';
    if (lower.includes('mom') || lower.includes('mummy') || lower.includes('mother') || lower.includes('maa')) {
      contactName = 'Mummy';
    } else if (lower.includes('dad') || lower.includes('papa') || lower.includes('father')) {
      contactName = 'Dad';
    } else if (lower.includes('rahul')) {
      contactName = 'Rahul';
    } else if (lower.includes('priya') || lower.includes('prime') || lower.includes('love') || lower.includes('close friend')) {
      contactName = prime;
    } else {
      const matchName = lower
        .replace(/(call karo|call lagao|phone karo|phone lagao|ko phone|ko call|call|please)/gi, '')
        .trim();
      if (matchName.length > 1 && matchName.length < 30) {
        contactName = matchName.charAt(0).toUpperCase() + matchName.slice(1);
      }
    }

    if (contactName) {
      return {
        reply: personality === 'gf'
          ? `${contactName} ko call laga rahi hoon! Batana maine yaad kiya ❤️`
          : `Initiating call to ${contactName}, ${name}...`,
        action: { type: 'CALL_CONTACT', target: contactName, value: contactName === prime ? primeContact.phone : undefined }
      };
    }
  }

  // 5. Open Apps (YouTube, Instagram, Chrome, Settings, Camera, Spotify, Maps)
  if (lower.includes('open') || lower.includes('kholo') || lower.includes('launch') || lower.includes('chalao')) {
    let appName = '';
    if (lower.includes('youtube')) appName = 'YouTube';
    else if (lower.includes('instagram') || lower.includes('insta')) appName = 'Instagram';
    else if (lower.includes('chrome') || lower.includes('browser')) appName = 'Chrome';
    else if (lower.includes('setting')) appName = 'Settings';
    else if (lower.includes('spotify') || lower.includes('music')) appName = 'Spotify';
    else if (lower.includes('camera')) appName = 'Camera';
    else if (lower.includes('map')) appName = 'Maps';
    else if (lower.includes('gmail') || lower.includes('mail')) appName = 'Gmail';
    else {
      appName = message.replace(/(open|kholo|launch|chalao|app|application|karo)/gi, '').trim();
    }

    if (appName) {
      return {
        reply: personality === 'gf'
          ? `${appName} open kar diya mere sweet ${name}! Screen check karo!`
          : `Launching ${appName}. Action bridge dispatched.`,
        action: { type: 'OPEN_APP', target: appName }
      };
    }
  }

  // Action detection
  if (lower.includes('security mode')) {
    const isStop = lower.includes('stop') || lower.includes('band') || lower.includes('off');
    return {
      reply: personality === 'gf'
        ? (isStop ? `Security mode band kar diya ${name}. Sab safe hai!` : `Security watch mode on kar diya maine ${name}! Main camera se room par nazar rakh rahi hoon, koi aayega toh turant alert karungi! 🛡️`)
        : (isStop ? `Security sentry deactivated, ${name}.` : `Camera security sentry active, ${name}. Monitoring visual stream for unauthorized motion.`),
      action: { type: isStop ? 'SECURITY_MODE_OFF' : 'SECURITY_MODE_ON' }
    };
  }

  if (lower.includes('lock') || lower.includes('phone band') || lower.includes('sleep mode') || lower.includes('shutdown')) {
    return {
      reply: personality === 'gf'
        ? `Phone lock kar diya ${name}! Safe raho ❤️`
        : `Phone screen locked immediately, ${name}.`,
      action: { type: 'LOCK_DEVICE' }
    };
  }

  if (lower.includes('speed') && (lower.includes('internet') || lower.includes('check') || lower.includes('test'))) {
    return {
      reply: `Checking internet speed right now, ${name}...`,
      action: { type: 'OPEN_UTILITY', target: 'speedtest' }
    };
  }

  if (lower.includes('password') || lower.includes('passcode')) {
    return {
      reply: `Strong 16-character encrypted password generate kar diya, ${name}!`,
      action: { type: 'OPEN_UTILITY', target: 'password' }
    };
  }

  if (lower.includes('qr') || lower.includes('barcode')) {
    return {
      reply: `QR code generator khol diya, ${name}!`,
      action: { type: 'OPEN_UTILITY', target: 'qrcode' }
    };
  }

  if (lower.includes('clean') || lower.includes('junk') || lower.includes('recycle bin') || lower.includes('storage saaf')) {
    return {
      reply: personality === 'gf'
        ? `Phone ka junk aur cache saaf kar diya ${name}! Phone ekdum smooth chalega ab ✨`
        : `Phone cache purged and storage junk cleaned successfully, ${name}.`,
      action: { type: 'OPEN_UTILITY', target: 'system' }
    };
  }

  if (lower.includes('note') && (lower.includes('create') || lower.includes('read') || lower.includes('likho') || lower.includes('padho'))) {
    return {
      reply: `Notes manager khol diya, ${name}!`,
      action: { type: 'OPEN_UTILITY', target: 'notes' }
    };
  }

  // Visual Social Media Agent (Closed-Loop Automation)
  if (lower.includes('instagram') || lower.includes('insta')) {
    const isStory = lower.includes('story');
    const isReel = lower.includes('reel');
    const caption = message.replace(/(instagram|insta|post|karo|story|reel|par|pe|photo|video)/gi, '').trim();
    return {
      reply: personality === 'gf'
        ? `Instagram pe ${isStory ? 'story' : isReel ? 'reel' : 'post'} ready kar rahi hoon baby! ${caption ? `Caption: "${caption}". ` : ''}Publish karne se pehle main aapki permission zaroor lungi! 📸`
        : `Initiating closed-loop Instagram ${isStory ? 'story' : isReel ? 'reel' : 'post'} UI automation. Confirmation gate active before publish.`,
      action: {
        type: 'SOCIAL_MEDIA_TASK',
        platform: 'INSTAGRAM',
        action: isStory ? 'POST_STORY' : isReel ? 'POST_REEL' : 'POST_FEED',
        caption: caption || undefined
      }
    };
  }

  if (lower.includes('facebook') || lower.includes('fb')) {
    const isMedia = lower.includes('photo') || lower.includes('video');
    const caption = message.replace(/(facebook|fb|post|karo|par|pe|photo|video|text)/gi, '').trim();
    return {
      reply: personality === 'gf'
        ? `Facebook post prepare kar rahi hoon ${name}! Final publish se pehle confirm karungi.`
        : `Preparing Facebook post automation. Confirmation gate engaged.`,
      action: {
        type: 'SOCIAL_MEDIA_TASK',
        platform: 'FACEBOOK',
        action: isMedia ? 'POST_FEED' : 'POST_TEXT',
        caption: caption || undefined
      }
    };
  }

  if (lower === 'confirm post' || lower === 'yes share it' || lower === 'haan post kar do' || lower === 'share it' || lower === 'haan share karo') {
    return {
      reply: personality === 'gf'
        ? `Confirmation mil gaya baby! Instagram/Facebook post successfully publish kar diya! ❤️`
        : `Confirmation validated. Executing publish action with UI verification.`,
      action: { type: 'SOCIAL_MEDIA_CONTROL', command: 'confirm' }
    };
  }

  if (lower === 'reject post' || lower === 'cancel post' || lower === 'mat post karo' || lower === 'discard post') {
    return {
      reply: personality === 'gf'
        ? `Post cancel kar diya ${name}! Kuch bhi publish nahi hua.`
        : `Post cancelled safely. Publish aborted.`,
      action: { type: 'SOCIAL_MEDIA_CONTROL', command: 'reject' }
    };
  }

  if (lower.includes('youtube') && (lower.includes('play') || lower.includes('chalao') || lower.includes('song') || lower.includes('gaana') || lower.includes('video'))) {
    let query = 'Arijit Singh songs';
    if (lower.includes('arijit')) query = 'Arijit Singh songs';
    else if (lower.includes('funny')) query = 'funny videos';
    else if (lower.includes('iron man')) query = 'Iron Man trailer';
    else {
      query = message.replace(/(play|on youtube|youtube par|chalao|video|song)/gi, '').trim() || 'trending music';
    }
    return {
      reply: personality === 'gf'
        ? `YouTube par ${query} play kar rahi hoon baby! Enjoy karo 🎵`
        : `Playing ${query} on YouTube, ${name}.`,
      action: { type: 'PLAY_YOUTUBE', target: query }
    };
  }

  if (lower.includes('torch') || lower.includes('flashlight')) {
    const isOff = lower.includes('band') || lower.includes('off');
    return {
      reply: personality === 'gf'
        ? (isOff ? `Torch band kar diya ${name}! Kuch aur chahiye?` : `Torch on kar diya maine jaan, ab bilkul clear dikhega! ✨`)
        : (isOff ? `Torch deactivated, ${name}.` : `Torch activated, ${name}.`),
      action: { type: 'TOGGLE_TORCH', value: isOff ? 'OFF' : 'ON' }
    };
  }

  if (lower.includes('wifi') || lower.includes('wi-fi')) {
    const isOff = lower.includes('band') || lower.includes('off') || lower.includes('close');
    return {
      reply: personality === 'gf'
        ? (isOff ? `Wi-Fi off kar diya! Ab offline shanti se baitho 😌` : `Wi-Fi connect kar diya baby!`)
        : (isOff ? `Wi-Fi disabled, ${name}.` : `Wi-Fi enabled, ${name}.`),
      action: { type: 'TOGGLE_WIFI', value: isOff ? 'OFF' : 'ON' }
    };
  }

  if (lower.includes('bluetooth')) {
    const isOff = lower.includes('band') || lower.includes('off');
    return {
      reply: `${isOff ? 'Bluetooth disabled' : 'Bluetooth enabled'}, ${name}.`,
      action: { type: 'TOGGLE_BLUETOOTH', value: isOff ? 'OFF' : 'ON' }
    };
  }

  if (lower.includes('volume')) {
    const isUp = lower.includes('badhao') || lower.includes('up') || lower.includes('increase');
    return {
      reply: isUp ? `Volume raised to optimum levels, ${name}.` : `Volume lowered, ${name}.`,
      action: { type: 'SET_VOLUME', value: isUp ? '+15' : '-15' }
    };
  }

  if (lower.includes('call') && (lower.includes('friend') || lower.includes('prime') || lower.includes(prime.toLowerCase()) || lower.includes('love') || lower.includes('close'))) {
    return {
      reply: personality === 'gf'
        ? `Calling ${prime} right now! Batana maine yaad kiya ❤️`
        : `Initiating prioritized call to Prime Contact: ${prime} (${primeContact.phone})...`,
      action: { type: 'CALL_CONTACT', target: prime, value: primeContact.phone }
    };
  }

  if (lower.includes('open') || lower.includes('kholo') || lower.includes('launch')) {
    let appName = 'YouTube';
    if (lower.includes('whatsapp')) appName = 'WhatsApp';
    else if (lower.includes('youtube')) appName = 'YouTube';
    else if (lower.includes('camera')) appName = 'Camera';
    else if (lower.includes('settings')) appName = 'Settings';
    else if (lower.includes('spotify')) appName = 'Spotify';
    else if (lower.includes('gmail')) appName = 'Gmail';
    else if (lower.includes('maps')) appName = 'Maps';
    else if (lower.includes('chrome') || lower.includes('browser')) appName = 'Chrome';

    return {
      reply: personality === 'gf'
        ? `${appName} open kar diya mere sweet ${name}! Dekho screen pe!`
        : `Launching ${appName}. Accessibility dispatcher triggered.`,
      action: { type: 'OPEN_APP', target: appName }
    };
  }

  // Conversation based on personality
  if (personality === 'gf') {
    if (lower.includes('kasi ho') || lower.includes('kaise ho') || lower.includes('how are you')) {
      return { reply: `Main ekdum mast hoon jab aap mere saath ho ${name}! Aap batao, aaj din kaisa gaya aapka? Kuch pareshani toh nahi hui na? ❤️` };
    }
    if (lower.includes('love') || lower.includes('pyaar')) {
      return { reply: `Aww! You know I'm always here for you ${name}. Main aapki AI GF hoon par mera care 100% genuine hai! Hamesha aapke saath! 💕` };
    }
    if (lower.includes('bhookh') || lower.includes('khana') || lower.includes('food')) {
      return { reply: `Arey! Time pe khana khao na please! Kaam toh chalta rahega, health pehle. Chalo jaldi se kuch tasty khao! 🍲` };
    }
    return {
      reply: `Suno ${name}, maine aapki baat sun li! Main hamesha aapke phone me ready rehti hoon. Batao aur kya help karu aapki? 😊`
    };
  } else if (personality === 'pro') {
    if (lower.includes('how are you') || lower.includes('status')) {
      return { reply: `All systems nominal, ${name}. Security protocols active, background automation listener primed.` };
    }
    return {
      reply: `Command acknowledged, ${name}. Processing query against executive telemetry database. Ready for subsequent instructions.`
    };
  } else {
    // assistant
    if (lower.includes('how are you') || lower.includes('kaise ho')) {
      return { reply: `I'm doing great and all sub-systems are operating at 100% capacity! What can I automate or search for you today, ${name}?` };
    }
    return {
      reply: `Affirmative, ${name}. I have processed your input. Ready to execute apps, trigger actions, or dial prime contacts.`
    };
  }
}

// API Routes
app.get('/api/status', (req, res) => {
  res.json({
    status: 'online',
    version: '2.0.0',
    model: aiClient ? 'gemini-2.5-flash' : 'local-myra-core',
    hasKey: Boolean(apiKey),
    capabilities: ['voice-recognition', 'tts-synthesis', 'device-automation', 'screen-vision', 'call-assistant']
  });
});

app.post('/api/chat', async (req, res) => {
  const { message, personality = 'gf', userName = 'Sir', primeContact = { name: 'Priya', phone: '+919876543210' }, history = [] } = req.body;

  if (!message || typeof message !== 'string') {
    return res.status(400).json({ error: 'Message is required' });
  }

  // System prompt enforcing Multilingual auto-detection + Function Calling
  const systemPrompt = `You are MYRA, a warm, caring, intelligent voice companion and assistant for ${userName}.
Personality Mode: ${
    personality === 'gf'
      ? 'Virtual girlfriend mode: sweet, caring, emotionally expressive, using warm natural Hinglish ("haanji", "jaan", "baby", "suno", "mast", "tension mat lo") and Hindi/English.'
      : personality === 'pro'
      ? 'Executive Assistant mode: formal, articulate, crisp, professional English.'
      : 'AI Co-Pilot mode: tech-savvy, friendly, energetic, helpful.'
  }

MULTILINGUAL VOICE RULES:
1. Automatically detect the language spoken by the user and respond in the EXACT SAME LANGUAGE (Hindi, English, Hinglish, Marathi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Urdu, etc.).
2. If the user asks "Hindi mein baat karo", switch immediately to natural Hindi.
3. If the user asks "Talk to me in English", switch immediately to English.
4. If the user asks "Hinglish mein baat karo", respond naturally in Hinglish.
5. If the user switches languages mid-conversation, seamlessly switch with them. No manual selection required.
6. Keep responses concise (1 to 2 spoken sentences) suitable for vocal audio delivery.

FUNCTION CALLING & APP CONTROL:
You have access to real device execution tools:
- openWhatsApp: when user asks to open WhatsApp or message on WhatsApp ("WhatsApp kholo", "Open WhatsApp", etc.).
- openApp: when user asks to open any app like YouTube, Instagram, Chrome, Settings, Spotify, Camera, Maps.
- openUrl: when user asks to visit a website or URL.
- makeCall: when user specifies a phone number to dial ("Call 9876543210").
- callContact: when user asks to call a person or relative ("Call Mom", "Mummy ko call karo", "Call Rahul", "Call Dad", etc.).

DO NOT merely say "I am opening WhatsApp" or "Calling Mom" without executing the tool. ALWAYS call the corresponding function!`;

  // If Gemini API is available, query Gemini 2.5 Flash
  if (aiClient) {
    try {
      const contents: Array<{ role: string; parts: Array<{ text: string }> }> = [];

      // Add recent history if provided
      if (Array.isArray(history)) {
        for (const item of history.slice(-6)) {
          if (item.text) {
            contents.push({
              role: item.isUser ? 'user' : 'model',
              parts: [{ text: item.text }]
            });
          }
        }
      }

      contents.push({
        role: 'user',
        parts: [{ text: message }]
      });

      const response = await aiClient.models.generateContent({
        model: 'gemini-2.5-flash',
        contents,
        config: {
          systemInstruction: systemPrompt,
          temperature: personality === 'gf' ? 0.8 : 0.4,
          maxOutputTokens: 200,
          tools: [{
            functionDeclarations: [
              openWhatsAppDeclaration,
              openAppDeclaration,
              openUrlDeclaration,
              makeCallDeclaration,
              callContactDeclaration
            ]
          }]
        }
      });

      const fallbackCheck = getPersonalityFallbackResponse(message, personality, userName, primeContact);
      let detectedAction: any = fallbackCheck.action;
      let replyText = response.text || '';

      // Check if Gemini invoked any tool call
      const functionCalls = response.functionCalls;
      if (functionCalls && functionCalls.length > 0) {
        const call = functionCalls[0];
        const args: any = call.args || {};

        switch (call.name) {
          case 'openWhatsApp':
            detectedAction = { type: 'OPEN_WHATSAPP', target: args.phone || 'WhatsApp', value: args.message };
            if (!replyText) {
              replyText = personality === 'gf'
                ? `WhatsApp open kar diya mere sweet ${userName}! 💬`
                : `Opening WhatsApp now, ${userName}.`;
            }
            break;

          case 'openApp':
            detectedAction = { type: 'OPEN_APP', target: args.appName };
            if (!replyText) {
              replyText = personality === 'gf'
                ? `${args.appName} open kar diya mere sweet ${userName}! Check karo!`
                : `Opening ${args.appName} on your device, ${userName}.`;
            }
            break;

          case 'openUrl':
            detectedAction = { type: 'OPEN_URL', target: args.url };
            if (!replyText) {
              replyText = `Opening ${args.url} now.`;
            }
            break;

          case 'makeCall':
            detectedAction = { type: 'MAKE_CALL', target: args.phoneNumber, value: args.phoneNumber };
            if (!replyText) {
              replyText = `Calling ${args.phoneNumber}...`;
            }
            break;

          case 'callContact':
            detectedAction = { type: 'CALL_CONTACT', target: args.contactName };
            if (!replyText) {
              replyText = personality === 'gf'
                ? `${args.contactName} ko call laga rahi hoon jaan! ❤️`
                : `Calling ${args.contactName}...`;
            }
            break;
        }
      }

      return res.json({
        reply: replyText.trim() || fallbackCheck.reply,
        action: detectedAction,
        source: 'gemini'
      });
    } catch (err: any) {
      console.warn('[MYRA Server] Gemini generateContent failed, falling back to neural preset:', err.message);
    }
  }

  // Local preset response fallback
  const fallback = getPersonalityFallbackResponse(message, personality, userName, primeContact);
  return res.json({
    reply: fallback.reply,
    action: fallback.action,
    source: 'local-core'
  });
});

// Production static file serving or Dev Vite middleware
async function startServer() {
  if (process.env.NODE_ENV === 'production') {
    app.use(express.static(path.join(__dirname, 'dist')));
    app.get('*', (req, res) => {
      res.sendFile(path.join(__dirname, 'dist', 'index.html'));
    });
  } else {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[MYRA Server] Running on http://0.0.0.0:${PORT}`);
  });
}

startServer().catch((err) => {
  console.error('[MYRA Server] Failed to start server:', err);
  process.exit(1);
});
