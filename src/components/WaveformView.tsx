import React, { useEffect, useState } from 'react';
import { AssistantState } from '../types';

interface WaveformProps {
  state: AssistantState;
  audioLevel?: number; // 0 to 1
  barCount?: number;
}

export const WaveformView: React.FC<WaveformProps> = ({
  state,
  audioLevel = 0,
  barCount = 20,
}) => {
  const [heights, setHeights] = useState<number[]>(() =>
    Array.from({ length: barCount }, () => 8)
  );

  useEffect(() => {
    let animId: number;
    let t = 0;

    const updateBars = () => {
      t += 0.15;
      const newHeights = Array.from({ length: barCount }, (_, i) => {
        if (state === 'idle') {
          // Subtle resting breathing wave
          return 4 + Math.sin(t + i * 0.4) * 3;
        }

        if (state === 'listening') {
          // Dynamic reactiveness to audioLevel
          const base = audioLevel * 36;
          const sineNoise = Math.sin(t * 2 + i * 0.8) * (8 + base * 0.5);
          return Math.max(6, Math.min(42, 6 + base + sineNoise));
        }

        if (state === 'thinking') {
          // Center-out pulsing ripple
          const distFromCenter = Math.abs(i - barCount / 2);
          const ripple = Math.sin(t * 3 - distFromCenter * 0.5) * 16;
          return Math.max(6, Math.min(38, 14 + ripple));
        }

        if (state === 'speaking') {
          // Rhythmic energetic voice bars
          const voiceWave = Math.sin(t * 3 + i * 0.6) * 18 + Math.cos(t * 1.5 + i * 0.3) * 10;
          return Math.max(8, Math.min(44, 20 + voiceWave));
        }

        return 8;
      });

      setHeights(newHeights);
      animId = requestAnimationFrame(updateBars);
    };

    animId = requestAnimationFrame(updateBars);
    return () => cancelAnimationFrame(animId);
  }, [state, audioLevel, barCount]);

  let barBg = 'bg-[#FF1744] shadow-[#FF1744]/60';
  if (state === 'listening') {
    barBg = 'bg-[#00E5FF] shadow-[#00E5FF]/70';
  } else if (state === 'thinking') {
    barBg = 'bg-[#D500F9] shadow-[#D500F9]/70';
  }

  return (
    <div className="flex items-center justify-center gap-1.5 h-12 px-4 py-1">
      {heights.map((h, i) => (
        <div
          key={i}
          className={`w-1.5 rounded-full transition-all duration-75 shadow-sm ${barBg}`}
          style={{
            height: `${Math.round(h)}px`,
            opacity: 0.35 + (h / 45) * 0.65,
          }}
        />
      ))}
    </div>
  );
};
