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
import { DeviceSimulatorModal } from './components/DeviceSimulatorModal';
import { CallAssistantModal } from './components/CallAssistantModal';
import { SecurityLockModal } from './components/SecurityLockModal';
import { FloatingOrbOverlay } from './components/FloatingOrbOverlay';
import {
  Battery,
  Settings,
  Mic,
  MicOff,
  Send,
  Smartphone,
  PhoneCall,
  Flame,
  Volume2,
  Sparkles,
  Shield,
  Layers,
} from 'lucide-react';

export default function App() {
  // State
  const [personality, setPersonality] = useState<Personality>('gf');
  const [userName, setUserName] = useState<string>('Sir');
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
    battery: 98,
    isCharging: false,
    ramUsage: '2.1 GB / 8 GB',
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
  const [showDeviceSim, setShowDeviceSim] = useState<boolean>(false);
  const [showPinModal, setShowPinModal] = useState<boolean>(false);
  const [pendingActionAfterPin, setPendingActionAfterPin] = useState<(() => void) | null>(null);

  // Chat & Input
  const [textInput, setTextInput] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'myra',
      text: 'Namaste Sir! MYRA AI Companion initialized. GF Mode active 💖. Say "YouTube kholo", "Torch on karo", or "Mere close friend ko call karo"!',
      timestamp: 'NOW',
    },
  ]);

  const [automationLogs, setAutomationLogs] = useState<string[]>([
    'AutomationManager: initialized with lifecycleScope',
    'AppDetector: 8 user applications mapped dynamically',
    'ScreenMonitor: real-time accessibility event listener active',
    'ActionExecutor: multi-method intent parser online',
  ]);

  const recognitionRef = useRef<any>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const animFrameRef = useRef<number | null>(null);

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
        addLog('Microphone listening: audio stream active');
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

  // Audio level simulation / mic analysis
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

  // Add telemetry log
  const addLog = (text: string) => {
    const timeStr = new Date().toLocaleTimeString();
    setAutomationLogs((prev) => [`[${timeStr}] ${text}`, ...prev.slice(0, 40)]);
  };

  // Speak aloud via SpeechSynthesis
  const speakText = (text: string) => {
    if (!naturalVoice || typeof window === 'undefined' || !window.speechSynthesis) return;

    window.speechSynthesis.cancel();
    const cleanText = text.replace(/[*_#`]/g, '');
    const utterance = new SpeechSynthesisUtterance(cleanText);

    if (personality === 'gf') {
      utterance.pitch = 1.25;
      utterance.rate = 1.05;
      utterance.lang = 'hi-IN';
    } else if (personality === 'pro') {
      utterance.pitch = 0.95;
      utterance.rate = 1.0;
      utterance.lang = 'en-US';
    } else {
      utterance.pitch = 1.1;
      utterance.rate = 1.02;
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

  // Execute device actions triggered by voice or AI
  const executeDeviceAction = (action: { type: string; target?: string; value?: string | number }) => {
    addLog(`ActionExecutor: dispatching action ${action.type} -> ${action.target || action.value || ''}`);

    switch (action.type) {
      case 'TOGGLE_TORCH':
        setDeviceState((s) => ({ ...s, torch: action.value === 'ON' ? true : action.value === 'OFF' ? false : !s.torch }));
        break;
      case 'TOGGLE_WIFI':
        setDeviceState((s) => ({ ...s, wifi: action.value === 'ON' ? true : action.value === 'OFF' ? false : !s.wifi }));
        break;
      case 'TOGGLE_BLUETOOTH':
        setDeviceState((s) => ({ ...s, bluetooth: action.value === 'ON' ? true : action.value === 'OFF' ? false : !s.bluetooth }));
        break;
      case 'SET_VOLUME':
        setDeviceState((s) => {
          let newVol = s.volume;
          if (typeof action.value === 'string' && action.value.startsWith('+')) {
            newVol = Math.min(100, s.volume + 15);
          } else if (typeof action.value === 'string' && action.value.startsWith('-')) {
            newVol = Math.max(0, s.volume - 15);
          } else if (typeof action.value === 'number') {
            newVol = action.value;
          }
          return { ...s, volume: newVol };
        });
        break;
      case 'OPEN_APP':
        if (action.target) {
          setDeviceState((s) => ({ ...s, activeApp: action.target || null }));
          setShowDeviceSim(true);
        }
        break;
      case 'CALL_CONTACT':
        triggerCall(action.target || primeContact.name, (action.value as string) || primeContact.phone);
        break;
      case 'LOCK_DEVICE':
        setDeviceState((s) => ({ ...s, screenLocked: true }));
        break;
      default:
        break;
    }
  };

  // Trigger Incoming or Outgoing Call
  const triggerCall = (name: string, phone: string, isIncoming = false) => {
    setCallState({
      active: true,
      callerName: name,
      callerNumber: phone,
      status: isIncoming ? 'incoming' : 'connected',
      callDuration: 0,
    });

    if (isIncoming) {
      const callNotice = `Sir, ${name} ka call aa raha hai... uthau ya reject karu?`;
      speakText(callNotice);
      addLog(`CallMonitorService: Incoming call from ${name} (${phone})`);
    } else {
      const callNotice = `Calling ${name}...`;
      speakText(callNotice);
      addLog(`CallAssistant: Initiated call to ${name}`);
    }
  };

  // Handle User Message input
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
        addLog('CallAssistant: Voice accepted incoming call');
        return;
      } else if (lower.includes('reject') || lower.includes('kaat') || lower.includes('no') || lower.includes('nahi')) {
        setCallState((s) => ({ ...s, active: false }));
        speakText('Call reject kar diya.');
        addLog('CallAssistant: Voice rejected incoming call');
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
    addLog(`CommandProcessor: received "${trimmed}"`);

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
        // Fallback for browsers without speech recognition support
        setAssistantState('listening');
        setTimeout(() => {
          setAssistantState('idle');
        }, 3000);
      }
    }
  };

  // Protected settings click
  const handleOpenSettings = () => {
    if (securitySettings.usePin) {
      setPendingActionAfterPin(() => () => setShowSettings(true));
      setShowPinModal(true);
    } else {
      setShowSettings(true);
    }
  };

  // Status text label
  let statusText = 'SYSTEM READY';
  let statusColor = 'text-[#FF1744]';
  if (assistantState === 'listening') {
    statusText = 'LISTENING…';
    statusColor = 'text-[#00E5FF]';
  } else if (assistantState === 'thinking') {
    statusText = 'THINKING…';
    statusColor = 'text-[#D500F9]';
  } else if (assistantState === 'speaking') {
    statusText = 'SPEAKING…';
    statusColor = 'text-[#FF1744]';
  }

  return (
    <div className="relative w-screen h-screen bg-[#040408] text-[#E8E8FF] flex flex-col justify-between overflow-hidden select-none">
      
      {/* Flashlight Screen Beam Effect when Torch is ON */}
      {deviceState.torch && (
        <div className="absolute inset-0 z-10 pointer-events-none bg-gradient-to-b from-amber-100/25 via-amber-200/10 to-transparent shadow-[inset_0_0_100px_rgba(251,191,36,0.3)] animate-pulse" />
      )}

      {/* Screen Locked Simulation */}
      {deviceState.screenLocked && (
        <div className="absolute inset-0 z-50 bg-black/95 flex flex-col items-center justify-center p-6 text-center animate-fadeIn">
          <Shield className="w-16 h-16 text-[#FF1744] mb-4 animate-bounce" />
          <h2 className="text-xl font-bold font-mono tracking-widest text-white mb-2">
            MYRA SECURED LOCKSCREEN
          </h2>
          <p className="text-xs text-zinc-400 font-mono mb-6">
            Device locked via Myra DeviceAdminReceiver
          </p>
          <button
            onClick={() => setDeviceState((s) => ({ ...s, screenLocked: false }))}
            className="px-6 py-2.5 bg-[#FF1744] hover:bg-[#ff335c] text-white font-mono text-xs font-bold rounded-xl shadow-lg shadow-[#FF1744]/40"
          >
            TAP TO UNLOCK
          </button>
        </div>
      )}

      {/* Red Active Tint Overlay */}
      <div
        className={`absolute inset-0 pointer-events-none transition-opacity duration-500 bg-[#FF1744] ${
          assistantState === 'speaking' || assistantState === 'listening' ? 'opacity-[0.04]' : 'opacity-0'
        }`}
      />

      {/* Ambient background mesh gradient */}
      <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(circle_at_50%_40%,_rgba(255,23,68,0.12)_0%,_rgba(213,0,249,0.05)_40%,_rgba(4,4,8,0.95)_75%)]" />

      {/* TOP SYSTEM BAR */}
      <header className="relative z-20 flex items-center justify-between px-6 pt-5 pb-3">
        
        {/* Left: Battery & RAM */}
        <div className="flex flex-col gap-0.5 min-w-[70px]">
          <div className="flex items-center gap-1.5 text-xs text-[#FF6D6D] font-mono font-medium">
            <Battery className="w-3.5 h-3.5 text-[#FF6D6D]" />
            <span>{deviceState.battery}%</span>
          </div>
          <span className="text-[10px] text-[#444466] font-mono tracking-tight">
            {deviceState.ramUsage.split('/')[0].trim()}
          </span>
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
              className="text-[9px] px-1.5 py-0.2 rounded-full border border-[#FF1744]/40 bg-[#FF1744]/15 text-[#FF6D6D] font-mono cursor-pointer hover:bg-[#FF1744]/30"
            >
              {personality === 'gf' ? '💖 GF' : personality === 'pro' ? '💼 PRO' : '🤖 ASSIST'}
            </span>
          </div>
        </div>

        {/* Right: Time & Action Buttons */}
        <div className="flex items-center gap-2.5">
          <span className="text-xs sm:text-sm text-[#FF6D6D] font-mono font-semibold">
            {currentTime}
          </span>
          <button
            onClick={() => setShowDeviceSim(true)}
            className="p-1.5 rounded-lg bg-[#111124] border border-[#22223e] hover:border-[#00E5FF] text-zinc-300 hover:text-[#00E5FF] transition-colors"
            title="Open Device & Automation Center"
          >
            <Smartphone className="w-4 h-4" />
          </button>
          <button
            onClick={handleOpenSettings}
            className="p-1.5 rounded-lg bg-[#111124] border border-[#22223e] hover:border-[#FF1744] text-zinc-300 hover:text-[#FF1744] transition-colors"
            title="System Settings"
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>

      </header>

      {/* Quick Access Quick Action Chips Bar */}
      <div className="relative z-20 flex items-center justify-center gap-2 px-4 py-1 text-xs">
        <button
          onClick={() => triggerCall(primeContact.name, primeContact.phone, true)}
          className="px-2.5 py-1 rounded-full bg-[#18080c] border border-[#FF1744]/40 hover:bg-[#FF1744]/20 text-[#FF6D6D] text-[10px] font-mono flex items-center gap-1.5 transition-all shadow-sm"
        >
          <PhoneCall className="w-3 h-3 text-[#FF1744]" />
          Simulate Incoming Call
        </button>
        <button
          onClick={() => setDeviceState((s) => ({ ...s, torch: !s.torch }))}
          className={`px-2.5 py-1 rounded-full border text-[10px] font-mono flex items-center gap-1.5 transition-all shadow-sm ${
            deviceState.torch
              ? 'bg-amber-500/20 border-amber-400 text-amber-300'
              : 'bg-[#101020] border-[#22223c] text-zinc-400 hover:text-white'
          }`}
        >
          <Flame className="w-3 h-3" />
          Torch: {deviceState.torch ? 'ON' : 'OFF'}
        </button>
        <button
          onClick={() => setDeviceState((s) => ({ ...s, overlayOrbActive: !s.overlayOrbActive }))}
          className={`px-2.5 py-1 rounded-full border text-[10px] font-mono flex items-center gap-1.5 transition-all shadow-sm ${
            deviceState.overlayOrbActive
              ? 'bg-[#FF1744]/20 border-[#FF1744] text-white'
              : 'bg-[#101020] border-[#22223c] text-zinc-400 hover:text-white'
          }`}
        >
          <Layers className="w-3 h-3" />
          Floating Orb: {deviceState.overlayOrbActive ? 'ACTIVE' : 'OFF'}
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
              {/* Outer Glow Ring */}
              <div
                className={`absolute -inset-1 rounded-full blur transition-all ${
                  assistantState === 'listening'
                    ? 'bg-[#00E5FF] opacity-90 scale-125'
                    : 'bg-[#FF1744] opacity-40 hover:opacity-80'
                }`}
              />
              <button
                onClick={toggleListening}
                className={`relative p-3 rounded-full text-white shadow-xl active:scale-95 transition-all flex items-center justify-center ${
                  assistantState === 'listening'
                    ? 'bg-[#00E5FF] text-black shadow-[#00E5FF]/60'
                    : 'bg-gradient-to-tr from-[#FF1744] to-[#B71C1C] shadow-[#FF1744]/50 hover:brightness-110'
                }`}
                title="Tap to speak with MYRA"
              >
                {assistantState === 'listening' ? (
                  <MicOff className="w-5 h-5 animate-pulse" />
                ) : (
                  <Mic className="w-5 h-5" />
                )}
              </button>
            </div>
          )}

        </div>

        <span className="text-[9px] text-[#444466] font-mono mt-1 tracking-wider uppercase">
          Tap mic or orb to speak · Voice Assistant Ready
        </span>

      </footer>

      {/* Floating Draggable Orb Overlay (simulating Android MyraOverlayService.kt) */}
      {deviceState.overlayOrbActive && (
        <FloatingOrbOverlay
          state={assistantState}
          onActivate={toggleListening}
          onClose={() => setDeviceState((s) => ({ ...s, overlayOrbActive: false }))}
        />
      )}

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
        setOverlayActive={(o) => setDeviceState((s) => ({ ...s, overlayOrbActive: o }))}
        naturalVoice={naturalVoice}
        setNaturalVoice={setNaturalVoice}
        securitySettings={securitySettings}
        setSecuritySettings={setSecuritySettings}
      />

      {/* Device Simulator & Automation Sheet */}
      <DeviceSimulatorModal
        isOpen={showDeviceSim}
        onClose={() => setShowDeviceSim(false)}
        deviceState={deviceState}
        setDeviceState={setDeviceState}
        onLaunchApp={(app) => {
          setDeviceState((s) => ({ ...s, activeApp: app }));
          addLog(`AppDetector: launch intent dispatched for [${app}]`);
        }}
        logs={automationLogs}
      />

      {/* Call Assistant Simulation Modal */}
      <CallAssistantModal
        callState={callState}
        onAccept={() => {
          setCallState((s) => ({ ...s, status: 'connected' }));
          speakText('Call connected!');
          addLog('CallAssistant: Call accepted');
        }}
        onReject={() => {
          setCallState((s) => ({ ...s, active: false }));
          speakText('Call ended.');
          addLog('CallAssistant: Call terminated');
        }}
      />

      {/* Security PIN Lock Modal */}
      <SecurityLockModal
        isOpen={showPinModal}
        expectedPin={securitySettings.pin}
        onSuccess={() => {
          setShowPinModal(false);
          if (pendingActionAfterPin) {
            pendingActionAfterPin();
            setPendingActionAfterPin(null);
          }
        }}
        onCancel={() => {
          setShowPinModal(false);
          setPendingActionAfterPin(null);
        }}
      />

    </div>
  );
}
