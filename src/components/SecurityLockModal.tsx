import React, { useState } from 'react';
import { Lock, Delete, ShieldAlert } from 'lucide-react';

interface SecurityLockProps {
  isOpen: boolean;
  onSuccess: () => void;
  onCancel: () => void;
  expectedPin?: string;
}

export const SecurityLockModal: React.FC<SecurityLockProps> = ({
  isOpen,
  onSuccess,
  onCancel,
  expectedPin = '2601',
}) => {
  const [pin, setPin] = useState('');
  const [error, setError] = useState(false);

  if (!isOpen) return null;

  const handleDigit = (digit: string) => {
    if (pin.length < 4) {
      const nextPin = pin + digit;
      setPin(nextPin);
      setError(false);

      if (nextPin.length === 4) {
        if (nextPin === expectedPin) {
          setTimeout(() => {
            setPin('');
            onSuccess();
          }, 150);
        } else {
          setTimeout(() => {
            setError(true);
            setPin('');
          }, 200);
        }
      }
    }
  };

  const handleDelete = () => {
    setPin((p) => p.slice(0, -1));
    setError(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/90 backdrop-blur-xl animate-fadeIn">
      <div className="relative w-full max-w-xs bg-[#090914] border border-[#FF1744]/40 rounded-3xl p-6 shadow-2xl shadow-[#FF1744]/20 flex flex-col items-center">
        
        {/* Lock Icon */}
        <div className="w-14 h-14 rounded-full bg-[#180a0e] border border-[#FF1744] flex items-center justify-center mb-3 text-[#FF1744] shadow-md shadow-[#FF1744]/30">
          <Lock className="w-7 h-7" />
        </div>

        <h3 className="text-base font-bold text-white font-mono tracking-wider">
          MYRA APP LOCK
        </h3>
        <p className="text-[11px] text-zinc-400 font-mono mt-0.5">
          Enter 4-Digit Security PIN (Default: 2601)
        </p>

        {/* PIN Indicators */}
        <div className="flex items-center gap-3 my-5">
          {[0, 1, 2, 3].map((i) => (
            <div
              key={i}
              className={`w-3.5 h-3.5 rounded-full transition-all duration-200 ${
                error
                  ? 'bg-rose-500 shadow-[0_0_10px_#f43f5e]'
                  : pin.length > i
                  ? 'bg-[#FF1744] shadow-[0_0_10px_#FF1744] scale-110'
                  : 'bg-zinc-800 border border-zinc-700'
              }`}
            />
          ))}
        </div>

        {error && (
          <span className="text-xs text-rose-400 font-mono flex items-center gap-1 mb-2">
            <ShieldAlert className="w-3.5 h-3.5" /> Incorrect PIN. Try 2601
          </span>
        )}

        {/* Numpad */}
        <div className="grid grid-cols-3 gap-2.5 w-full my-2">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
            <button
              key={digit}
              type="button"
              onClick={() => handleDigit(digit)}
              className="h-14 rounded-2xl bg-[#111124] border border-[#1f1f3a] text-lg font-bold font-mono text-zinc-100 hover:bg-[#1a1a38] active:scale-95 transition-all"
            >
              {digit}
            </button>
          ))}
          <button
            type="button"
            onClick={onCancel}
            className="h-14 rounded-2xl bg-zinc-900 border border-zinc-800 text-xs font-mono text-zinc-400 hover:text-white"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => handleDigit('0')}
            className="h-14 rounded-2xl bg-[#111124] border border-[#1f1f3a] text-lg font-bold font-mono text-zinc-100 hover:bg-[#1a1a38] active:scale-95 transition-all"
          >
            0
          </button>
          <button
            type="button"
            onClick={handleDelete}
            className="h-14 rounded-2xl bg-[#111124] border border-[#1f1f3a] flex items-center justify-center text-zinc-300 hover:text-white active:scale-95 transition-all"
          >
            <Delete className="w-5 h-5" />
          </button>
        </div>

      </div>
    </div>
  );
};
