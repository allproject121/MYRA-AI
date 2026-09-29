import React, { useEffect, useState } from 'react';
import { CallState } from '../types';
import { Phone, PhoneOff, Mic, Volume2, User } from 'lucide-react';

interface CallAssistantProps {
  callState: CallState;
  onAccept: () => void;
  onReject: () => void;
}

export const CallAssistantModal: React.FC<CallAssistantProps> = ({
  callState,
  onAccept,
  onReject,
}) => {
  const [duration, setDuration] = useState(0);

  useEffect(() => {
    let timer: any;
    if (callState.active && callState.status === 'connected') {
      timer = setInterval(() => {
        setDuration((d) => d + 1);
      }, 1000);
    } else {
      setDuration(0);
    }
    return () => clearInterval(timer);
  }, [callState.active, callState.status]);

  if (!callState.active) return null;

  const formatTime = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const remainder = secs % 60;
    return `${mins.toString().padStart(2, '0')}:${remainder.toString().padStart(2, '0')}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/90 backdrop-blur-xl animate-fadeIn">
      <div className="relative w-full max-w-sm bg-[#080811] border-2 border-[#FF1744] rounded-3xl p-6 shadow-[0_0_50px_rgba(255,23,68,0.4)] flex flex-col items-center text-center">
        
        {/* Pulsing Aura Badge */}
        <div className="mb-4 inline-flex items-center gap-2 px-3 py-1 bg-[#FF1744]/15 border border-[#FF1744]/40 rounded-full text-[#FF1744] text-[10px] font-mono font-bold tracking-widest uppercase animate-pulse">
          <div className="w-2 h-2 rounded-full bg-[#FF1744]" />
          MYRA CALL ASSISTANT
        </div>

        {/* Caller Avatar */}
        <div className="relative w-28 h-28 my-4 flex items-center justify-center">
          <div className="absolute inset-0 rounded-full bg-[#FF1744]/20 animate-ping opacity-75" />
          <div className="relative w-24 h-24 rounded-full bg-gradient-to-tr from-[#1b0a0e] to-[#3a0a14] border-2 border-[#FF1744] flex items-center justify-center shadow-lg shadow-[#FF1744]/40">
            <User className="w-12 h-12 text-[#FF6D6D]" />
          </div>
        </div>

        {/* Caller Info */}
        <h3 className="text-xl font-bold text-white font-sans tracking-wide">
          {callState.callerName}
        </h3>
        <p className="text-xs text-zinc-400 font-mono mt-1 tracking-wider">
          {callState.callerNumber}
        </p>

        {/* Status prompt */}
        {callState.status === 'incoming' ? (
          <div className="my-6 p-3 bg-[#16060a] border border-[#FF1744]/40 rounded-2xl w-full">
            <p className="text-xs text-[#FF6D6D] font-mono leading-relaxed">
              &quot;Sir, <strong className="text-white">{callState.callerName}</strong> ka call aa raha hai... uthau ya reject karu?&quot;
            </p>
            <span className="text-[10px] text-zinc-400 block mt-1.5 font-mono">
              Say &quot;Uthao&quot; or &quot;Reject karo&quot;
            </span>
          </div>
        ) : (
          <div className="my-6">
            <span className="text-lg font-mono font-bold text-emerald-400 tracking-wider">
              {formatTime(duration)}
            </span>
            <span className="text-[11px] text-zinc-400 block font-mono mt-0.5">
              Call Connected (Speaker Active)
            </span>
          </div>
        )}

        {/* Action Controls */}
        {callState.status === 'incoming' ? (
          <div className="flex items-center justify-center gap-10 mt-2 w-full">
            {/* Reject */}
            <div className="flex flex-col items-center gap-1.5">
              <button
                onClick={onReject}
                className="w-16 h-16 rounded-full bg-rose-600 hover:bg-rose-500 active:scale-95 flex items-center justify-center shadow-lg shadow-rose-600/40 text-white transition-all"
                title="Reject Call"
              >
                <PhoneOff className="w-7 h-7" />
              </button>
              <span className="text-[11px] text-zinc-300 font-mono">Reject karo</span>
            </div>

            {/* Accept */}
            <div className="flex flex-col items-center gap-1.5">
              <button
                onClick={onAccept}
                className="w-16 h-16 rounded-full bg-emerald-500 hover:bg-emerald-400 active:scale-95 flex items-center justify-center shadow-lg shadow-emerald-500/40 text-white transition-all"
                title="Accept Call"
              >
                <Phone className="w-7 h-7" />
              </button>
              <span className="text-[11px] text-zinc-300 font-mono">Uthao</span>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center gap-6 mt-2 w-full">
            <button
              onClick={onReject}
              className="py-3 px-8 rounded-full bg-rose-600 hover:bg-rose-500 text-white font-bold font-mono text-xs flex items-center gap-2 shadow-lg shadow-rose-600/50 transition-all active:scale-95"
            >
              <PhoneOff className="w-4 h-4" /> End Call
            </button>
          </div>
        )}

      </div>
    </div>
  );
};
