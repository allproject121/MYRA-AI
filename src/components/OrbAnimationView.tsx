import React, { useEffect, useRef } from 'react';
import { AssistantState } from '../types';

interface OrbProps {
  state: AssistantState;
  audioLevel?: number; // 0 to 1
  onClick?: () => void;
  size?: number;
}

export const OrbAnimationView: React.FC<OrbProps> = ({
  state,
  audioLevel = 0,
  onClick,
  size = 280,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let angle = 0;

    // Core particles
    const particleCount = 28;
    const particles = Array.from({ length: particleCount }).map(() => ({
      angle: Math.random() * Math.PI * 2,
      radius: Math.random() * 45 + 15,
      speed: (Math.random() * 0.02 + 0.008) * (Math.random() > 0.5 ? 1 : -1),
      size: Math.random() * 3 + 1.5,
      alpha: Math.random() * 0.7 + 0.3,
    }));

    const render = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const cx = canvas.width / 2;
      const cy = canvas.height / 2;

      // Color scheme based on state
      let primaryColor = '255, 23, 68';     // #FF1744 red
      let secondaryColor = '255, 109, 109'; // #FF6D6D soft red
      let tertiaryColor = '213, 0, 249';    // #D500F9 purple

      if (state === 'listening') {
        primaryColor = '0, 229, 255';       // #00E5FF cyan
        secondaryColor = '0, 184, 212';     // #00B8D4
        tertiaryColor = '255, 255, 255';
      } else if (state === 'thinking') {
        primaryColor = '213, 0, 249';       // #D500F9 purple
        secondaryColor = '234, 128, 255';   // #EA80FF
        tertiaryColor = '255, 23, 68';
      } else if (state === 'speaking') {
        primaryColor = '255, 23, 68';
        secondaryColor = '255, 64, 129';
        tertiaryColor = '255, 215, 0';
      }

      // Dynamic boost from audio level
      const dynamicBoost = audioLevel * 25;
      const baseRadius = 56 + Math.sin(angle * 2) * 4 + dynamicBoost;

      // Outer ambient glowing aura
      const outerGlow = ctx.createRadialGradient(cx, cy, 20, cx, cy, baseRadius * 2.2);
      outerGlow.addColorStop(0, `rgba(${primaryColor}, ${0.45 + audioLevel * 0.4})`);
      outerGlow.addColorStop(0.5, `rgba(${secondaryColor}, ${0.2 + audioLevel * 0.2})`);
      outerGlow.addColorStop(1, 'rgba(0, 0, 0, 0)');
      ctx.fillStyle = outerGlow;
      ctx.beginPath();
      ctx.arc(cx, cy, baseRadius * 2.2, 0, Math.PI * 2);
      ctx.fill();

      // Energy Ring 1 (Rotating clock-wise)
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(angle);
      ctx.strokeStyle = `rgba(${primaryColor}, 0.8)`;
      ctx.lineWidth = 2.5;
      ctx.shadowColor = `rgba(${primaryColor}, 0.9)`;
      ctx.shadowBlur = 15;
      ctx.beginPath();
      ctx.arc(0, 0, baseRadius + 14, 0, Math.PI * 1.6);
      ctx.stroke();
      ctx.restore();

      // Energy Ring 2 (Rotating counter-clockwise)
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(-angle * 1.4);
      ctx.strokeStyle = `rgba(${secondaryColor}, 0.65)`;
      ctx.lineWidth = 1.8;
      ctx.shadowColor = `rgba(${secondaryColor}, 0.8)`;
      ctx.shadowBlur = 12;
      ctx.beginPath();
      ctx.arc(0, 0, baseRadius + 26, Math.PI * 0.4, Math.PI * 1.9);
      ctx.stroke();
      ctx.restore();

      // Energy Ring 3 (Thinking or Speaking high-speed outer ring)
      if (state === 'thinking' || state === 'speaking') {
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(angle * 3);
        ctx.strokeStyle = `rgba(${tertiaryColor}, 0.7)`;
        ctx.lineWidth = 1.2;
        ctx.setLineDash([8, 12]);
        ctx.beginPath();
        ctx.arc(0, 0, baseRadius + 38, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();
      }

      // Swirling particles
      particles.forEach((p) => {
        p.angle += p.speed * (state === 'speaking' || state === 'listening' ? 2 : 1);
        const px = cx + Math.cos(p.angle) * (p.radius + dynamicBoost * 0.8);
        const py = cy + Math.sin(p.angle) * (p.radius + dynamicBoost * 0.8);

        ctx.fillStyle = `rgba(${secondaryColor}, ${p.alpha})`;
        ctx.shadowColor = `rgba(${primaryColor}, 0.8)`;
        ctx.shadowBlur = 8;
        ctx.beginPath();
        ctx.arc(px, py, p.size, 0, Math.PI * 2);
        ctx.fill();
      });

      // Core Orb Sphere (Rich glowing 3D-like ball)
      const coreGradient = ctx.createRadialGradient(
        cx - baseRadius * 0.25,
        cy - baseRadius * 0.25,
        baseRadius * 0.1,
        cx,
        cy,
        baseRadius
      );
      coreGradient.addColorStop(0, '#ffffff');
      coreGradient.addColorStop(0.3, `rgb(${secondaryColor})`);
      coreGradient.addColorStop(0.8, `rgb(${primaryColor})`);
      coreGradient.addColorStop(1, '#050508');

      ctx.save();
      ctx.shadowColor = `rgba(${primaryColor}, 1)`;
      ctx.shadowBlur = 30 + dynamicBoost * 1.5;
      ctx.fillStyle = coreGradient;
      ctx.beginPath();
      ctx.arc(cx, cy, baseRadius, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      // Core Highlight reflection
      const highlight = ctx.createRadialGradient(
        cx - baseRadius * 0.35,
        cy - baseRadius * 0.35,
        2,
        cx - baseRadius * 0.35,
        cy - baseRadius * 0.35,
        baseRadius * 0.45
      );
      highlight.addColorStop(0, 'rgba(255, 255, 255, 0.85)');
      highlight.addColorStop(1, 'rgba(255, 255, 255, 0)');
      ctx.fillStyle = highlight;
      ctx.beginPath();
      ctx.arc(cx - baseRadius * 0.35, cy - baseRadius * 0.35, baseRadius * 0.45, 0, Math.PI * 2);
      ctx.fill();

      angle += 0.02 + audioLevel * 0.04;
      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [state, audioLevel]);

  return (
    <div
      onClick={onClick}
      className="relative cursor-pointer transition-transform duration-300 hover:scale-105 active:scale-95 flex items-center justify-center select-none"
      style={{ width: size, height: size }}
      title="Tap to speak with MYRA"
    >
      <canvas
        ref={canvasRef}
        width={size * 1.5}
        height={size * 1.5}
        className="w-full h-full pointer-events-none"
      />
    </div>
  );
};
