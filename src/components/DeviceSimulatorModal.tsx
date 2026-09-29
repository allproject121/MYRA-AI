import React, { useState } from 'react';
import { DeviceState } from '../types';
import {
  X,
  Wifi,
  WifiOff,
  Bluetooth,
  Flashlight,
  Volume2,
  VolumeX,
  Lock,
  Unlock,
  Smartphone,
  Play,
  MessageSquare,
  Camera,
  MapPin,
  Music,
  Globe,
  Mail,
  Image as ImageIcon,
  Cpu,
  Terminal,
  Activity,
} from 'lucide-react';

interface DeviceSimulatorProps {
  isOpen: boolean;
  onClose: () => void;
  deviceState: DeviceState;
  setDeviceState: React.Dispatch<React.SetStateAction<DeviceState>>;
  onLaunchApp: (appName: string) => void;
  logs: string[];
}

export const DeviceSimulatorModal: React.FC<DeviceSimulatorProps> = ({
  isOpen,
  onClose,
  deviceState,
  setDeviceState,
  onLaunchApp,
  logs,
}) => {
  const [activeTab, setActiveTab] = useState<'apps' | 'telemetry' | 'hardware'>('apps');

  if (!isOpen) return null;

  const apps = [
    { name: 'YouTube', icon: Play, color: 'text-red-500 bg-red-950/40 border-red-800/50' },
    { name: 'WhatsApp', icon: MessageSquare, color: 'text-green-500 bg-green-950/40 border-green-800/50' },
    { name: 'Camera', icon: Camera, color: 'text-amber-400 bg-amber-950/40 border-amber-800/50' },
    { name: 'Maps', icon: MapPin, color: 'text-blue-400 bg-blue-950/40 border-blue-800/50' },
    { name: 'Spotify', icon: Music, color: 'text-emerald-400 bg-emerald-950/40 border-emerald-800/50' },
    { name: 'Chrome', icon: Globe, color: 'text-yellow-400 bg-yellow-950/40 border-yellow-800/50' },
    { name: 'Gmail', icon: Mail, color: 'text-rose-400 bg-rose-950/40 border-rose-800/50' },
    { name: 'Gallery', icon: ImageIcon, color: 'text-purple-400 bg-purple-950/40 border-purple-800/50' },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-xl max-h-[90vh] bg-[#070710] border border-[#00E5FF]/30 rounded-2xl shadow-2xl shadow-[#00E5FF]/10 flex flex-col overflow-hidden text-sm">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#16162a] bg-[#0a0a16]">
          <div className="flex items-center gap-2.5">
            <Smartphone className="w-5 h-5 text-[#00E5FF]" />
            <div>
              <h2 className="text-sm font-bold tracking-wider text-[#00E5FF] font-mono">
                DEVICE ENVIRONMENT &amp; AUTOMATION
              </h2>
              <span className="text-[10px] text-zinc-400 font-mono">
                Active App: <strong className="text-white">{deviceState.activeApp || 'MYRA Home'}</strong>
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-zinc-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-[#16162a] bg-[#06060e] text-xs font-mono">
          <button
            onClick={() => setActiveTab('apps')}
            className={`flex-1 py-3 px-4 border-b-2 text-center transition-colors flex items-center justify-center gap-2 ${
              activeTab === 'apps'
                ? 'border-[#00E5FF] text-[#00E5FF] bg-[#00E5FF]/5 font-bold'
                : 'border-transparent text-zinc-400 hover:text-white'
            }`}
          >
            <Smartphone className="w-4 h-4" /> Installed Apps
          </button>
          <button
            onClick={() => setActiveTab('hardware')}
            className={`flex-1 py-3 px-4 border-b-2 text-center transition-colors flex items-center justify-center gap-2 ${
              activeTab === 'hardware'
                ? 'border-[#00E5FF] text-[#00E5FF] bg-[#00E5FF]/5 font-bold'
                : 'border-transparent text-zinc-400 hover:text-white'
            }`}
          >
            <Cpu className="w-4 h-4" /> Hardware &amp; Quick Controls
          </button>
          <button
            onClick={() => setActiveTab('telemetry')}
            className={`flex-1 py-3 px-4 border-b-2 text-center transition-colors flex items-center justify-center gap-2 ${
              activeTab === 'telemetry'
                ? 'border-[#00E5FF] text-[#00E5FF] bg-[#00E5FF]/5 font-bold'
                : 'border-transparent text-zinc-400 hover:text-white'
            }`}
          >
            <Terminal className="w-4 h-4" /> Automation Engine Logs
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === 'apps' && (
            <div className="space-y-4">
              <div className="bg-[#0b0b18] p-3 rounded-xl border border-[#1a1a36] flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-zinc-200 block">Dynamic AppDetector.kt</span>
                  <span className="text-[11px] text-zinc-400">
                    Fuzzy matching intent launcher without hardcoded package indices
                  </span>
                </div>
                <span className="text-[10px] bg-emerald-950 text-emerald-300 border border-emerald-800 px-2 py-0.5 rounded font-mono font-bold">
                  8 APPS SYNCED
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                {apps.map((app) => {
                  const Icon = app.icon;
                  const isActive = deviceState.activeApp === app.name;
                  return (
                    <button
                      key={app.name}
                      onClick={() => onLaunchApp(app.name)}
                      className={`flex flex-col items-center justify-center p-4 rounded-xl border transition-all ${app.color} ${
                        isActive
                          ? 'ring-2 ring-[#00E5FF] scale-105 shadow-[0_0_15px_rgba(0,229,255,0.4)]'
                          : 'hover:scale-102 hover:border-white/30'
                      }`}
                    >
                      <Icon className="w-7 h-7 mb-2" />
                      <span className="text-xs font-bold text-white">{app.name}</span>
                      <span className="text-[9px] text-zinc-400 font-mono mt-0.5">
                        {isActive ? 'CURRENT' : 'LAUNCH'}
                      </span>
                    </button>
                  );
                })}
              </div>

              {deviceState.activeApp && (
                <div className="mt-4 p-4 rounded-xl bg-[#0e0e22] border border-[#232348] flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
                    <div>
                      <span className="text-xs font-bold text-white block">
                        Running: {deviceState.activeApp}
                      </span>
                      <span className="text-[11px] text-zinc-400">
                        ScreenVisionAnalyzer: 14 clickable nodes identified
                      </span>
                    </div>
                  </div>
                  <button
                    onClick={() => onLaunchApp('MYRA')}
                    className="px-3 py-1.5 bg-[#FF1744]/20 border border-[#FF1744] hover:bg-[#FF1744] text-white text-xs font-mono rounded-lg transition-colors"
                  >
                    Return to MYRA
                  </button>
                </div>
              )}
            </div>
          )}

          {activeTab === 'hardware' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                {/* Wi-Fi */}
                <button
                  onClick={() => setDeviceState((s) => ({ ...s, wifi: !s.wifi }))}
                  className={`p-4 rounded-xl border flex items-center gap-3 transition-all ${
                    deviceState.wifi
                      ? 'border-[#00E5FF] bg-[#00E5FF]/10 text-white'
                      : 'border-zinc-800 bg-zinc-900/40 text-zinc-400'
                  }`}
                >
                  {deviceState.wifi ? <Wifi className="w-6 h-6 text-[#00E5FF]" /> : <WifiOff className="w-6 h-6" />}
                  <div className="text-left">
                    <span className="text-xs font-bold block">Wi-Fi</span>
                    <span className="text-[10px] font-mono">{deviceState.wifi ? 'CONNECTED' : 'OFF'}</span>
                  </div>
                </button>

                {/* Bluetooth */}
                <button
                  onClick={() => setDeviceState((s) => ({ ...s, bluetooth: !s.bluetooth }))}
                  className={`p-4 rounded-xl border flex items-center gap-3 transition-all ${
                    deviceState.bluetooth
                      ? 'border-indigo-500 bg-indigo-950/40 text-white'
                      : 'border-zinc-800 bg-zinc-900/40 text-zinc-400'
                  }`}
                >
                  <Bluetooth className={`w-6 h-6 ${deviceState.bluetooth ? 'text-indigo-400' : ''}`} />
                  <div className="text-left">
                    <span className="text-xs font-bold block">Bluetooth</span>
                    <span className="text-[10px] font-mono">{deviceState.bluetooth ? 'ACTIVE' : 'OFF'}</span>
                  </div>
                </button>

                {/* Torch / Flashlight */}
                <button
                  onClick={() => setDeviceState((s) => ({ ...s, torch: !s.torch }))}
                  className={`p-4 rounded-xl border flex items-center gap-3 transition-all ${
                    deviceState.torch
                      ? 'border-amber-400 bg-amber-950/40 text-white shadow-[0_0_20px_rgba(251,191,36,0.3)]'
                      : 'border-zinc-800 bg-zinc-900/40 text-zinc-400'
                  }`}
                >
                  <Flashlight className={`w-6 h-6 ${deviceState.torch ? 'text-amber-400' : ''}`} />
                  <div className="text-left">
                    <span className="text-xs font-bold block">Torch / Flashlight</span>
                    <span className="text-[10px] font-mono">{deviceState.torch ? 'BEAM ON' : 'OFF'}</span>
                  </div>
                </button>

                {/* Screen Lock */}
                <button
                  onClick={() => setDeviceState((s) => ({ ...s, screenLocked: !s.screenLocked }))}
                  className={`p-4 rounded-xl border flex items-center gap-3 transition-all ${
                    deviceState.screenLocked
                      ? 'border-rose-500 bg-rose-950/40 text-white'
                      : 'border-zinc-800 bg-zinc-900/40 text-zinc-400'
                  }`}
                >
                  {deviceState.screenLocked ? (
                    <Lock className="w-6 h-6 text-rose-400" />
                  ) : (
                    <Unlock className="w-6 h-6 text-emerald-400" />
                  )}
                  <div className="text-left">
                    <span className="text-xs font-bold block">Screen Lock</span>
                    <span className="text-[10px] font-mono">{deviceState.screenLocked ? 'LOCKED' : 'UNLOCKED'}</span>
                  </div>
                </button>
              </div>

              {/* Volume Slider */}
              <div className="p-4 rounded-xl bg-[#0d0d1e] border border-[#1d1d3a]">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    {deviceState.volume === 0 ? (
                      <VolumeX className="w-4 h-4 text-zinc-400" />
                    ) : (
                      <Volume2 className="w-4 h-4 text-[#00E5FF]" />
                    )}
                    <span className="text-xs font-bold text-white">Media Volume</span>
                  </div>
                  <span className="text-xs font-mono text-[#00E5FF] font-bold">{deviceState.volume}%</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={deviceState.volume}
                  onChange={(e) =>
                    setDeviceState((s) => ({ ...s, volume: parseInt(e.target.value, 10) }))
                  }
                  className="w-full accent-[#00E5FF] cursor-pointer"
                />
              </div>

              {/* Memory & Battery */}
              <div className="grid grid-cols-2 gap-3 text-xs font-mono">
                <div className="p-3 bg-[#0a0a14] rounded-xl border border-zinc-800">
                  <span className="text-zinc-400 block text-[10px]">BATTERY</span>
                  <span className="text-emerald-400 text-sm font-bold">{deviceState.battery}% Charged</span>
                </div>
                <div className="p-3 bg-[#0a0a14] rounded-xl border border-zinc-800">
                  <span className="text-zinc-400 block text-[10px]">RAM FOOTPRINT</span>
                  <span className="text-[#00E5FF] text-sm font-bold">{deviceState.ramUsage}</span>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'telemetry' && (
            <div className="space-y-3 font-mono">
              <div className="flex items-center justify-between text-xs text-zinc-400 pb-1 border-b border-zinc-800">
                <span className="flex items-center gap-1.5 text-[#00E5FF]">
                  <Activity className="w-3.5 h-3.5" /> Dynamic Automation Pipeline
                </span>
                <span className="text-[10px]">REAL-TIME STREAM</span>
              </div>
              <div className="bg-[#030307] border border-zinc-800 rounded-xl p-3 h-64 overflow-y-auto space-y-1.5 text-[11px]">
                {logs.length === 0 ? (
                  <div className="text-zinc-500 py-6 text-center">
                    Awaiting commands... Say &quot;Torch on karo&quot;, &quot;Open WhatsApp&quot;, or &quot;Call my close friend&quot;.
                  </div>
                ) : (
                  logs.map((log, index) => (
                    <div key={index} className="flex gap-2">
                      <span className="text-[#FF1744] select-none">&gt;</span>
                      <span className="text-zinc-300">{log}</span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-[#16162a] bg-[#0a0a16] flex justify-end">
          <button
            onClick={onClose}
            className="py-2 px-5 bg-zinc-800 hover:bg-zinc-700 text-white rounded-lg text-xs font-mono font-bold transition-colors"
          >
            DISMISS
          </button>
        </div>

      </div>
    </div>
  );
};
