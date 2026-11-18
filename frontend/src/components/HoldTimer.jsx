import { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';
import { countdown } from '../lib/format.js';

const HOLD_MS = 5 * 60 * 1000;

/** Countdown to the first of your holds running out. Calls onExpire once when it reaches zero. */
export default function HoldTimer({ expiresAt, onExpire }) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(tick);
  }, []);

  const left = expiresAt - now;
  useEffect(() => {
    if (left <= 0) onExpire?.();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- fire once when the time runs out
  }, [left <= 0]);

  const urgent = left < 60 * 1000;
  const percent = Math.max(0, Math.min(100, (left / HOLD_MS) * 100));

  return (
    <div className={`rounded-xl border p-3.5 ${urgent ? 'border-red-200 bg-red-50' : 'border-brand-100 bg-brand-50'}`}>
      <div className="flex items-center justify-between">
        <span className={`flex items-center gap-2 text-sm font-medium ${urgent ? 'text-red-700' : 'text-brand-800'}`}>
          <Clock className="h-4 w-4" aria-hidden="true" />
          Seats held for you
        </span>
        <span className={`font-mono text-lg font-bold tabular-nums ${urgent ? 'text-red-700' : 'text-brand-800'}`} role="timer">
          {countdown(left)}
        </span>
      </div>
      <div className="mt-2.5 h-1.5 overflow-hidden rounded-full bg-white/80">
        <div
          className={`h-full rounded-full transition-[width] duration-1000 ease-linear ${urgent ? 'bg-red-500' : 'bg-brand-600'}`}
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}
