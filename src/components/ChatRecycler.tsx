import React, { useEffect, useRef } from 'react';
import { ChatMessage } from '../types';
import { Volume2, Sparkles, CheckCircle2 } from 'lucide-react';

interface ChatRecyclerProps {
  messages: ChatMessage[];
  onSpeakMessage?: (text: string) => void;
  onSuggestionClick: (text: string) => void;
}

export const ChatRecycler: React.FC<ChatRecyclerProps> = ({
  messages,
  onSpeakMessage,
  onSuggestionClick,
}) => {
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const suggestions = [
    'YouTube kholo',
    'Mere close friend ko call karo',
    'Torch on karo',
    'Volume badhao',
    'WiFi band karo',
    'Kaisi ho Myra?',
    'Tell me a sweet secret',
  ];

  return (
    <div className="w-full flex flex-col justify-end">
      {/* Scrollable Message Feed */}
      <div className="max-h-56 overflow-y-auto px-4 space-y-3 pb-2 scroll-smooth">
        {messages.map((msg) => {
          const isMyra = msg.sender === 'myra';
          return (
            <div
              key={msg.id}
              className={`flex flex-col ${isMyra ? 'items-start' : 'items-end'} animate-fadeIn`}
            >
              <div
                className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-xs shadow-md transition-all ${
                  isMyra
                    ? 'bg-[#0E0E1E] border border-[#262648] text-[#E8E8FF] rounded-tl-sm'
                    : 'bg-gradient-to-r from-[#FF1744]/25 to-[#B71C1C]/40 border border-[#FF1744]/50 text-white rounded-tr-sm'
                }`}
              >
                {/* Header label */}
                <div className="flex items-center justify-between gap-3 mb-1">
                  <span
                    className={`font-mono text-[10px] font-bold tracking-wider uppercase ${
                      isMyra ? 'text-[#FF1744]' : 'text-zinc-300'
                    }`}
                  >
                    {isMyra ? 'M · Y · R · A' : 'YOU'}
                  </span>
                  <span className="text-[9px] text-zinc-500 font-mono">{msg.timestamp}</span>
                </div>

                {/* Message Body */}
                <p className="text-xs leading-relaxed break-words font-sans">{msg.text}</p>

                {/* Action feedback tag if device action occurred */}
                {msg.actionApplied && (
                  <div className="mt-2 inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-[#00E5FF]/10 border border-[#00E5FF]/40 text-[#00E5FF] text-[10px] font-mono">
                    <CheckCircle2 className="w-3 h-3" />
                    <span>{msg.actionApplied}</span>
                  </div>
                )}

                {/* Speak button for MYRA bubbles */}
                {isMyra && onSpeakMessage && (
                  <div className="mt-1.5 flex justify-end">
                    <button
                      onClick={() => onSpeakMessage(msg.text)}
                      className="p-1 text-zinc-400 hover:text-[#00E5FF] transition-colors"
                      title="Replay Audio"
                    >
                      <Volume2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {/* Suggestion Chips */}
      <div className="flex items-center gap-1.5 overflow-x-auto py-2 px-4 no-scrollbar">
        <span className="text-[10px] font-mono text-zinc-500 shrink-0 flex items-center gap-1">
          <Sparkles className="w-3 h-3 text-[#FF1744]" /> TRY:
        </span>
        {suggestions.map((sug, i) => (
          <button
            key={i}
            onClick={() => onSuggestionClick(sug)}
            className="shrink-0 px-2.5 py-1 rounded-full bg-[#101020] border border-[#22223c] hover:border-[#FF1744] hover:bg-[#FF1744]/10 text-zinc-300 hover:text-white text-[10px] font-mono transition-all"
          >
            {sug}
          </button>
        ))}
      </div>
    </div>
  );
};
