import { CalendarDays, MapPin } from 'lucide-react';
import { money, showTime } from '../lib/format.js';
import { Badge, cardClass } from './ui.jsx';

/** A confirmed booking, styled as a ticket with a tear-off stub. */
export default function TicketCard({ booking, highlight = false }) {
  return (
    <article
      className={`${cardClass} flex flex-col overflow-hidden sm:flex-row ${highlight ? 'ring-2 ring-emerald-300' : ''}`}
      aria-label={`Booking ${booking.id} for ${booking.showTitle}`}
    >
      <div className="flex-1 p-5 sm:p-6">
        <div className="flex items-start justify-between gap-3">
          <h3 className="text-lg font-semibold text-gray-900">{booking.showTitle}</h3>
          <Badge tone="green">Confirmed</Badge>
        </div>
        <p className="mt-3 flex items-center gap-2 text-sm text-gray-600">
          <CalendarDays className="h-4 w-4 text-gray-400" aria-hidden="true" />
          {showTime(booking.startsAt)}
        </p>
        <p className="mt-1.5 flex items-center gap-2 text-sm text-gray-600">
          <MapPin className="h-4 w-4 text-gray-400" aria-hidden="true" />
          {booking.venue}
        </p>
        <ul className="mt-4 flex flex-wrap gap-2" aria-label="Seats">
          {booking.seats.map((seat) => (
            <li key={seat} className="rounded-md bg-brand-50 px-2.5 py-1 text-sm font-semibold text-brand-700 ring-1 ring-inset ring-brand-100">
              {seat}
            </li>
          ))}
        </ul>
      </div>
      <div className="flex items-center justify-between gap-4 border-t-2 border-dashed border-gray-200 bg-gray-50 px-5 py-4 sm:w-44 sm:flex-col sm:items-start sm:justify-center sm:border-l-2 sm:border-t-0">
        <div>
          <p className="text-xs uppercase tracking-wider text-gray-400">Booking</p>
          <p className="font-mono text-sm font-semibold text-gray-900">#{String(booking.id).padStart(6, '0')}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wider text-gray-400">Paid</p>
          <p className="text-lg font-bold text-gray-900">{money(booking.totalCents)}</p>
        </div>
      </div>
    </article>
  );
}
