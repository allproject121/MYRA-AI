import React, { useState } from 'react';
import {
  X,
  Share2,
  Instagram,
  Facebook,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Eye,
  ShieldCheck,
  Send,
  UploadCloud,
  FileImage,
  ArrowRight,
  Flame,
  ThumbsUp,
  Ban
} from 'lucide-react';

interface SocialMediaAgentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onExecuteCommand?: (cmd: string) => void;
}

export const SocialMediaAgentModal: React.FC<SocialMediaAgentModalProps> = ({
  isOpen,
  onClose,
  onExecuteCommand,
}) => {
  const [platform, setPlatform] = useState<'INSTAGRAM' | 'FACEBOOK'>('INSTAGRAM');
  const [actionType, setActionType] = useState<'POST_FEED' | 'POST_STORY' | 'POST_REEL' | 'POST_TEXT'>('POST_FEED');
  const [caption, setCaption] = useState<string>('Sunset vibes with MYRA 🌅✨ #AICompanion');
  const [selectedPhoto, setSelectedPhoto] = useState<string>('sunset_beach.jpg');
  
  // Simulation of Closed-Loop States
  const [loopState, setLoopState] = useState<'IDLE' | 'OBSERVING' | 'RESOLVING' | 'AWAITING_CONFIRMATION' | 'PUBLISHED' | 'CANCELLED'>('IDLE');
  const [logTrace, setLogTrace] = useState<string[]>([
    'Visual Social Media Agent initialized.',
    'Accessibility closed-loop driver ready.',
    'Confirmation gate security active.',
  ]);

  const addLog = (msg: string) => {
    const time = new Date().toLocaleTimeString();
    setLogTrace((prev) => [`[${time}] ${msg}`, ...prev.slice(0, 15)]);
  };

  const handleStartTask = () => {
    setLoopState('OBSERVING');
    addLog(`[Task Started] Launching ${platform} with action ${actionType}...`);
    addLog(`[Lesson 2 Check] Recording window stamp to await fresh window...`);

    setTimeout(() => {
      setLoopState('RESOLVING');
      addLog(`[Observe Screen] Found 48 interactive nodes on screen.`);
      addLog(`[TargetResolver] Resolved NEXT button (id: next_button_textview) using structural anchor.`);
      addLog(`[UiActionExecutor] Clicked Next. Waiting for UI settle (400ms)...`);
    }, 1200);

    setTimeout(() => {
      addLog(`[TextInputExecutor] Typing caption: "${caption.slice(0, 30)}..." with keyboard re-observation.`);
      addLog(`[Confirmation Gate] Reached final Share/Publish screen. Publish button validated enabled.`);
      setLoopState('AWAITING_CONFIRMATION');
      addLog(`⚠️ [CONFIRMATION GATE ENGAGED] Stopped before publishing. Waiting for user approval.`);
    }, 2600);
  };

  const handleConfirmPublish = () => {
    setLoopState('PUBLISHED');
    addLog(`✅ [Explicit Confirmation] User confirmed publish!`);
    addLog(`[UiActionExecutor] Tapped Share button. Verifying on-screen upload success indicator...`);
    addLog(`🎉 [Verified] Success indicator "Finishing up... / Posted" observed on-screen! Status: PUBLISHED`);
    if (onExecuteCommand) {
      onExecuteCommand(`confirm post`);
    }
  };

  const handleRejectPublish = () => {
    setLoopState('CANCELLED');
    addLog(`🛑 [User Rejected] Publish cancelled. Discarding draft safely.`);
    if (onExecuteCommand) {
      onExecuteCommand(`reject post`);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-2xl bg-[#070712] border border-[#FF1744]/40 rounded-3xl shadow-2xl shadow-[#FF1744]/20 flex flex-col overflow-hidden text-sm">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#1A1A2E] bg-[#0c0c1c]">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-tr from-[#FF1744] to-[#D500F9] rounded-xl text-white">
              <Share2 className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold tracking-wider text-white font-mono">
                  VISUAL SOCIAL MEDIA AGENT
                </h2>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#FF1744]/20 border border-[#FF1744]/50 text-[#FF6D6D] font-mono font-bold">
                  CLOSED LOOP
                </span>
              </div>
              <p className="text-xs text-zinc-400 font-mono mt-0.5">
                Accessibility-first Instagram & Facebook UI automation with Confirmation Gate.
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 text-zinc-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Closed-Loop Pipeline Indicator */}
        <div className="bg-[#05050c] border-b border-[#1A1A2E] px-6 py-3">
          <div className="flex items-center justify-between text-[11px] font-mono">
            <div className={`flex items-center gap-1.5 ${loopState === 'OBSERVING' ? 'text-[#00E5FF] font-bold animate-pulse' : 'text-zinc-500'}`}>
              <Eye className="w-3.5 h-3.5" />
              <span>1. Observe</span>
            </div>
            <ArrowRight className="w-3 h-3 text-zinc-700" />
            <div className={`flex items-center gap-1.5 ${loopState === 'RESOLVING' ? 'text-[#D500F9] font-bold animate-pulse' : 'text-zinc-500'}`}>
              <RefreshCw className="w-3.5 h-3.5" />
              <span>2. Resolve & Tap</span>
            </div>
            <ArrowRight className="w-3 h-3 text-zinc-700" />
            <div className={`flex items-center gap-1.5 ${loopState === 'AWAITING_CONFIRMATION' ? 'text-[#FF1744] font-bold animate-bounce' : 'text-zinc-500'}`}>
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>3. Confirm Gate</span>
            </div>
            <ArrowRight className="w-3 h-3 text-zinc-700" />
            <div className={`flex items-center gap-1.5 ${loopState === 'PUBLISHED' ? 'text-[#00E676] font-bold' : 'text-zinc-500'}`}>
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>4. Verify Post</span>
            </div>
          </div>
        </div>

        {/* Body Content */}
        <div className="p-6 space-y-5 max-h-[60vh] overflow-y-auto">
          
          {/* Platform & Action Selector */}
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => setPlatform('INSTAGRAM')}
              className={`p-3.5 rounded-2xl border text-left font-mono transition-all flex items-center justify-between ${
                platform === 'INSTAGRAM'
                  ? 'bg-gradient-to-r from-[#833ab4]/20 via-[#fd1d1d]/20 to-[#fcb045]/20 border-[#FF1744] text-white shadow-lg'
                  : 'bg-[#0e0e1f] border-[#1f1f3a] text-zinc-400 hover:text-white'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Instagram className="w-5 h-5 text-[#FF1744]" />
                <div>
                  <span className="font-bold text-xs block">Instagram</span>
                  <span className="text-[10px] text-zinc-400">Post, Story, Reels</span>
                </div>
              </div>
              {platform === 'INSTAGRAM' && <CheckCircle2 className="w-4 h-4 text-[#FF1744]" />}
            </button>

            <button
              onClick={() => setPlatform('FACEBOOK')}
              className={`p-3.5 rounded-2xl border text-left font-mono transition-all flex items-center justify-between ${
                platform === 'FACEBOOK'
                  ? 'bg-[#1877F2]/20 border-[#1877F2] text-white shadow-lg'
                  : 'bg-[#0e0e1f] border-[#1f1f3a] text-zinc-400 hover:text-white'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Facebook className="w-5 h-5 text-[#1877F2]" />
                <div>
                  <span className="font-bold text-xs block">Facebook</span>
                  <span className="text-[10px] text-zinc-400">Feed & Text Posts</span>
                </div>
              </div>
              {platform === 'FACEBOOK' && <CheckCircle2 className="w-4 h-4 text-[#1877F2]" />}
            </button>
          </div>

          {/* Action Chips */}
          <div className="flex gap-2 font-mono text-xs overflow-x-auto pb-1">
            <button
              onClick={() => setActionType('POST_FEED')}
              className={`px-3 py-1.5 rounded-xl border transition-all ${
                actionType === 'POST_FEED'
                  ? 'bg-[#FF1744]/20 border-[#FF1744] text-[#FF6D6D] font-bold'
                  : 'bg-[#0e0e1f] border-[#1f1f3a] text-zinc-400'
              }`}
            >
              Feed Post
            </button>
            <button
              onClick={() => setActionType('POST_STORY')}
              className={`px-3 py-1.5 rounded-xl border transition-all ${
                actionType === 'POST_STORY'
                  ? 'bg-[#FF1744]/20 border-[#FF1744] text-[#FF6D6D] font-bold'
                  : 'bg-[#0e0e1f] border-[#1f1f3a] text-zinc-400'
              }`}
            >
              Story (Your Stories)
            </button>
            <button
              onClick={() => setActionType('POST_REEL')}
              className={`px-3 py-1.5 rounded-xl border transition-all ${
                actionType === 'POST_REEL'
                  ? 'bg-[#FF1744]/20 border-[#FF1744] text-[#FF6D6D] font-bold'
                  : 'bg-[#0e0e1f] border-[#1f1f3a] text-zinc-400'
              }`}
            >
              Reel
            </button>
            <button
              onClick={() => setActionType('POST_TEXT')}
              className={`px-3 py-1.5 rounded-xl border transition-all ${
                actionType === 'POST_TEXT'
                  ? 'bg-[#FF1744]/20 border-[#FF1744] text-[#FF6D6D] font-bold'
                  : 'bg-[#0e0e1f] border-[#1f1f3a] text-zinc-400'
              }`}
            >
              Text Post
            </button>
          </div>

          {/* Media & Caption Configuration */}
          <div className="space-y-3 font-mono text-xs">
            <div>
              <label className="text-zinc-400 text-[11px] block mb-1">
                Media Item (from Gallery Share Inbox):
              </label>
              <div className="flex items-center gap-3 p-3 bg-[#0e0e1f] border border-[#1f1f3a] rounded-xl">
                <FileImage className="w-4 h-4 text-[#00E5FF]" />
                <span className="text-zinc-200 flex-1 truncate">{selectedPhoto}</span>
                <span className="text-[10px] text-[#00E676] bg-[#00E676]/10 px-2 py-0.5 rounded-md border border-[#00E676]/30">
                  Ready (2.4 MB)
                </span>
              </div>
            </div>

            <div>
              <label className="text-zinc-400 text-[11px] block mb-1">
                Post Caption:
              </label>
              <input
                type="text"
                value={caption}
                onChange={(e) => setCaption(e.target.value)}
                placeholder="Write caption here..."
                className="w-full bg-[#0e0e1f] border border-[#1f1f3a] rounded-xl px-3 py-2 text-white focus:outline-none focus:border-[#FF1744]"
              />
            </div>
          </div>

          {/* CONFIRMATION GATE ALERT (Engaged when waiting for approval) */}
          {loopState === 'AWAITING_CONFIRMATION' && (
            <div className="p-4 bg-[#20080f] border-2 border-[#FF1744] rounded-2xl space-y-3 animate-pulse">
              <div className="flex items-start gap-3">
                <ShieldCheck className="w-5 h-5 text-[#FF1744] shrink-0 mt-0.5" />
                <div className="flex-1">
                  <h4 className="font-mono font-bold text-sm text-white">
                    CONFIRMATION GATE ACTIVE
                  </h4>
                  <p className="text-xs font-mono text-zinc-300 mt-1 leading-relaxed">
                    "{platform} post ready hai! Caption hai: <strong>"{caption}"</strong>. Kya main ise share kar doon?"
                  </p>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-1">
                <button
                  onClick={handleRejectPublish}
                  className="px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 rounded-xl font-mono text-xs flex items-center gap-1.5 transition-colors"
                >
                  <Ban className="w-3.5 h-3.5" />
                  Cancel / Reject
                </button>
                <button
                  onClick={handleConfirmPublish}
                  className="px-5 py-2 bg-[#FF1744] hover:bg-[#D50000] text-white rounded-xl font-mono font-bold text-xs flex items-center gap-1.5 shadow-lg shadow-[#FF1744]/40 transition-all"
                >
                  <ThumbsUp className="w-3.5 h-3.5" />
                  Yes, Publish Now!
                </button>
              </div>
            </div>
          )}

          {/* Real-Time Agent Closed-Loop Execution Log */}
          <div className="bg-[#05050c] border border-[#1f1f3a] rounded-2xl p-3 h-28 overflow-y-auto font-mono text-[11px] space-y-1">
            <span className="text-[10px] text-zinc-500 uppercase tracking-wider font-bold block mb-1">
              Live Closed-Loop Trace (Accessibility Driver):
            </span>
            {logTrace.map((msg, i) => (
              <div
                key={i}
                className={
                  msg.includes('CONFIRMATION')
                    ? 'text-[#FF1744] font-bold'
                    : msg.includes('PUBLISHED') || msg.includes('Verified')
                    ? 'text-[#00E676] font-bold'
                    : 'text-zinc-400'
                }
              >
                {msg}
              </div>
            ))}
          </div>

        </div>

        {/* Footer Controls */}
        <div className="px-6 py-4 border-t border-[#1A1A2E] bg-[#0c0c1c] flex items-center justify-between text-xs font-mono">
          <div className="flex items-center gap-2">
            <span className="text-zinc-500">Voice command:</span>
            <span className="text-[#FF6D6D] font-bold">
              "{platform === 'INSTAGRAM' ? 'Instagram par photo post karo' : 'Facebook par post karo'}"
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleStartTask}
              disabled={loopState === 'OBSERVING' || loopState === 'RESOLVING' || loopState === 'AWAITING_CONFIRMATION'}
              className="px-5 py-2 bg-gradient-to-r from-[#FF1744] to-[#D500F9] hover:opacity-90 disabled:opacity-50 text-white rounded-xl font-bold shadow-lg shadow-[#FF1744]/25 transition-all flex items-center gap-1.5"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Start Closed-Loop Automation</span>
            </button>
            <button
              onClick={onClose}
              className="px-4 py-2 bg-white/10 hover:bg-white/15 text-white rounded-xl transition-colors"
            >
              Close
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
