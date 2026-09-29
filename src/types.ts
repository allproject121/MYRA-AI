export type Personality = 'gf' | 'pro' | 'assistant';

export type AssistantState = 'idle' | 'listening' | 'thinking' | 'speaking';

export interface ChatMessage {
  id: string;
  sender: 'user' | 'myra';
  text: string;
  timestamp: string;
  actionApplied?: string;
}

export interface DeviceState {
  wifi: boolean;
  bluetooth: boolean;
  torch: boolean;
  volume: number; // 0-100
  brightness: number;
  battery: number;
  isCharging: boolean;
  ramUsage: string;
  activeApp: string | null;
  screenLocked: boolean;
  overlayOrbActive: boolean;
}

export interface PrimeContact {
  name: string;
  phone: string;
  relation: string;
}

export interface SecuritySettings {
  isLocked: boolean;
  pin: string;
  usePin: boolean;
  voicePassphrase: string;
  useVoicePassphrase: boolean;
  lockedApps: string[];
}

export interface CallState {
  active: boolean;
  callerName: string;
  callerNumber: string;
  status: 'incoming' | 'connected' | 'ended';
  callDuration: number;
}
