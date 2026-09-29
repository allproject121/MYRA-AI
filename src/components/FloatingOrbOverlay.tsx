import React, { useState, useRef } from 'react';
import { AssistantState } from '../types';
import { Mic, X, Maximize2 } from 'lucide-react';

interface FloatingOrbProps {
  state: AssistantState;
  onActivate: () => void;
  onClose: () => void;
}

export const FloatingOrbOverlay: React.FC<FloatingOrbProps> = ({
  state,
  onActivate,
  onClose,
}) => {
  const [position, setPosition] = useState({ x: 24, y: 120 });
  const isDragging = useRef(false);
  const dragStart = useRef({ x: 0, y: 0 });
  const initialPos = useRef({ x: 0, y: 0 });

  const handleMouseDown = (e: React.MouseEvent) => {
    isDragging.current = true;
    dragStart.current = { x: e.clientX, y: e.clientY };
    initialPos.current = { ...position };
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging.current) return;
    const dx = e.clientX - dragStart.current.x;
    const dy = e.clientY - dragStart.current.y;
    setPosition({
      x: Math.max(10, Math.min(window.innerWidth - 80, initialPos.current.x + dx)),
      y: Math.max(10, Math.min(window.innerHeight - 80, initialPos.current.y + dy)),
    });
  };

  const handleMouseUp = () => {
    isDragging.current = false;
  };

  let glowColor = 'rgba(255, 23, 68, 0.7)';
  let bgOrb = 'from-[#FF1744] to-[#800010]';
  if (state === 'listening') {
    glowColor = 'rgba(0, 229, 255, 0.8)';
    bgOrb = 'from-[#00E5FF] to-[#005577]';
  } else if (state === 'thinking') {
    glowColor = 'rgba(213, 0, 249, 0.8)';
    bgOrb = 'from-[#D500F9] to-[#600077]';
  }

  return (
    <div
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
      }}
      className="fixed z-50 cursor-grab active:cursor-grabbing select-none"
    >
      <div className="relative group">
        {/* Glow Ring */}
        <div
          className="absolute -inset-2 rounded-full animate-ping opacity-40 blur-md pointer-events-none"
          style={{ backgroundColor: glowColor }}
        />

        {/* Core Floating Orb */}
        <div
          onMouseDown={handleMouseDown}
          onClick={onActivate}
          className={`relative w-16 h-16 rounded-full bg-gradient-to-tr ${bgOrb} border-2 border-white/40 flex items-center justify-center shadow-xl cursor-pointer transition-transform hover:scale-110 active:scale-95`}
          style={{
            boxShadow: `0 0 25px ${glowColor}`,
          }}
          title="MYRA Floating Orb - Tap to talk or drag to move"
        >
          <Mic className="w-7 h-7 text-white drop-shadow" />
          <span className="absolute -bottom-5 text-[9px] font-mono font-bold tracking-widest text-white/90 bg-black/75 px-1.5 py-0.5 rounded shadow">
            MYRA
          </span>
        </div>

        {/* Quick action controls on hover */}
        <div className="absolute -top-2 -right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onClose();
            }}
            className="w-5 h-5 rounded-full bg-zinc-900 border border-zinc-700 hover:bg-rose-600 text-white flex items-center justify-center text-[10px]"
            title="Dismiss overlay"
          >
            <X className="w-3 h-3" />
          </button>
        </div>
      </div>
    </div>
  );
};
