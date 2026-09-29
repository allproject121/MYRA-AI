import React from 'react';
import { Personality, PrimeContact, SecuritySettings } from '../types';
import { X, Sparkles, Phone, Shield, Volume2, User, Mic, Heart, Briefcase, Bot, Check, AlertTriangle } from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  personality: Personality;
  setPersonality: (p: Personality) => void;
  userName: string;
  setUserName: (n: string) => void;
  primeContact: PrimeContact;
  setPrimeContact: (c: PrimeContact) => void;
  accessibilityActive: boolean;
  setAccessibilityActive: (a: boolean) => void;
  overlayActive: boolean;
  setOverlayActive: (o: boolean) => void;
  naturalVoice: boolean;
  setNaturalVoice: (v: boolean) => void;
  securitySettings: SecuritySettings;
  setSecuritySettings: React.Dispatch<React.SetStateAction<SecuritySettings>>;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  personality,
  setPersonality,
  userName,
  setUserName,
  primeContact,
  setPrimeContact,
  accessibilityActive,
  setAccessibilityActive,
  overlayActive,
  setOverlayActive,
  naturalVoice,
  setNaturalVoice,
  securitySettings,
  setSecuritySettings,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-lg max-h-[90vh] bg-[#080812] border border-[#FF1744]/30 rounded-2xl shadow-2xl shadow-[#FF1744]/15 flex flex-col overflow-hidden text-sm">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#1A1A2E] bg-[#0c0c1a]">
          <div className="flex items-center gap-2.5">
            <div className="w-2.5 h-2.5 rounded-full bg-[#FF1744] shadow-[0_0_10px_#FF1744] animate-pulse" />
            <h2 className="text-base font-bold tracking-wider text-[#FF1744] font-mono">
              ⚙️ MYRA SYSTEM CONFIG
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-zinc-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">

          {/* Personality Selection */}
          <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-xl p-4">
            <div className="flex items-center gap-2 mb-3">
              <Sparkles className="w-4 h-4 text-[#FF1744]" />
              <label className="font-semibold text-zinc-200 tracking-wide text-xs uppercase font-mono">
                Personality Mode
              </label>
            </div>
            <div className="grid grid-cols-1 gap-2.5">
              {/* GF Mode */}
              <button
                type="button"
                onClick={() => setPersonality('gf')}
                className={`flex items-start gap-3 p-3 rounded-xl border text-left transition-all ${
                  personality === 'gf'
                    ? 'border-[#FF1744] bg-[#FF1744]/10 shadow-[0_0_15px_rgba(255,23,68,0.2)]'
                    : 'border-[#222238] bg-[#070712] hover:border-[#3d3d5c]'
                }`}
              >
                <div className={`p-2 rounded-lg ${personality === 'gf' ? 'bg-[#FF1744]/20 text-[#FF1744]' : 'bg-zinc-800 text-zinc-400'}`}>
                  <Heart className="w-5 h-5" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white text-xs">💖 GF Mode</span>
                    {personality === 'gf' && <span className="text-[10px] bg-[#FF1744] text-white px-1.5 py-0.5 rounded font-mono font-bold">ACTIVE</span>}
                  </div>
                  <p className="text-zinc-400 text-[11px] mt-1">
                    Caring, sweet, emotional, speaks fluent Hinglish (&quot;Haanji jaan, tension mat lo!&quot;)
                  </p>
                </div>
              </button>

              {/* Professional Mode */}
              <button
                type="button"
                onClick={() => setPersonality('pro')}
                className={`flex items-start gap-3 p-3 rounded-xl border text-left transition-all ${
                  personality === 'pro'
                    ? 'border-[#00E5FF] bg-[#00E5FF]/10 shadow-[0_0_15px_rgba(0,229,255,0.2)]'
                    : 'border-[#222238] bg-[#070712] hover:border-[#3d3d5c]'
                }`}
              >
                <div className={`p-2 rounded-lg ${personality === 'pro' ? 'bg-[#00E5FF]/20 text-[#00E5FF]' : 'bg-zinc-800 text-zinc-400'}`}>
                  <Briefcase className="w-5 h-5" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white text-xs">💼 Professional Mode</span>
                    {personality === 'pro' && <span className="text-[10px] bg-[#00E5FF] text-black px-1.5 py-0.5 rounded font-mono font-bold">ACTIVE</span>}
                  </div>
                  <p className="text-zinc-400 text-[11px] mt-1">
                    Concise, formal, corporate English co-pilot for high-efficiency daily tasks.
                  </p>
                </div>
              </button>

              {/* Assistant Mode */}
              <button
                type="button"
                onClick={() => setPersonality('assistant')}
                className={`flex items-start gap-3 p-3 rounded-xl border text-left transition-all ${
                  personality === 'assistant'
                    ? 'border-[#D500F9] bg-[#D500F9]/10 shadow-[0_0_15px_rgba(213,0,249,0.2)]'
                    : 'border-[#222238] bg-[#070712] hover:border-[#3d3d5c]'
                }`}
              >
                <div className={`p-2 rounded-lg ${personality === 'assistant' ? 'bg-[#D500F9]/20 text-[#D500F9]' : 'bg-zinc-800 text-zinc-400'}`}>
                  <Bot className="w-5 h-5" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white text-xs">🤖 Assistant Mode</span>
                    {personality === 'assistant' && <span className="text-[10px] bg-[#D500F9] text-white px-1.5 py-0.5 rounded font-mono font-bold">ACTIVE</span>}
                  </div>
                  <p className="text-zinc-400 text-[11px] mt-1">
                    Balanced, cheerful, tech-savvy cybernetic assistant ready for device automations.
                  </p>
                </div>
              </button>
            </div>
          </div>

          {/* User Name */}
          <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-xl p-4">
            <div className="flex items-center gap-2 mb-3">
              <User className="w-4 h-4 text-[#FF1744]" />
              <label className="font-semibold text-zinc-200 tracking-wide text-xs uppercase font-mono">
                User Nickname / Salutation
              </label>
            </div>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              placeholder="Sir, Roshan, Boss..."
              className="w-full bg-[#05050d] border border-[#262642] focus:border-[#FF1744] focus:outline-none rounded-lg px-3 py-2 text-white font-mono text-xs"
            />
          </div>

          {/* Prime Contact Card */}
          <div className="bg-[#14080b] border border-[#FF1744]/40 rounded-xl p-4 shadow-[0_0_20px_rgba(255,23,68,0.1)]">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Phone className="w-4 h-4 text-[#FF1744]" />
                <span className="font-bold text-[#FF1744] tracking-wide text-xs uppercase font-mono">
                  ⭐ Prime Contact (Speed Dial)
                </span>
              </div>
              <span className="bg-[#FF1744] text-white text-[9px] font-black px-2 py-0.5 rounded uppercase tracking-wider">
                PRIME
              </span>
            </div>
            <p className="text-zinc-400 text-xs mb-3">
              Trigger instant hands-free calls: say <span className="text-[#FF6D6D]">&quot;Call my close friend&quot;</span> or <span className="text-[#FF6D6D]">&quot;Call {primeContact.name}&quot;</span>.
            </p>

            <div className="space-y-2.5">
              <div>
                <label className="text-[11px] text-zinc-400 block mb-1">Contact Name / Nickname</label>
                <input
                  type="text"
                  value={primeContact.name}
                  onChange={(e) => setPrimeContact({ ...primeContact, name: e.target.value })}
                  placeholder="e.g. Priya / Roshan"
                  className="w-full bg-[#080305] border border-[#3d141a] focus:border-[#FF1744] focus:outline-none rounded-lg px-3 py-2 text-white text-xs font-mono"
                />
              </div>

              <div>
                <label className="text-[11px] text-zinc-400 block mb-1">Phone Number</label>
                <input
                  type="text"
                  value={primeContact.phone}
                  onChange={(e) => setPrimeContact({ ...primeContact, phone: e.target.value })}
                  placeholder="+91 98765 43210"
                  className="w-full bg-[#080305] border border-[#3d141a] focus:border-[#FF1744] focus:outline-none rounded-lg px-3 py-2 text-white text-xs font-mono"
                />
              </div>
            </div>
          </div>

          {/* Voice & TTS */}
          <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-xl p-4">
            <div className="flex items-center gap-2 mb-3">
              <Volume2 className="w-4 h-4 text-[#00E5FF]" />
              <label className="font-semibold text-zinc-200 tracking-wide text-xs uppercase font-mono">
                Speech & Audio Synthesis
              </label>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-[#1c1c38]">
              <div>
                <span className="text-xs font-medium text-white block">Natural Voice Feedback</span>
                <span className="text-[11px] text-zinc-400">Speak out responses with browser neural speech engine</span>
              </div>
              <button
                type="button"
                onClick={() => setNaturalVoice(!naturalVoice)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  naturalVoice ? 'bg-[#FF1744]' : 'bg-zinc-700'
                }`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    naturalVoice ? 'translate-x-6' : 'translate-x-1'
                  }`}
                />
              </button>
            </div>
          </div>

          {/* Permissions & Dynamic Automation Architecture */}
          <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-xl p-4">
            <div className="flex items-center gap-2 mb-3">
              <Shield className="w-4 h-4 text-[#FF1744]" />
              <label className="font-semibold text-zinc-200 tracking-wide text-xs uppercase font-mono">
                Automation &amp; Accessibility (Android Simulation)
              </label>
            </div>

            <div className="space-y-3">
              {/* Accessibility Service */}
              <div className="flex items-center justify-between p-2.5 bg-[#080814] rounded-lg border border-[#20203a]">
                <div className="pr-3">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-medium text-white">Accessibility Automation Service</span>
                    {accessibilityActive ? (
                      <span className="text-emerald-400 text-[10px] flex items-center gap-0.5"><Check className="w-3 h-3" /> ACTIVE</span>
                    ) : (
                      <span className="text-amber-400 text-[10px] flex items-center gap-0.5"><AlertTriangle className="w-3 h-3" /> OFF</span>
                    )}
                  </div>
                  <span className="text-[10px] text-zinc-400 block mt-0.5">
                    Enables dynamic AppDetector, ScreenMonitor, and ActionExecutor
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setAccessibilityActive(!accessibilityActive)}
                  className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors ${
                    accessibilityActive ? 'bg-emerald-500' : 'bg-zinc-700'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      accessibilityActive ? 'translate-x-6' : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>

              {/* Floating Overlay Orb */}
              <div className="flex items-center justify-between p-2.5 bg-[#080814] rounded-lg border border-[#20203a]">
                <div className="pr-3">
                  <span className="text-xs font-medium text-white block">Floating Orb Overlay</span>
                  <span className="text-[10px] text-zinc-400 block mt-0.5">
                    Display floating interactive orb over apps (Double-tap power trigger)
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setOverlayActive(!overlayActive)}
                  className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors ${
                    overlayActive ? 'bg-[#FF1744]' : 'bg-zinc-700'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      overlayActive ? 'translate-x-6' : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>

              {/* Security App Lock */}
              <div className="flex items-center justify-between p-2.5 bg-[#080814] rounded-lg border border-[#20203a]">
                <div className="pr-3">
                  <span className="text-xs font-medium text-white block">Security PIN Lock</span>
                  <span className="text-[10px] text-zinc-400 block mt-0.5">
                    Protect Settings and sensitive actions with 4-digit PIN (Default: 2601)
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setSecuritySettings(prev => ({ ...prev, usePin: !prev.usePin }))}
                  className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors ${
                    securitySettings.usePin ? 'bg-[#D500F9]' : 'bg-zinc-700'
                  }`}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                      securitySettings.usePin ? 'translate-x-6' : 'translate-x-1'
                    }`}
                  />
                </button>
              </div>
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-[#1A1A2E] bg-[#0c0c1a] flex justify-end">
          <button
            onClick={onClose}
            className="w-full py-2.5 px-4 bg-gradient-to-r from-[#FF1744] to-[#B71C1C] hover:from-[#ff335c] hover:to-[#d51a3a] text-white font-bold rounded-xl shadow-lg shadow-[#FF1744]/25 transition-all text-xs tracking-wider uppercase font-mono"
          >
            SAVE &amp; CLOSE
          </button>
        </div>

      </div>
    </div>
  );
};
