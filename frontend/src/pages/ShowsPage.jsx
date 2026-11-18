import { useCallback, useEffect, useState } from 'react';
import { CalendarX2, RefreshCw } from 'lucide-react';
import { api } from '../lib/api.js';
import ShowCard from '../components/ShowCard.jsx';
import { cardClass, EmptyState, PageHeader, secondaryButtonClass, Skeleton } from '../components/ui.jsx';

function ShowCardSkeleton() {
  return (
    <div className={`${cardClass} overflow-hidden`} aria-hidden="true">
      <Skeleton className="h-36 rounded-none" />
      <div className="space-y-3 p-5">
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-2 w-full" />
        <Skeleton className="h-6 w-1/3" />
      </div>
    </div>
  );
}

export default function ShowsPage() {
  const [shows, setShows] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    setError(null);
    api.shows().then(setShows).catch(setError);
  }, []);

  useEffect(load, [load]);

  return (
    <>
      <PageHeader
        eyebrow="Upcoming shows"
        title="Pick a seat. Keep it. Pay once."
        description="Seats are held for you for 5 minutes and every open browser sees them change live. However many people click the same seat, only one gets it."
      />

      {error ? (
        <EmptyState
          icon={CalendarX2}
          title="Couldn't load shows"
          text={`${error.message} The free server may take up to a minute to wake up.`}
          action={
            <button type="button" onClick={load} className={secondaryButtonClass}>
              <RefreshCw className="h-4 w-4" aria-hidden="true" /> Try again
            </button>
          }
        />
      ) : shows === null ? (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3" role="status" aria-label="Loading shows">
          {[0, 1, 2].map((i) => (
            <ShowCardSkeleton key={i} />
          ))}
        </div>
      ) : shows.length === 0 ? (
        <EmptyState icon={CalendarX2} title="No upcoming shows" text="New shows are added automatically. Check back soon." />
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {shows.map((show) => (
            <ShowCard key={show.id} show={show} />
          ))}
        </div>
      )}
    </>
  );
}
