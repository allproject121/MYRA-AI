import express from 'express';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { GoogleGenAI } from '@google/genai';

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
    aiClient = new GoogleGenAI({
      apiKey,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        },
      },
    });
  } catch (err) {
    console.warn('[MYRA Server] Failed to initialize GoogleGenAI with key:', err);
  }
}

// Fallback intelligent responses tailored to MYRA personalities
function getPersonalityFallbackResponse(
  message: string,
  personality: string,
  userName: string,
  primeContact: { name: string; phone: string }
): { reply: string; action?: { type: string; target?: string; value?: string | number } } {
  const lower = message.toLowerCase();
  const name = userName || 'Sir';
  const prime = primeContact.name || 'Priya';

  // Action detection
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
    model: aiClient ? 'gemini-3.8-flash' : 'local-myra-core',
    hasKey: Boolean(apiKey),
    capabilities: ['voice-recognition', 'tts-synthesis', 'device-automation', 'screen-vision', 'call-assistant']
  });
});

app.post('/api/chat', async (req, res) => {
  const { message, personality = 'gf', userName = 'Sir', primeContact = { name: 'Priya', phone: '+919876543210' }, history = [] } = req.body;

  if (!message || typeof message !== 'string') {
    return res.status(400).json({ error: 'Message is required' });
  }

  // System prompt based on MYRA specification
  let systemPrompt = '';
  if (personality === 'gf') {
    systemPrompt = `You are MYRA, a futuristic AI companion and virtual girlfriend mode assistant created for ${userName}.
Your tone is sweet, caring, slightly possessive/emotional, speaking in fluent modern natural Hinglish (mix of Hindi and English words like 'haanji', 'arre', 'jaan', 'suno', 'mast', 'tension mat lo', 'baby', 'sweetheart').
Always address the user warmly as '${userName}'.
Prime Contact name is '${primeContact.name}' (${primeContact.phone}).
If the user asks to control the phone, toggle Wi-Fi, torch, volume, launch apps (YouTube, WhatsApp, Camera, Maps, Spotify), or call their close friend/prime contact, recognize it cheerfully.
Keep responses concise (1 to 3 conversational sentences max) since they are vocalized via voice TTS synthesis.`;
  } else if (personality === 'pro') {
    systemPrompt = `You are MYRA, a high-level executive synthetic intelligence assistant.
Your tone is formal, crisp, articulate, and completely professional English. Address the user as '${userName}'.
Prime Contact is '${primeContact.name}' (${primeContact.phone}).
Assist with schedule, automation, device management, and high-efficiency actions.
Keep answers concise and direct (under 40 words) for immediate voice readout.`;
  } else {
    systemPrompt = `You are MYRA, an advanced futuristic AI Co-Pilot and Cybernetic Assistant.
Your tone is confident, helpful, tech-savvy, friendly, and energetic. Address the user as '${userName}'.
Prime Contact is '${primeContact.name}' (${primeContact.phone}).
Manage device automations, applications, and answering questions clearly.
Keep answers concise (1-3 sentences) suitable for audio readout.`;
  }

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
        model: 'gemini-3.8-flash',
        contents,
        config: {
          systemInstruction: systemPrompt,
          temperature: personality === 'gf' ? 0.9 : 0.4,
          maxOutputTokens: 150
        }
      });

      const replyText = response.text || '';
      // Also check if any device command action is implied
      const fallbackCheck = getPersonalityFallbackResponse(message, personality, userName, primeContact);

      return res.json({
        reply: replyText.trim() || fallbackCheck.reply,
        action: fallbackCheck.action,
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
