// src/components/layout/TotpInput.tsx
import React from 'react';

interface TotpInputProps {
  title: string;
  description: string;
  value: string;
  onChange: (val: string) => void;
  countdown: number;
  resendLabel: string;
  onResend: () => void;
  error?: string;
}

export const TotpInput: React.FC<TotpInputProps> = ({
  title, description, value, onChange, countdown, resendLabel, onResend, error
}) => (
  <div className="space-y-6 text-center">
    <h3 className="font-bold text-base text-slate-900 flex items-center justify-center gap-2">
      <span className="material-symbols-outlined text-corporate-blue">mail_lock</span>
      {title}
    </h3>
    <p className="text-xs text-slate-500 leading-relaxed px-2">{description}</p>
    {error && (
      <div className="p-3 bg-red-50 border border-red-100 rounded-lg text-red-600 text-xs font-bold flex items-center justify-center gap-2 animate-[fadeIn_0.2s_ease-out] w-5/6 mx-auto text-left">
        <span className="material-symbols-outlined text-[16px]">error</span>
        {error}
      </div>
    )}
    <input
      type="text"
      maxLength={6}
      value={value}
      placeholder="000000"
      onChange={(e) => onChange(e.target.value.replace(/[^\d]/g, ''))}
      className="w-3/4 mx-auto block text-center tracking-[0.4em] font-mono text-xl font-bold rounded-xl border-slate-200 bg-slate-50 h-12 focus:ring-corporate-blue focus:border-corporate-blue"
    />
    <button
      type="button"
      disabled={countdown > 0}
      onClick={onResend}
      className="text-[10px] font-bold text-slate-400 hover:text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all py-1 cursor-pointer"
    >
      {countdown > 0 ? `${resendLabel} (${countdown}s)` : resendLabel}
    </button>
  </div>
);