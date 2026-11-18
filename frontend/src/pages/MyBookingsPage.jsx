import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { RefreshCw, Ticket } from 'lucide-react';
import { api } from '../lib/api.js';
import TicketCard from '../components/TicketCard.jsx';
import { EmptyState, PageHeader, primaryButtonClass, secondaryButtonClass, Skeleton } from '../components/ui.jsx';

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    setError(null);
    api.myBookings().then(setBookings).catch(setError);
  }, []);

  useEffect(load, [load]);

  return (
    <>
      <PageHeader
        eyebrow="Your tickets"
        title="My bookings"
        description="Bookings are saved for this browser. No account needed."
      />

      {error ? (
        <EmptyState
          icon={Ticket}
          title="Couldn't load your bookings"
          text={error.message}
          action={
            <button type="button" onClick={load} className={secondaryButtonClass}>
              <RefreshCw className="h-4 w-4" aria-hidden="true" /> Try again
            </button>
          }
        />
      ) : bookings === null ? (
        <div className="space-y-4" role="status" aria-label="Loading bookings">
          <Skeleton className="h-40 w-full rounded-2xl" />
          <Skeleton className="h-40 w-full rounded-2xl" />
        </div>
      ) : bookings.length === 0 ? (
        <EmptyState
          icon={Ticket}
          title="No bookings yet"
          text="When you pay for seats, your tickets show up here."
          action={
            <Link to="/" className={primaryButtonClass}>
              Browse shows
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {bookings.map((booking) => (
            <TicketCard key={booking.id} booking={booking} />
          ))}
        </div>
      )}
    </>
  );
}
