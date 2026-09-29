import React, { useState, useEffect, useRef } from 'react';
import { Shield, ShieldAlert, Video, VideoOff, Eye, Volume2, X, AlertTriangle } from 'lucide-react';

interface SecurityModeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAlertTriggered?: (msg: string) => void;
}

export const SecurityModeModal: React.FC<SecurityModeModalProps> = ({
  isOpen,
  onClose,
  onAlertTriggered,
}) => {
  const [isActive, setIsActive] = useState<boolean>(true);
  const [motionDetected, setMotionDetected] = useState<boolean>(false);
  const [sensitivity, setSensitivity] = useState<number>(30);
  const [logMessages, setLogMessages] = useState<string[]>([]);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const prevImageDataRef = useRef<Uint8ClampedArray | null>(null);
  const animFrameRef = useRef<number | null>(null);

  const addLog = (msg: string) => {
    const time = new Date().toLocaleTimeString();
    setLogMessages((prev) => [`[${time}] ${msg}`, ...prev.slice(0, 15)]);
  };

  useEffect(() => {
    if (!isOpen) {
      stopCamera();
      return;
    }

    startCamera();
    addLog('Security Mode initialized: Watching camera stream for motion...');

    return () => {
      stopCamera();
    };
  }, [isOpen]);

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 } },
        audio: false,
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
      }
      setIsActive(true);
      startMotionDetection();
    } catch (err: any) {
      addLog(`Camera access notice: ${err.message || 'Permission needed'}`);
    }
  };

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current);
    }
    setIsActive(false);
  };

  const startMotionDetection = () => {
    const canvas = canvasRef.current || document.createElement('canvas');
    canvasRef.current = canvas;
    canvas.width = 160;
    canvas.height = 120;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });

    let lastCheckTime = Date.now();

    const loop = () => {
      if (!streamRef.current || !videoRef.current || videoRef.current.readyState < 2) {
        animFrameRef.current = requestAnimationFrame(loop);
        return;
      }

      const now = Date.now();
      if (now - lastCheckTime > 300) {
        // analyze every 300ms
        lastCheckTime = now;
        if (ctx) {
          ctx.drawImage(videoRef.current, 0, 0, 160, 120);
          const currentFrame = ctx.getImageData(0, 0, 160, 120).data;

          if (prevImageDataRef.current) {
            let diffCount = 0;
            const totalPixels = currentFrame.length / 4;

            for (let i = 0; i < currentFrame.length; i += 16) {
              const diffR = Math.abs(currentFrame[i] - prevImageDataRef.current[i]);
              const diffG = Math.abs(currentFrame[i + 1] - prevImageDataRef.current[i + 1]);
              const diffB = Math.abs(currentFrame[i + 2] - prevImageDataRef.current[i + 2]);
              if (diffR + diffG + diffB > 60) {
                diffCount++;
              }
            }

            const motionRatio = (diffCount / (totalPixels / 4)) * 100;

            if (motionRatio > (100 - sensitivity * 2)) {
              setMotionDetected(true);
              addLog(`⚠️ MOTION ALERT! Movement detected (Intensity: ${Math.round(motionRatio)}%)`);
              if (onAlertTriggered) {
                onAlertTriggered('Security Alert! Movement detected on camera!');
              }
              // Beep sound
              playAlertTone();
              setTimeout(() => setMotionDetected(false), 2000);
            }
          }

          prevImageDataRef.current = new Uint8ClampedArray(currentFrame);
        }
      }

      animFrameRef.current = requestAnimationFrame(loop);
    };

    animFrameRef.current = requestAnimationFrame(loop);
  };

  const playAlertTone = () => {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(880, audioCtx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(440, audioCtx.currentTime + 0.3);
      gain.gain.setValueAtTime(0.2, audioCtx.currentTime);
      gain.gain.linearRampToValueAtTime(0, audioCtx.currentTime + 0.3);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.3);
    } catch (e) {}
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-lg bg-[#070712] border border-[#FF1744]/50 rounded-3xl shadow-2xl shadow-[#FF1744]/30 flex flex-col overflow-hidden text-sm">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#1A1A2E] bg-[#0c0c1c]">
          <div className="flex items-center gap-2.5">
            <div className={`p-2 rounded-xl ${motionDetected ? 'bg-[#FF1744] text-white animate-pulse' : 'bg-[#FF1744]/20 text-[#FF1744]'}`}>
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold tracking-wider text-white font-mono flex items-center gap-2">
                MYRA SECURITY WATCH MODE
              </h2>
              <span className="text-[10px] text-zinc-400 font-mono">
                Real-Time Phone Camera Motion & Intruder Sentry
              </span>
            </div>
          </div>
          <button
            onClick={() => {
              stopCamera();
              onClose();
            }}
            className="p-1.5 text-zinc-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Status Banner */}
        <div className={`px-5 py-2.5 flex items-center justify-between text-xs font-mono border-b ${
          motionDetected
            ? 'bg-rose-950/80 border-rose-600 text-rose-200 animate-pulse'
            : 'bg-[#090918] border-[#1f1f3a] text-zinc-300'
        }`}>
          <div className="flex items-center gap-2">
            <div className={`w-2 h-2 rounded-full ${motionDetected ? 'bg-[#FF1744]' : 'bg-[#00E676] animate-ping'}`} />
            <span>
              {motionDetected ? '⚠️ ALERT: MOTION DETECTED IN ROOM!' : 'SENTRY ACTIVE: WATCHING CAMERA STREAM'}
            </span>
          </div>
          <span className="text-[10px] text-zinc-400">
            {isActive ? 'FEED LIVE' : 'STOPPED'}
          </span>
        </div>

        {/* Video Viewport */}
        <div className="relative w-full h-56 bg-black flex items-center justify-center overflow-hidden border-b border-[#1A1A2E]">
          <video
            ref={videoRef}
            playsInline
            muted
            className="w-full h-full object-cover"
          />
          <canvas ref={canvasRef} className="hidden" />

          {/* Grid reticle overlay */}
          <div className="absolute inset-0 pointer-events-none flex flex-col justify-between p-4 opacity-40">
            <div className="flex justify-between text-[10px] font-mono text-[#00E5FF]">
              <span>[SEC-CAM-01]</span>
              <span>1080P/30FPS</span>
            </div>
            <div className="flex justify-center items-center">
              <div className="w-16 h-16 border border-dashed border-[#FF1744]/70 rounded-full flex items-center justify-center">
                <div className="w-1.5 h-1.5 bg-[#FF1744] rounded-full" />
              </div>
            </div>
            <div className="flex justify-between text-[10px] font-mono text-[#00E5FF]">
              <span>STATUS: SENTINEL</span>
              <span>SENSITIVITY: {sensitivity}%</span>
            </div>
          </div>
        </div>

        {/* Controls & Log */}
        <div className="p-5 space-y-4">
          {/* Sensitivity Slider */}
          <div className="flex items-center justify-between gap-4">
            <span className="text-xs font-mono text-zinc-400">Detection Sensitivity:</span>
            <input
              type="range"
              min="10"
              max="45"
              value={sensitivity}
              onChange={(e) => setSensitivity(parseInt(e.target.value, 10))}
              className="flex-1 accent-[#FF1744]"
            />
            <span className="text-xs font-mono font-bold text-white w-8 text-right">
              {sensitivity}%
            </span>
          </div>

          {/* Event Log */}
          <div className="bg-[#0e0e1f] border border-[#1f1f3a] rounded-2xl p-3 h-28 overflow-y-auto font-mono text-[11px] space-y-1">
            <div className="text-[10px] text-zinc-400 uppercase tracking-wider font-bold mb-1">
              Sentry Activity Log:
            </div>
            {logMessages.length === 0 ? (
              <span className="text-zinc-600">No events detected yet. Standing guard...</span>
            ) : (
              logMessages.map((log, idx) => (
                <div
                  key={idx}
                  className={log.includes('ALERT') ? 'text-rose-400 font-bold' : 'text-zinc-400'}
                >
                  {log}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3.5 border-t border-[#1A1A2E] bg-[#0c0c1c] flex items-center justify-between text-xs font-mono">
          <button
            onClick={() => {
              if (isActive) stopCamera();
              else startCamera();
            }}
            className={`px-4 py-2 rounded-xl font-bold transition-all ${
              isActive
                ? 'bg-rose-950/60 border border-rose-600 text-rose-300 hover:bg-rose-900/60'
                : 'bg-[#FF1744] text-white hover:bg-[#D50000]'
            }`}
          >
            {isActive ? 'Stop Security Mode' : 'Resume Security Mode'}
          </button>

          <button
            onClick={() => {
              stopCamera();
              onClose();
            }}
            className="px-4 py-2 bg-white/10 hover:bg-white/15 text-white rounded-xl transition-colors"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
};
