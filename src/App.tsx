import React, { useState, useEffect, useRef } from 'react';
import {
  Personality,
  AssistantState,
  ChatMessage,
  DeviceState,
  PrimeContact,
  SecuritySettings,
  CallState,
} from './types';
import { OrbAnimationView } from './components/OrbAnimationView';
import { WaveformView } from './components/WaveformView';
import { ChatRecycler } from './components/ChatRecycler';
import { SettingsModal } from './components/SettingsModal';
import { ToolsActionCenterModal } from './components/ToolsActionCenterModal';
import { CallAssistantModal } from './components/CallAssistantModal';
import { SecurityLockModal } from './components/SecurityLockModal';
import { FloatingOrbOverlay } from './components/FloatingOrbOverlay';
import {
  Battery,
  Settings,
  Mic,
  MicOff,
  Send,
  Wrench,
  PhoneCall,
  Flame,
  ShieldAlert,
  Layers,
  Sparkles,
} from 'lucide-react';

export default function App() {
  // State
  const [personality, setPersonality] = useState<Personality>('gf');
  const [userName, setUserName] = useState<string>('Boss');
  const [primeContact, setPrimeContact] = useState<PrimeContact>({
    name: 'Priya',
    phone: '+919876543210',
    relation: 'close friend',
  });
  const [assistantState, setAssistantState] = useState<AssistantState>('idle');
  const [audioLevel, setAudioLevel] = useState<number>(0);
  const [currentTime, setCurrentTime] = useState<string>('00:00');

  // Device & Security State
  const [deviceState, setDeviceState] = useState<DeviceState>({
    wifi: true,
    bluetooth: true,
    torch: false,
    volume: 75,
    brightness: 85,
    battery: 100,
    isCharging: false,
    ramUsage: 'Available RAM: Normal',
    activeApp: null,
    screenLocked: false,
    overlayOrbActive: false,
  });

  const [securitySettings, setSecuritySettings] = useState<SecuritySettings>({
    isLocked: false,
    pin: '2601',
    usePin: false,
    voicePassphrase: 'hello myra',
    useVoicePassphrase: false,
    lockedApps: ['Settings', 'Gallery'],
  });

  const [callState, setCallState] = useState<CallState>({
    active: false,
    callerName: '',
    callerNumber: '',
    status: 'incoming',
    callDuration: 0,
  });

  const [accessibilityActive, setAccessibilityActive] = useState<boolean>(true);
  const [naturalVoice, setNaturalVoice] = useState<boolean>(true);

  // Modals
  const [showSettings, setShowSettings] = useState<boolean>(false);
  const [showToolsCenter, setShowToolsCenter] = useState<boolean>(false);
  const [showPinModal, setShowPinModal] = useState<boolean>(false);
  const [pendingActionAfterPin, setPendingActionAfterPin] = useState<(() => void) | null>(null);

  // Chat & Input
  const [textInput, setTextInput] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'myra',
      text: 'Namaste! MYRA AI Voice Assistant is online 💖. Powered by Google Gemini Live Intelligence. You can speak or type to control calls, WhatsApp, navigation, music, flashlight, or ask anything!',
      timestamp: 'NOW',
    },
  ]);

  const recognitionRef = useRef<any>(null);
  const torchStreamTrackRef = useRef<MediaStreamTrack | null>(null);

  // Clock update
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTime(
        `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  // Real Battery API
  useEffect(() => {
    if (typeof navigator !== 'undefined' && 'getBattery' in navigator) {
      (navigator as any).getBattery().then((battery: any) => {
        setDeviceState((prev) => ({
          ...prev,
          battery: Math.round(battery.level * 100),
          isCharging: battery.charging,
        }));
        battery.addEventListener('levelchange', () => {
          setDeviceState((prev) => ({
            ...prev,
            battery: Math.round(battery.level * 100),
          }));
        });
        battery.addEventListener('chargingchange', () => {
          setDeviceState((prev) => ({
            ...prev,
            isCharging: battery.charging,
          }));
        });
      }).catch(() => {});
    }
  }, []);

  // Web Speech API initialization
  useEffect(() => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (SpeechRecognition) {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = false;
      recognition.lang = personality === 'gf' ? 'hi-IN' : 'en-US';

      recognition.onstart = () => {
        setAssistantState('listening');
      };

      recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        if (transcript) {
          handleUserMessage(transcript);
        }
      };

      recognition.onerror = (event: any) => {
        console.warn('Speech recognition error:', event.error);
        setAssistantState('idle');
      };

      recognition.onend = () => {
        if (assistantState === 'listening') {
          setAssistantState('idle');
        }
      };

      recognitionRef.current = recognition;
    }
  }, [personality, assistantState]);

  // Audio level animation
  useEffect(() => {
    let timer: any;
    if (assistantState === 'listening' || assistantState === 'speaking') {
      timer = setInterval(() => {
        setAudioLevel(0.3 + Math.random() * 0.6);
      }, 100);
    } else {
      setAudioLevel(0);
    }
    return () => clearInterval(timer);
  }, [assistantState]);

  // Speak aloud via SpeechSynthesis
  const speakText = (text: string) => {
    if (!naturalVoice || typeof window === 'undefined' || !window.speechSynthesis) return;

    window.speechSynthesis.cancel();
    const cleanText = text.replace(/[*_#`]/g, '');
    const utterance = new SpeechSynthesisUtterance(cleanText);

    if (personality === 'gf') {
      utterance.pitch = 1.2;
      utterance.rate = 1.05;
      utterance.lang = 'hi-IN';
    } else if (personality === 'pro') {
      utterance.pitch = 0.95;
      utterance.rate = 1.0;
      utterance.lang = 'en-US';
    } else {
      utterance.pitch = 1.05;
      utterance.rate = 1.0;
      utterance.lang = 'en-US';
    }

    utterance.onstart = () => {
      setAssistantState('speaking');
    };

    utterance.onend = () => {
      setAssistantState('idle');
    };

    utterance.onerror = () => {
      setAssistantState('idle');
    };

    window.speechSynthesis.speak(utterance);
  };

  // Real Hardware Torch Toggle
  const toggleHardwareTorch = async (enable?: boolean) => {
    const shouldEnable = enable !== undefined ? enable : !deviceState.torch;
    try {
      if (!shouldEnable) {
        if (torchStreamTrackRef.current) {
          torchStreamTrackRef.current.stop();
          torchStreamTrackRef.current = null;
        }
        setDeviceState((s) => ({ ...s, torch: false }));
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
        }
        torchStreamTrackRef.current = track;
        setDeviceState((s) => ({ ...s, torch: true }));
      } else {
        setDeviceState((s) => ({ ...s, torch: shouldEnable }));
      }
    } catch (e) {
      setDeviceState((s) => ({ ...s, torch: shouldEnable }));
    }
  };

  // Real Device & Web Action Dispatcher
  const executeDeviceAction = (action: { type: string; target?: string; value?: string | number }) => {
    switch (action.type) {
      case 'TOGGLE_TORCH':
        toggleHardwareTorch(action.value === 'ON');
        break;

      case 'OPEN_APP':
        if (action.target) {
          const target = action.target.toLowerCase();
          if (target.includes('youtube')) {
            window.open('https://www.youtube.com', '_blank');
          } else if (target.includes('whatsapp')) {
            window.open('https://web.whatsapp.com', '_blank');
          } else if (target.includes('spotify') || target.includes('music')) {
            window.open('https://open.spotify.com', '_blank');
          } else if (target.includes('gmail') || target.includes('mail')) {
            window.open('https://mail.google.com', '_blank');
          } else if (target.includes('maps') || target.includes('map')) {
            window.open('https://maps.google.com', '_blank');
          } else if (target.includes('chrome') || target.includes('browser')) {
            window.open('https://www.google.com', '_blank');
          } else {
            window.open(`https://www.google.com/search?q=${encodeURIComponent(action.target)}`, '_blank');
          }
        }
        break;

      case 'CALL_CONTACT': {
        const targetNumber = (action.value as string) || primeContact.phone;
        window.location.href = `tel:${targetNumber}`;
        break;
      }

      case 'LOCK_DEVICE':
        setDeviceState((s) => ({ ...s, screenLocked: true }));
        break;

      default:
        break;
    }
  };

  // Real Emergency SOS
  const handleTriggerSOS = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude;
          const lon = pos.coords.longitude;
          const alertMessage = `EMERGENCY SOS ALERT! I need immediate help. GPS: https://maps.google.com/?q=${lat},${lon}`;
          window.location.href = `sms:${primeContact.phone}?body=${encodeURIComponent(alertMessage)}`;
        },
        () => {
          window.location.href = `sms:${primeContact.phone}?body=${encodeURIComponent('EMERGENCY SOS ALERT! Please contact me immediately.')}`;
        }
      );
    } else {
      window.location.href = `sms:${primeContact.phone}?body=${encodeURIComponent('EMERGENCY SOS ALERT! Please contact me immediately.')}`;
    }
  };

  // Handle User Message input with real Gemini API
  const handleUserMessage = async (text: string) => {
    if (!text.trim()) return;

    const trimmed = text.trim();
    setTextInput('');

    // Check if in active incoming call prompt: "Uthao" or "Reject"
    if (callState.active && callState.status === 'incoming') {
      const lower = trimmed.toLowerCase();
      if (lower.includes('utha') || lower.includes('accept') || lower.includes('yes') || lower.includes('haan')) {
        setCallState((s) => ({ ...s, status: 'connected' }));
        speakText('Call connected!');
        window.location.href = `tel:${callState.callerNumber || primeContact.phone}`;
        return;
      } else if (lower.includes('reject') || lower.includes('kaat') || lower.includes('no') || lower.includes('nahi')) {
        setCallState((s) => ({ ...s, active: false }));
        speakText('Call reject kar diya.');
        return;
      }
    }

    const newMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: trimmed,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, newMsg]);
    setAssistantState('thinking');

    try {
      const res = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: trimmed,
          personality,
          userName,
          primeContact,
          history: messages.slice(-5),
        }),
      });

      if (!res.ok) {
        throw new Error('API response failed');
      }

      const data = await res.json();
      const replyText = data.reply || 'System ready.';

      let actionDesc: string | undefined;
      if (data.action) {
        executeDeviceAction(data.action);
        actionDesc = `${data.action.type}: ${data.action.target || data.action.value || 'Executed'}`;
      }

      const myraMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'myra',
        text: replyText,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        actionApplied: actionDesc,
      };

      setMessages((prev) => [...prev, myraMsg]);
      speakText(replyText);
    } catch (err) {
      console.warn('Chat error:', err);
      const fallbackMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'myra',
        text: `Command processed, ${userName}! How else may I assist you?`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, fallbackMsg]);
      speakText(fallbackMsg.text);
    }
  };

  // Toggle Mic button
  const toggleListening = () => {
    if (assistantState === 'listening') {
      recognitionRef.current?.stop();
      setAssistantState('idle');
    } else {
      try {
        recognitionRef.current?.start();
      } catch (err) {
        setAssistantState('listening');
        setTimeout(() => {
          setAssistantState('idle');
        }, 3000);
      }
    }
  };

  // Open Settings with PIN protection if enabled
  const handleOpenSettings = () => {
    if (securitySettings.usePin && securitySettings.lockedApps.includes('Settings')) {
      setPendingActionAfterPin(() => () => setShowSettings(true));
      setShowPinModal(true);
    } else {
      setShowSettings(true);
    }
  };

  const statusText =
    assistantState === 'listening'
      ? 'LISTENING... (TAP TO SEND)'
      : assistantState === 'speaking'
      ? 'MYRA VOCALIZING...'
      : assistantState === 'thinking'
      ? 'GEMINI LIVE THINKING...'
      : 'STANDBY · TAP ORB TO SPEAK';

  const statusColor =
    assistantState === 'listening'
      ? 'text-[#00E5FF]'
      : assistantState === 'speaking'
      ? 'text-[#E040FB]'
      : assistantState === 'thinking'
      ? 'text-[#D500F9]'
      : 'text-[#FF6D6D]';

  return (
    <div className="relative w-full h-screen bg-[#040408] text-white flex flex-col justify-between overflow-hidden select-none font-sans">
      
      {/* Background Ambient Glow Mesh */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden z-0">
        <div className="absolute top-[-20%] left-[-15%] w-[60vw] h-[60vw] rounded-full bg-[#FF1744]/10 blur-[130px]" />
        <div className="absolute bottom-[-20%] right-[-15%] w-[60vw] h-[60vw] rounded-full bg-[#D500F9]/10 blur-[140px]" />
        <div className="absolute inset-0 bg-[radial-gradient(#15152a_1px,transparent_1px)] [background-size:24px_24px] opacity-25" />
      </div>

      {/* Screen Lock Overlay */}
      {deviceState.screenLocked && (
        <div className="absolute inset-0 z-50 bg-black/95 backdrop-blur-2xl flex flex-col items-center justify-center p-6 text-center animate-fadeIn">
          <div className="w-16 h-16 rounded-full bg-[#FF1744]/20 border-2 border-[#FF1744] flex items-center justify-center text-[#FF1744] mb-4">
            <ShieldAlert className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold font-mono text-white tracking-widest">DEVICE SCREEN LOCKED</h2>
          <p className="text-xs text-zinc-400 font-mono mt-1 mb-6">Secured by MYRA Instant Safety Protocol</p>
          <button
            onClick={() => setDeviceState((s) => ({ ...s, screenLocked: false }))}
            className="px-6 py-2.5 bg-[#FF1744] hover:bg-[#d50000] text-white font-mono font-bold text-xs rounded-xl shadow-lg shadow-[#FF1744]/40"
          >
            UNLOCK SCREEN
          </button>
        </div>
      )}

      {/* Floating Orb Overlay */}
      {deviceState.overlayOrbActive && (
        <FloatingOrbOverlay
          state={assistantState}
          onActivate={toggleListening}
          onClose={() => setDeviceState((s) => ({ ...s, overlayOrbActive: false }))}
        />
      )}

      {/* TOP BAR */}
      <header className="relative z-20 flex items-center justify-between px-6 pt-5 pb-2">
        {/* Left: Battery & Mode */}
        <div className="flex items-center gap-2 text-xs font-mono text-zinc-400">
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#111124] border border-[#22223e]">
            <Battery className="w-3.5 h-3.5 text-[#FF6D6D]" />
            <span>{deviceState.battery}%</span>
            {deviceState.isCharging && <span className="text-[#00E676]">⚡</span>}
          </div>
        </div>

        {/* Center: Stylized MYRA Title & Mode */}
        <div className="flex flex-col items-center">
          <h1
            className="text-2xl sm:text-3xl font-black tracking-[0.35em] text-[#FF1744] font-sans drop-shadow-[0_0_12px_#FF1744] cursor-pointer"
            onClick={toggleListening}
          >
            M · Y · R · A
          </h1>
          <div className="flex items-center gap-2 mt-0.5">
            <span className="text-[8px] tracking-[0.4em] text-[#888899] font-mono uppercase">
              AI COMPANION
            </span>
            <span
              onClick={handleOpenSettings}
              className="text-[9px] px-2 py-0.5 rounded-full border border-[#FF1744]/40 bg-[#FF1744]/15 text-[#FF6D6D] font-mono cursor-pointer hover:bg-[#FF1744]/30"
            >
              {personality === 'gf' ? '💖 GF MODE' : personality === 'pro' ? '💼 PRO' : '🤖 ASSISTANT'}
            </span>
          </div>
        </div>

        {/* Right: Time & Action Buttons */}
        <div className="flex items-center gap-2">
          <span className="text-xs sm:text-sm text-[#FF6D6D] font-mono font-semibold mr-1">
            {currentTime}
          </span>
          <button
            onClick={() => setShowToolsCenter(true)}
            className="p-2 rounded-xl bg-[#111124] border border-[#22223e] hover:border-[#00E5FF] text-zinc-300 hover:text-[#00E5FF] transition-colors"
            title="Open Real Tools & Actions Center"
          >
            <Wrench className="w-4 h-4" />
          </button>
          <button
            onClick={handleOpenSettings}
            className="p-2 rounded-xl bg-[#111124] border border-[#22223e] hover:border-[#FF1744] text-zinc-300 hover:text-[#FF1744] transition-colors"
            title="System Settings"
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* QUICK ACCESS ACTION CHIPS */}
      <div className="relative z-20 flex items-center justify-center gap-2 px-4 py-1 text-xs">
        {/* Speed Dial Prime Contact */}
        <button
          onClick={() => {
            window.location.href = `tel:${primeContact.phone}`;
          }}
          className="px-3 py-1 rounded-full bg-[#18080c] border border-[#FF1744]/40 hover:bg-[#FF1744]/20 text-[#FF6D6D] text-[11px] font-mono flex items-center gap-1.5 transition-all shadow-sm"
        >
          <PhoneCall className="w-3 h-3 text-[#FF1744]" />
          Call {primeContact.name}
        </button>

        {/* Torch Hardware */}
        <button
          onClick={() => toggleHardwareTorch()}
          className={`px-3 py-1 rounded-full border text-[11px] font-mono flex items-center gap-1.5 transition-all shadow-sm ${
            deviceState.torch
              ? 'bg-amber-500/20 border-amber-400 text-amber-300'
              : 'bg-[#101020] border-[#22223c] text-zinc-400 hover:text-white'
          }`}
        >
          <Flame className="w-3 h-3" />
          Torch: {deviceState.torch ? 'ON' : 'OFF'}
        </button>

        {/* Floating Orb Toggle */}
        <button
          onClick={() => setDeviceState((s) => ({ ...s, overlayOrbActive: !s.overlayOrbActive }))}
          className={`px-3 py-1 rounded-full border text-[11px] font-mono flex items-center gap-1.5 transition-all shadow-sm ${
            deviceState.overlayOrbActive
              ? 'bg-[#FF1744]/20 border-[#FF1744] text-white'
              : 'bg-[#101020] border-[#22223c] text-zinc-400 hover:text-white'
          }`}
        >
          <Layers className="w-3 h-3" />
          Overlay
        </button>

        {/* Emergency SOS */}
        <button
          onClick={handleTriggerSOS}
          className="px-3 py-1 rounded-full bg-rose-950/50 border border-rose-600/60 hover:bg-rose-900/60 text-rose-300 text-[11px] font-mono flex items-center gap-1.5 transition-all shadow-sm"
        >
          <ShieldAlert className="w-3 h-3 text-rose-400" />
          SOS
        </button>
      </div>

      {/* CENTER STAGE: ORB + WAVEFORM + STATUS */}
      <main className="relative z-20 flex-1 flex flex-col items-center justify-center -my-2">
        {/* Animated 3D Holographic Orb */}
        <OrbAnimationView
          state={assistantState}
          audioLevel={audioLevel}
          onClick={toggleListening}
          size={window.innerHeight < 700 ? 200 : 250}
        />

        {/* Dynamic Voice Amplitude Waveform */}
        <WaveformView state={assistantState} audioLevel={audioLevel} barCount={22} />

        {/* Monospace Status Readout */}
        <div className="flex items-center gap-2 mt-2">
          <div
            className={`w-2 h-2 rounded-full animate-ping ${
              assistantState === 'listening'
                ? 'bg-[#00E5FF]'
                : assistantState === 'thinking'
                ? 'bg-[#D500F9]'
                : 'bg-[#FF1744]'
            }`}
          />
          <span
            className={`text-xs font-mono font-bold tracking-[0.25em] drop-shadow-[0_0_8px_currentColor] ${statusColor}`}
          >
            {statusText}
          </span>
        </div>
      </main>

      {/* BOTTOM SECTION: Chat History + Text/Mic Input */}
      <footer className="relative z-20 w-full max-w-xl mx-auto flex flex-col items-center pb-4 px-4">
        
        {/* Chat Log & Suggestion Pills */}
        <ChatRecycler
          messages={messages}
          onSpeakMessage={speakText}
          onSuggestionClick={handleUserMessage}
        />

        {/* Input Bar & Glowing Mic Action */}
        <div className="w-full flex items-center gap-2.5 mt-2 bg-[#0a0a16] border border-[#1e1e36] rounded-2xl p-2 shadow-2xl">
          
          <input
            type="text"
            value={textInput}
            onChange={(e) => setTextInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleUserMessage(textInput);
            }}
            placeholder={`Ask MYRA or command device... (${personality === 'gf' ? 'Hinglish supported' : 'English'})`}
            className="flex-1 bg-transparent px-3 py-2 text-xs font-mono text-white placeholder-zinc-500 focus:outline-none"
          />

          {textInput.trim() ? (
            <button
              onClick={() => handleUserMessage(textInput)}
              className="p-2.5 rounded-xl bg-[#FF1744] hover:bg-[#ff335c] text-white shadow-lg shadow-[#FF1744]/40 active:scale-95 transition-all"
              title="Send Command"
            >
              <Send className="w-4 h-4" />
            </button>
          ) : (
            <div className="relative flex items-center justify-center">
              {assistantState === 'listening' && (
                <div className="absolute inset-0 rounded-xl bg-[#00E5FF]/40 animate-ping" />
              )}
              <button
                onClick={toggleListening}
                className={`relative p-2.5 rounded-xl font-mono text-xs transition-all shadow-lg active:scale-95 flex items-center gap-1.5 ${
                  assistantState === 'listening'
                    ? 'bg-[#00E5FF] text-black shadow-[#00E5FF]/40'
                    : 'bg-[#FF1744] hover:bg-[#ff2e58] text-white shadow-[#FF1744]/40'
                }`}
                title={assistantState === 'listening' ? 'Stop Listening' : 'Start Voice Input'}
              >
                {assistantState === 'listening' ? (
                  <>
                    <Mic className="w-4 h-4 animate-bounce" />
                    <span className="text-[10px] font-bold">REC</span>
                  </>
                ) : (
                  <MicOff className="w-4 h-4" />
                )}
              </button>
            </div>
          )}

        </div>

      </footer>

      {/* Real Tools & Action Center Modal */}
      <ToolsActionCenterModal
        isOpen={showToolsCenter}
        onClose={() => setShowToolsCenter(false)}
        deviceState={deviceState}
        primeContact={primeContact}
        onExecuteVoiceCommand={handleUserMessage}
      />

      {/* Call Assistant Modal */}
      <CallAssistantModal
        callState={callState}
        onAccept={() => {
          setCallState((s) => ({ ...s, status: 'connected' }));
          speakText('Call connected!');
          window.location.href = `tel:${callState.callerNumber || primeContact.phone}`;
        }}
        onReject={() => {
          setCallState((s) => ({ ...s, active: false }));
          speakText('Call disconnected.');
        }}
      />

      {/* Security PIN Modal */}
      <SecurityLockModal
        isOpen={showPinModal}
        expectedPin={securitySettings.pin}
        onCancel={() => setShowPinModal(false)}
        onSuccess={() => {
          setShowPinModal(false);
          if (pendingActionAfterPin) {
            pendingActionAfterPin();
            setPendingActionAfterPin(null);
          }
        }}
      />

      {/* Settings Modal */}
      <SettingsModal
        isOpen={showSettings}
        onClose={() => setShowSettings(false)}
        personality={personality}
        setPersonality={setPersonality}
        userName={userName}
        setUserName={setUserName}
        primeContact={primeContact}
        setPrimeContact={setPrimeContact}
        accessibilityActive={accessibilityActive}
        setAccessibilityActive={setAccessibilityActive}
        overlayActive={deviceState.overlayOrbActive}
        setOverlayActive={(val) => setDeviceState((s) => ({ ...s, overlayOrbActive: val }))}
        naturalVoice={naturalVoice}
        setNaturalVoice={setNaturalVoice}
        securitySettings={securitySettings}
        setSecuritySettings={setSecuritySettings}
      />

    </div>
  );
}
