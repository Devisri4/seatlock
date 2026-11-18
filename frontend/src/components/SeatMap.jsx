import { Loader2 } from 'lucide-react';
import { money } from '../lib/format.js';

const SEAT_STYLES = {
  AVAILABLE: 'border-emerald-400 bg-white text-emerald-700 hover:bg-emerald-50 hover:border-emerald-500 cursor-pointer',
  MINE: 'border-brand-700 bg-brand-600 text-white shadow-sm ring-2 ring-brand-200 cursor-pointer hover:bg-brand-700',
  HELD: 'border-amber-300 bg-amber-100 text-amber-700 cursor-not-allowed',
  SOLD: 'border-gray-200 bg-gray-200 text-gray-400 cursor-not-allowed',
};

const STATE_TEXT = {
  AVAILABLE: 'available',
  MINE: 'held by you, click to release',
  HELD: 'held by someone else',
  SOLD: 'sold',
};

export function seatKind(seat) {
  if (seat.status === 'HELD' && seat.heldByYou) return 'MINE';
  return seat.status;
}

export const LEGEND = [
  { kind: 'AVAILABLE', label: 'Available' },
  { kind: 'MINE', label: 'Your seats' },
  { kind: 'HELD', label: 'Held by others' },
  { kind: 'SOLD', label: 'Sold' },
];

/**
 * The seat map: one line per row, front row (A) nearest the stage. Only available seats and your own
 * held seats can be clicked; the rest are shown but disabled.
 */
export default function SeatMap({ rows, onSeatClick, busySeatId }) {
  return (
    <div className="overflow-x-auto pb-2">
      <div className="mx-auto w-max min-w-full">
        <div className="mx-auto mb-8 max-w-md">
          <div className="h-2 rounded-full bg-gradient-to-r from-brand-200 via-brand-500 to-brand-200" aria-hidden="true" />
          <p className="mt-2 text-center text-xs font-semibold uppercase tracking-[0.3em] text-gray-400">Stage</p>
        </div>

        <div className="space-y-1.5 sm:space-y-2">
          {rows.map((row) => (
            <div key={row.label} className="flex items-center justify-center gap-2 sm:gap-3">
              <span className="w-4 text-center text-xs font-semibold text-gray-400">{row.label}</span>
              <div className="flex gap-1 sm:gap-1.5">
                {row.seats.map((seat, index) => {
                  const kind = seatKind(seat);
                  const clickable = kind === 'AVAILABLE' || kind === 'MINE';
                  const busy = busySeatId === seat.id;
                  const aisle = index === Math.floor(row.seats.length / 2) - 1;
                  return (
                    <button
                      key={seat.id}
                      type="button"
                      disabled={!clickable || busy}
                      onClick={() => onSeatClick(seat, row.label)}
                      aria-label={`Seat ${row.label}${seat.number}, ${money(seat.priceCents)}, ${STATE_TEXT[kind]}`}
                      aria-pressed={kind === 'MINE'}
                      title={`${row.label}${seat.number} · ${money(seat.priceCents)}`}
                      className={`flex h-6 w-6 items-center justify-center rounded-t-lg rounded-b-md border text-[10px] font-semibold transition sm:h-8 sm:w-8 sm:text-xs ${
                        SEAT_STYLES[kind]
                      } ${aisle ? 'mr-3 sm:mr-5' : ''}`}
                    >
                      {busy ? <Loader2 className="h-3 w-3 animate-spin" aria-hidden="true" /> : seat.number}
                    </button>
                  );
                })}
              </div>
              <span className="w-4 text-center text-xs font-semibold text-gray-400">{row.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export function SeatLegend() {
  return (
    <ul className="flex flex-wrap gap-x-5 gap-y-2 text-sm text-gray-600" aria-label="Seat colours">
      {LEGEND.map(({ kind, label }) => (
        <li key={kind} className="flex items-center gap-2">
          <span className={`h-4 w-4 rounded-t-md rounded-b-sm border ${SEAT_STYLES[kind].split(' cursor')[0]}`} aria-hidden="true" />
          {label}
        </li>
      ))}
    </ul>
  );
}

/** "A–B $45.00 · C–F $30.00 · G–H $20.00" worked out from the seat map itself. */
export function priceTiers(rows) {
  const tiers = [];
  for (const row of rows) {
    const price = row.seats[0]?.priceCents;
    if (price === undefined) continue;
    const last = tiers[tiers.length - 1];
    if (last && last.price === price) {
      last.to = row.label;
    } else {
      tiers.push({ price, from: row.label, to: row.label });
    }
  }
  return tiers.map((t) => ({ rows: t.from === t.to ? t.from : `${t.from}–${t.to}`, price: money(t.price) }));
}
