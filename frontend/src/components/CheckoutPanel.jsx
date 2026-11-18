import { CreditCard, Loader2, MousePointerClick, X } from 'lucide-react';
import { money } from '../lib/format.js';
import HoldTimer from './HoldTimer.jsx';
import { Card, Notice, primaryButtonClass } from './ui.jsx';

/** Right-hand panel: the seats you hold, the countdown, the total and the (simulated) Pay button. */
export default function CheckoutPanel({ seats, onRelease, onPay, onExpire, paying, error, busySeatId }) {
  const total = seats.reduce((sum, seat) => sum + seat.priceCents, 0);
  const firstExpiry = Math.min(...seats.map((seat) => seat.expiresAt));

  return (
    <Card className="p-5 sm:p-6">
      <h2 className="text-base font-semibold text-gray-900">Your seats</h2>

      {seats.length === 0 ? (
        <div className="mt-4 flex flex-col items-center rounded-xl border border-dashed border-gray-300 px-4 py-8 text-center">
          <MousePointerClick className="h-6 w-6 text-brand-600" aria-hidden="true" />
          <p className="mt-3 text-sm font-medium text-gray-900">Click a green seat to hold it</p>
          <p className="mt-1 text-sm text-gray-500">It's yours for 5 minutes while you pay. Nobody else can take it.</p>
        </div>
      ) : (
        <div className="mt-4 space-y-4">
          <HoldTimer key={firstExpiry} expiresAt={firstExpiry} onExpire={onExpire} />

          <ul className="divide-y divide-gray-100 rounded-xl border border-gray-200">
            {seats.map((seat) => (
              <li key={seat.id} className="flex items-center justify-between px-3.5 py-2.5 text-sm">
                <span className="font-medium text-gray-900">Seat {seat.label}</span>
                <span className="flex items-center gap-3">
                  <span className="text-gray-600">{money(seat.priceCents)}</span>
                  <button
                    type="button"
                    onClick={() => onRelease(seat)}
                    disabled={paying || busySeatId === seat.id}
                    aria-label={`Release seat ${seat.label}`}
                    className="rounded-md p-1 text-gray-400 transition hover:bg-gray-100 hover:text-gray-700 disabled:opacity-50"
                  >
                    <X className="h-4 w-4" aria-hidden="true" />
                  </button>
                </span>
              </li>
            ))}
          </ul>

          <div className="flex items-baseline justify-between border-t border-gray-100 pt-4">
            <span className="text-sm text-gray-500">Total</span>
            <span className="text-2xl font-bold tracking-tight text-gray-900">{money(total)}</span>
          </div>

          <Notice type="error">{error}</Notice>

          <button type="button" onClick={onPay} disabled={paying} className={`${primaryButtonClass} w-full py-3`}>
            {paying ? (
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
            ) : (
              <CreditCard className="h-4 w-4" aria-hidden="true" />
            )}
            {paying ? 'Paying…' : `Pay ${money(total)}`}
          </button>
          <p className="text-center text-xs text-gray-400">Demo checkout: payment is simulated, no card needed.</p>
        </div>
      )}
    </Card>
  );
}
