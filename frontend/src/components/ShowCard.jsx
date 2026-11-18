import { Link } from 'react-router-dom';
import { ArrowRight, CalendarDays, MapPin } from 'lucide-react';
import { money, showTime } from '../lib/format.js';
import { Badge, cardClass } from './ui.jsx';

// A different gradient per show, so the list doesn't need poster images.
const POSTERS = [
  'from-brand-600 via-brand-700 to-brand-950',
  'from-rose-500 via-fuchsia-600 to-brand-900',
  'from-sky-500 via-brand-600 to-brand-900',
  'from-amber-500 via-orange-600 to-rose-800',
];

export default function ShowCard({ show }) {
  const poster = POSTERS[show.id % POSTERS.length];
  const soldOut = show.availableSeats === 0;
  const left = show.totalSeats === 0 ? 0 : Math.round((show.availableSeats / show.totalSeats) * 100);

  return (
    <Link
      to={`/shows/${show.id}`}
      className={`${cardClass} group flex flex-col overflow-hidden transition hover:-translate-y-0.5 hover:shadow-card-hover`}
    >
      <div className={`relative flex h-36 items-end bg-gradient-to-br p-5 ${poster}`}>
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_20%,rgba(255,255,255,0.25),transparent_55%)]" aria-hidden="true" />
        <h2 className="relative text-xl font-bold leading-tight text-white drop-shadow-sm">{show.title}</h2>
      </div>
      <div className="flex flex-1 flex-col gap-3 p-5">
        <p className="flex items-center gap-2 text-sm text-gray-600">
          <CalendarDays className="h-4 w-4 text-gray-400" aria-hidden="true" />
          {showTime(show.startsAt)}
        </p>
        <p className="flex items-center gap-2 text-sm text-gray-600">
          <MapPin className="h-4 w-4 text-gray-400" aria-hidden="true" />
          {show.venue}
        </p>

        <div className="mt-1">
          <div className="mb-1.5 flex justify-between text-xs font-medium text-gray-500">
            <span>{soldOut ? 'Sold out' : `${show.availableSeats} of ${show.totalSeats} seats left`}</span>
            <span>{left}%</span>
          </div>
          <div className="h-1.5 overflow-hidden rounded-full bg-gray-100">
            <div className="h-full rounded-full bg-seat-available" style={{ width: `${left}%` }} />
          </div>
        </div>

        <div className="mt-auto flex items-center justify-between pt-2">
          {soldOut ? <Badge tone="red">Sold out</Badge> : <Badge tone="brand">From {money(show.priceFromCents)}</Badge>}
          <span className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700">
            Pick seats
            <ArrowRight className="h-4 w-4 transition group-hover:translate-x-0.5" aria-hidden="true" />
          </span>
        </div>
      </div>
    </Link>
  );
}
