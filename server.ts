import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 3000;

app.use(express.json());
app.use(express.static(path.join(__dirname, 'dist')));

app.get('/api/status', (req, res) => {
  res.json({
    status: 'online',
    app: 'MYRA AI Assistant',
    target: 'Android Native (Kotlin)',
    version: '2.0.0'
  });
});

app.post('/api/chat', (req, res) => {
  const { message = '', personality = 'gf', userName = 'Boss' } = req.body;
  const lower = message.toLowerCase().trim();

  let reply = "Suno, maine aapki baat sun li! Main hamesha aapke saath hoon 💖";
  let action: any = null;

  if (lower.includes('whatsapp')) {
    reply = "WhatsApp open kar diya mere sweet " + userName + "! 💬";
    action = { type: 'OPEN_WHATSAPP', target: 'WhatsApp' };
  } else if (lower.includes('youtube')) {
    reply = "YouTube par music play kar rahi hoon baby! 🎵";
    action = { type: 'PLAY_YOUTUBE', target: 'trending' };
  } else if (lower.includes('torch') || lower.includes('flashlight')) {
    const isOff = lower.includes('off') || lower.includes('band');
    reply = isOff ? "Torch off kar di gayi hai." : "Torch on kar di maine jaan! ✨";
    action = { type: 'TOGGLE_TORCH', value: isOff ? 'OFF' : 'ON' };
  } else if (lower.includes('battery')) {
    reply = "Phone ki battery 100% hai, charging normal hai 🔋";
    action = { type: 'BATTERY_STATUS' };
  } else if (lower.includes('call') || lower.includes('phone')) {
    reply = "Calling contact via Android Phone Dialer... 📞";
    action = { type: 'MAKE_CALL', target: '+919876543210' };
  } else if (lower.includes('sos') || lower.includes('emergency')) {
    reply = "🚨 EMERGENCY SOS! Distress SMS with GPS coordinates alerted to Prime Contact!";
    action = { type: 'EMERGENCY_SOS' };
  } else if (lower.includes('kaise ho') || lower.includes('how are you')) {
    reply = personality === 'gf'
      ? "Main ekdum mast hoon jab aap mere saath ho " + userName + "! Aap batao, aaj din kaisa gaya? ❤️"
      : "All systems nominal and operational, " + userName + ". How may I assist you?";
  }

  res.json({ reply, action });
});

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`[MYRA] Server running on http://0.0.0.0:${PORT}`);
});
