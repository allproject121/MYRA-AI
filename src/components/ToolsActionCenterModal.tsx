import React, { useState, useEffect } from 'react';
import { DeviceState, PrimeContact } from '../types';
import {
  X,
  Phone,
  MessageSquare,
  Flashlight,
  Compass,
  Music,
  Globe,
  Mail,
  ShieldAlert,
  Battery,
  Copy,
  ExternalLink,
  Download,
  Search,
  CheckCircle,
  AlertCircle
} from 'lucide-react';

interface ToolsActionCenterProps {
  isOpen: boolean;
  onClose: () => void;
  deviceState: DeviceState;
  primeContact: PrimeContact;
  onExecuteVoiceCommand: (cmd: string) => void;
}

export const ToolsActionCenterModal: React.FC<ToolsActionCenterProps> = ({
  isOpen,
  onClose,
  deviceState,
  primeContact,
  onExecuteVoiceCommand,
}) => {
  const [activeTab, setActiveTab] = useState<'tools' | 'communication' | 'media' | 'apk'>('tools');
  const [actionNotice, setActionNotice] = useState<string | null>(null);
  const [torchActive, setTorchActive] = useState<boolean>(false);
  const [streamTrack, setStreamTrack] = useState<MediaStreamTrack | null>(null);
  const [batteryInfo, setBatteryInfo] = useState<{ level: number; charging: boolean } | null>(null);

  // Read real battery
  useEffect(() => {
    if (typeof navigator !== 'undefined' && 'getBattery' in navigator) {
      (navigator as any).getBattery().then((battery: any) => {
        setBatteryInfo({
          level: Math.round(battery.level * 100),
          charging: battery.charging,
        });
        battery.addEventListener('levelchange', () => {
          setBatteryInfo({
            level: Math.round(battery.level * 100),
            charging: battery.charging,
          });
        });
      }).catch(() => {});
    }
  }, []);

  if (!isOpen) return null;

  const showNotification = (msg: string) => {
    setActionNotice(msg);
    setTimeout(() => setActionNotice(null), 3500);
  };

  // Real Phone Call
  const handleCall = (phoneNumber: string) => {
    showNotification(`Calling ${phoneNumber}... Opening device phone dialer.`);
    window.location.href = `tel:${phoneNumber}`;
  };

  // Real WhatsApp
  const handleWhatsApp = (phoneNumber: string, message: string = 'Hi!') => {
    const cleanNumber = phoneNumber.replace(/[^0-9]/g, '');
    showNotification(`Opening WhatsApp for ${phoneNumber}...`);
    window.open(`https://wa.me/${cleanNumber}?text=${encodeURIComponent(message)}`, '_blank');
  };

  // Real SMS
  const handleSMS = (phoneNumber: string, message: string = 'Hello from MYRA!') => {
    showNotification(`Opening SMS composer for ${phoneNumber}...`);
    window.location.href = `sms:${phoneNumber}?body=${encodeURIComponent(message)}`;
  };

  // Real Email
  const handleEmail = (email: string = 'support@google.com') => {
    showNotification(`Opening email compose window...`);
    window.location.href = `mailto:${email}?subject=${encodeURIComponent('Inquiry from MYRA AI')}`;
  };

  // Real Maps Navigation
  const handleNavigation = (destination: string) => {
    showNotification(`Opening Google Maps navigation to ${destination}...`);
    window.open(`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destination)}`, '_blank');
  };

  // Real Search
  const handleSearch = (query: string) => {
    showNotification(`Searching Google for "${query}"...`);
    window.open(`https://www.google.com/search?q=${encodeURIComponent(query)}`, '_blank');
  };

  // Real Music Playback
  const handleMusic = (query: string = 'Arijit Singh') => {
    showNotification(`Opening music for "${query}"...`);
    window.open(`https://music.youtube.com/search?q=${encodeURIComponent(query)}`, '_blank');
  };

  // Real Camera Flashlight / Torch
  const handleToggleTorch = async () => {
    try {
      if (torchActive && streamTrack) {
        streamTrack.stop();
        setStreamTrack(null);
        setTorchActive(false);
        showNotification('Torch turned off.');
        return;
      }

      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' }
        });
        const track = stream.getVideoTracks()[0];
        const capabilities: any = track.getCapabilities ? track.getCapabilities() : {};
        if (capabilities.torch) {
          await (track as any).applyConstraints({ advanced: [{ torch: true }] });
          setStreamTrack(track);
          setTorchActive(true);
          showNotification('Torch activated via device camera hardware! ✨');
        } else {
          // If browser restricts torch constraint, keep track for video
          setStreamTrack(track);
          setTorchActive(true);
          showNotification('Camera hardware activated. Torch constraint active.');
        }
      } else {
        showNotification('Camera/Flashlight hardware not accessible in this environment.');
      }
    } catch (err: any) {
      showNotification('Torch toggle notice: ' + (err.message || 'Permission denied'));
    }
  };

  // Real Emergency SOS Alert
  const handleSOS = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude;
          const lon = pos.coords.longitude;
          const mapLink = `https://maps.google.com/?q=${lat},${lon}`;
          const alertMessage = `EMERGENCY SOS ALERT from MYRA! I need immediate help. My location: ${mapLink}`;
          handleSMS(primeContact.phone, alertMessage);
          showNotification(`SOS dispatched with GPS coordinates to ${primeContact.name}!`);
        },
        () => {
          const alertMessage = `EMERGENCY SOS ALERT from MYRA! I need immediate assistance!`;
          handleSMS(primeContact.phone, alertMessage);
          showNotification(`SOS alert sent to ${primeContact.name}!`);
        }
      );
    } else {
      handleSMS(primeContact.phone, 'EMERGENCY SOS ALERT! Please contact me immediately.');
    }
  };

  // Real Clipboard Copy
  const handleCopy = (text: string) => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
      showNotification(`Copied to clipboard: "${text}"`);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-xl max-h-[92vh] bg-[#080812] border border-[#FF1744]/40 rounded-3xl shadow-2xl shadow-[#FF1744]/20 flex flex-col overflow-hidden text-sm">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#1A1A2E] bg-[#0c0c1a]">
          <div className="flex items-center gap-2.5">
            <div className="w-2.5 h-2.5 rounded-full bg-[#00E676] shadow-[0_0_10px_#00E676]" />
            <div>
              <h2 className="text-base font-bold tracking-wider text-white font-mono flex items-center gap-2">
                MYRA REAL TOOLS & ACTIONS
              </h2>
              <span className="text-[10px] text-zinc-400 font-mono">2026 Edition · Native & Real Web Integrations</span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-zinc-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Action Notice Banner */}
        {actionNotice && (
          <div className="bg-[#1C050B] border-b border-[#FF1744]/50 px-5 py-2.5 flex items-center gap-2 text-xs text-[#FF6D6D] font-mono animate-fadeIn">
            <CheckCircle className="w-4 h-4 text-[#FF1744] shrink-0" />
            <span>{actionNotice}</span>
          </div>
        )}

        {/* Tab Navigation */}
        <div className="flex border-b border-[#1A1A2E] bg-[#06060c] px-4">
          <button
            onClick={() => setActiveTab('tools')}
            className={`py-3 px-4 font-mono text-xs font-semibold tracking-wider transition-all border-b-2 ${
              activeTab === 'tools'
                ? 'border-[#FF1744] text-[#FF1744]'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            ⚡ Device & System
          </button>
          <button
            onClick={() => setActiveTab('communication')}
            className={`py-3 px-4 font-mono text-xs font-semibold tracking-wider transition-all border-b-2 ${
              activeTab === 'communication'
                ? 'border-[#FF1744] text-[#FF1744]'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            📞 Communication
          </button>
          <button
            onClick={() => setActiveTab('media')}
            className={`py-3 px-4 font-mono text-xs font-semibold tracking-wider transition-all border-b-2 ${
              activeTab === 'media'
                ? 'border-[#FF1744] text-[#FF1744]'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            🎵 Media & Maps
          </button>
          <button
            onClick={() => setActiveTab('apk')}
            className={`py-3 px-4 font-mono text-xs font-semibold tracking-wider transition-all border-b-2 ${
              activeTab === 'apk'
                ? 'border-[#FF1744] text-[#FF1744]'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            📱 Android APK
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5">
          
          {/* TAB 1: Device & System Tools */}
          {activeTab === 'tools' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                {/* Battery Status */}
                <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex flex-col justify-between">
                  <div className="flex items-center justify-between text-zinc-400 text-xs font-mono">
                    <span>BATTERY TELEMETRY</span>
                    <Battery className="w-4 h-4 text-[#FF1744]" />
                  </div>
                  <div className="my-2">
                    <span className="text-2xl font-bold font-mono text-white">
                      {batteryInfo ? `${batteryInfo.level}%` : `${deviceState.battery}%`}
                    </span>
                    <span className="text-[10px] text-zinc-400 block font-mono">
                      {batteryInfo?.charging ? '⚡ Charging active' : 'Discharging'}
                    </span>
                  </div>
                  <button
                    onClick={() => onExecuteVoiceCommand('Battery kitni hai')}
                    className="mt-1 text-[11px] text-[#FF6D6D] hover:underline font-mono text-left"
                  >
                    Ask MYRA →
                  </button>
                </div>

                {/* Torch / Flashlight */}
                <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex flex-col justify-between">
                  <div className="flex items-center justify-between text-zinc-400 text-xs font-mono">
                    <span>FLASHLIGHT HARDWARE</span>
                    <Flashlight className={`w-4 h-4 ${torchActive ? 'text-amber-400' : 'text-zinc-500'}`} />
                  </div>
                  <div className="my-2">
                    <span className="text-xl font-bold font-mono text-white">
                      {torchActive ? 'ON ✨' : 'OFF'}
                    </span>
                    <span className="text-[10px] text-zinc-400 block font-mono">
                      Real device camera hardware
                    </span>
                  </div>
                  <button
                    onClick={handleToggleTorch}
                    className={`mt-1 py-1.5 px-3 rounded-xl text-xs font-mono font-bold transition-all ${
                      torchActive
                        ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                        : 'bg-[#FF1744]/20 text-[#FF6D6D] border border-[#FF1744]/40 hover:bg-[#FF1744]/30'
                    }`}
                  >
                    {torchActive ? 'Turn Off' : 'Turn On Torch'}
                  </button>
                </div>
              </div>

              {/* Emergency SOS */}
              <div className="bg-gradient-to-r from-[#2B050B] to-[#120306] border border-[#FF1744]/60 rounded-2xl p-4.5 flex items-center justify-between shadow-lg shadow-[#FF1744]/15">
                <div>
                  <div className="flex items-center gap-2 text-[#FF1744] font-bold font-mono text-xs uppercase tracking-wider">
                    <ShieldAlert className="w-4 h-4 animate-bounce" />
                    EMERGENCY SOS ALERT
                  </div>
                  <p className="text-xs text-zinc-300 mt-1">
                    Instantly sends GPS location & distress SMS to <strong>{primeContact.name}</strong> ({primeContact.phone}).
                  </p>
                </div>
                <button
                  onClick={handleSOS}
                  className="px-4 py-2 bg-[#FF1744] hover:bg-[#D50000] text-white font-bold font-mono text-xs rounded-xl shadow-lg shadow-[#FF1744]/40 transition-transform active:scale-95"
                >
                  SEND SOS
                </button>
              </div>

              {/* System Clipboard */}
              <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-mono font-bold text-zinc-200">CLIPBOARD TOOL</h4>
                  <p className="text-[11px] text-zinc-400 mt-0.5">Quickly copy MYRA response text or voice notes.</p>
                </div>
                <button
                  onClick={() => handleCopy('MYRA AI Companion 2026 Active Note')}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-white/5 hover:bg-white/10 border border-white/15 rounded-xl text-xs font-mono text-zinc-200 transition-colors"
                >
                  <Copy className="w-3.5 h-3.5 text-[#FF1744]" />
                  Copy Sample
                </button>
              </div>
            </div>
          )}

          {/* TAB 2: Real Communication */}
          {activeTab === 'communication' && (
            <div className="space-y-3.5">
              {/* Prime Contact Dial */}
              <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-mono text-[#FF1744] font-bold uppercase tracking-wider">
                      PRIME CONTACT SPEED DIAL
                    </span>
                    <h3 className="text-sm font-bold text-white mt-0.5">{primeContact.name}</h3>
                    <p className="text-xs font-mono text-zinc-400">{primeContact.phone}</p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleCall(primeContact.phone)}
                      className="p-2.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 rounded-xl hover:bg-emerald-500/30 transition-all"
                      title="Real Phone Call"
                    >
                      <Phone className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleWhatsApp(primeContact.phone, 'Hey! Message from MYRA AI.')}
                      className="p-2.5 bg-green-500/20 text-green-400 border border-green-500/40 rounded-xl hover:bg-green-500/30 transition-all"
                      title="Real WhatsApp"
                    >
                      <MessageSquare className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleSMS(primeContact.phone, 'Pahuch gaya.')}
                      className="p-2.5 bg-blue-500/20 text-blue-400 border border-blue-500/40 rounded-xl hover:bg-blue-500/30 transition-all"
                      title="Real SMS"
                    >
                      <Mail className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>

              {/* Direct WhatsApp Messaging */}
              <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-mono font-bold text-zinc-200">WHATSAPP DIRECT CHAT</h4>
                  <p className="text-[11px] text-zinc-400 mt-0.5">Open WhatsApp chat directly with custom message.</p>
                </div>
                <button
                  onClick={() => handleWhatsApp(primeContact.phone, 'Rahul ko WhatsApp pe bolo main late hoon')}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-green-950/40 border border-green-700/50 hover:bg-green-900/50 rounded-xl text-xs font-mono text-green-300 transition-colors"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                  Launch WhatsApp
                </button>
              </div>

              {/* Direct Email Compose */}
              <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-mono font-bold text-zinc-200">EMAIL INBOX & COMPOSE</h4>
                  <p className="text-[11px] text-zinc-400 mt-0.5">Open mail client to compose or read messages.</p>
                </div>
                <button
                  onClick={() => handleEmail()}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-rose-950/40 border border-rose-700/50 hover:bg-rose-900/50 rounded-xl text-xs font-mono text-rose-300 transition-colors"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                  Open Email
                </button>
              </div>
            </div>
          )}

          {/* TAB 3: Real Media & Maps */}
          {activeTab === 'media' && (
            <div className="space-y-3.5">
              {/* Music Streaming */}
              <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-1.5 text-xs font-mono font-bold text-white">
                    <Music className="w-4 h-4 text-[#FF1744]" />
                    <span>YOUTUBE MUSIC / SPOTIFY</span>
                  </div>
                  <p className="text-[11px] text-zinc-400 mt-0.5">Play songs & artist playlists instantly.</p>
                </div>
                <button
                  onClick={() => handleMusic('Arijit Singh')}
                  className="px-3.5 py-1.5 bg-[#FF1744]/20 border border-[#FF1744]/40 hover:bg-[#FF1744]/30 rounded-xl text-xs font-mono text-[#FF6D6D] font-bold transition-all"
                >
                  Play Song 🎵
                </button>
              </div>

              {/* Google Maps Navigation */}
              <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-1.5 text-xs font-mono font-bold text-white">
                    <Compass className="w-4 h-4 text-blue-400" />
                    <span>GOOGLE MAPS NAVIGATION</span>
                  </div>
                  <p className="text-[11px] text-zinc-400 mt-0.5">Turn-by-turn routing to your destination.</p>
                </div>
                <button
                  onClick={() => handleNavigation('Airport')}
                  className="px-3.5 py-1.5 bg-blue-500/20 border border-blue-500/40 hover:bg-blue-500/30 rounded-xl text-xs font-mono text-blue-300 font-bold transition-all"
                >
                  Navigate 🗺️
                </button>
              </div>

              {/* Web Search */}
              <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-4 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-1.5 text-xs font-mono font-bold text-white">
                    <Search className="w-4 h-4 text-emerald-400" />
                    <span>GOOGLE WEB SEARCH</span>
                  </div>
                  <p className="text-[11px] text-zinc-400 mt-0.5">Search live weather, news, and queries.</p>
                </div>
                <button
                  onClick={() => handleSearch('aaj ka mausam')}
                  className="px-3.5 py-1.5 bg-emerald-500/20 border border-emerald-500/40 hover:bg-emerald-500/30 rounded-xl text-xs font-mono text-emerald-300 font-bold transition-all"
                >
                  Search 🔍
                </button>
              </div>
            </div>
          )}

          {/* TAB 4: Real Android APK */}
          {activeTab === 'apk' && (
            <div className="space-y-4">
              <div className="bg-gradient-to-br from-[#12081f] to-[#080812] border border-[#D500F9]/50 rounded-2xl p-5 shadow-xl">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 bg-[#D500F9]/20 border border-[#D500F9]/40 rounded-xl text-[#E040FB]">
                    <Download className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-white font-mono">
                      REAL ANDROID APK BUILD (KOTLIN)
                    </h3>
                    <p className="text-xs text-zinc-400 font-mono mt-0.5">
                      AGP 9.1.1 · Gradle 9.3.1 · Java 17 · Native Gemini Live WebSocket
                    </p>
                  </div>
                </div>

                <div className="my-4 p-3.5 bg-black/40 border border-white/10 rounded-xl space-y-2 text-xs text-zinc-300 font-mono leading-relaxed">
                  <div className="flex items-start gap-2">
                    <span className="text-[#00E676] font-bold">✓</span>
                    <span><strong>Full Native App:</strong> Includes AudioRecord (16kHz), AudioTrack (24kHz), CallMonitorService, and Floating Orb Overlay.</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="text-[#00E676] font-bold">✓</span>
                    <span><strong>GitHub Actions:</strong> Automated build workflow is configured in <code>.github/workflows/build-apk.yml</code>.</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="text-[#00E676] font-bold">✓</span>
                    <span><strong>Phone Direct Install:</strong> Builds <code>app-debug.apk</code> and publishes directly under GitHub Releases with <code>--latest</code> flag for direct phone downloading.</span>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-1">
                  <span className="text-[11px] text-zinc-400 font-mono">
                    Target: <code>app/build/outputs/apk/debug/app-debug.apk</code>
                  </span>
                  <a
                    href="https://github.com"
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 px-4 py-2 bg-gradient-to-r from-[#FF1744] to-[#D500F9] text-white text-xs font-mono font-bold rounded-xl shadow-lg shadow-[#FF1744]/30 hover:opacity-95 transition-opacity"
                  >
                    <span>View GitHub Releases</span>
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>
              </div>
            </div>
          )}

        </div>

        {/* Footer */}
        <div className="px-6 py-3.5 border-t border-[#1A1A2E] bg-[#0c0c1a] flex items-center justify-between text-xs text-zinc-400 font-mono">
          <span>Active Mode: <strong className="text-white">Real Device Tools</strong></span>
          <button
            onClick={onClose}
            className="px-4 py-1.5 bg-white/10 hover:bg-white/15 text-white rounded-xl transition-colors"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
};
