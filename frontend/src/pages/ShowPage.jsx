import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CalendarDays, CalendarX2, MapPin, PartyPopper, Ticket } from 'lucide-react';
import { api } from '../lib/api.js';
import { showTime } from '../lib/format.js';
import { useSeatUpdates } from '../lib/live.js';
import { uuid } from '../lib/uuid.js';
import CheckoutPanel from '../components/CheckoutPanel.jsx';
import SeatMap, { priceTiers, SeatLegend } from '../components/SeatMap.jsx';
import TicketCard from '../components/TicketCard.jsx';
import { Badge, Card, EmptyState, Notice, primaryButtonClass, secondaryButtonClass, Skeleton } from '../components/ui.jsx';

const LIVE_BADGE = {
  live: { tone: 'green', dot: 'bg-emerald-500 animate-pulse', label: 'Live' },
  connecting: { tone: 'neutral', dot: 'bg-gray-400', label: 'Connecting…' },
  offline: { tone: 'amber', dot: 'bg-amber-500', label: 'Reconnecting…' },
};

function replaceSeat(show, seatId, status, heldByYou) {
  if (!show) return show;
  return {
    ...show,
    rows: show.rows.map((row) => ({
      ...row,
      seats: row.seats.map((seat) => (seat.id === seatId ? { ...seat, status, heldByYou } : seat)),
    })),
  };
}

function without(holds, seatId) {
  if (!(seatId in holds)) return holds;
  const next = { ...holds };
  delete next[seatId];
  return next;
}

export default function ShowPage() {
  const { showId } = useParams();
  const [show, setShow] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [holds, setHolds] = useState({}); // seatId -> hold end (ms since epoch)
  const [busySeatId, setBusySeatId] = useState(null);
  const [notice, setNotice] = useState(null);
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState(null);
  const [booking, setBooking] = useState(null);

  // One key per checkout: a retry after a network error reuses it, so the server never books twice.
  const paymentKey = useRef(null);
  const holdsRef = useRef(holds);
  holdsRef.current = holds;

  const load = useCallback(async () => {
    try {
      const [detail, mine] = await Promise.all([api.show(showId), api.myHolds(showId)]);
      setShow(detail);
      setHolds(Object.fromEntries(mine.map((hold) => [hold.seatId, Date.parse(hold.expiresAt)])));
      setLoadError(null);
    } catch (error) {
      setLoadError(error);
    }
  }, [showId]);

  useEffect(() => {
    load();
  }, [load]);

  // Someone (maybe in another tab) held, released or bought a seat, or a hold ran out.
  const onSeatUpdate = useCallback(({ seatId, status }) => {
    const mine = seatId in holdsRef.current;
    setShow((current) => replaceSeat(current, seatId, status, status === 'HELD' && mine));
    if (status !== 'HELD' && mine) {
      setHolds((current) => without(current, seatId));
      if (status === 'AVAILABLE') setNotice({ type: 'info', text: 'One of your holds ran out, so that seat is free again.' });
    }
  }, []);
  const live = useSeatUpdates(showId, onSeatUpdate, load);

  const mySeats = useMemo(() => {
    if (!show) return [];
    return show.rows.flatMap((row) =>
      row.seats
        .filter((seat) => seat.id in holds)
        .map((seat) => ({ ...seat, label: `${row.label}${seat.number}`, expiresAt: holds[seat.id] })),
    );
  }, [show, holds]);

  async function toggleSeat(seat, rowLabel) {
    if (busySeatId) return;
    setNotice(null);
    setPayError(null);
    setBooking(null);
    setBusySeatId(seat.id);
    try {
      if (seat.status === 'HELD' && seat.heldByYou) {
        await api.release(showId, seat.id);
        setHolds((current) => without(current, seat.id));
        setShow((current) => replaceSeat(current, seat.id, 'AVAILABLE', false));
      } else {
        const hold = await api.hold(showId, seat.id);
        setHolds((current) => ({ ...current, [seat.id]: Date.parse(hold.expiresAt) }));
        setShow((current) => replaceSeat(current, seat.id, 'HELD', true));
      }
      paymentKey.current = null; // different seats = a different payment
    } catch (error) {
      setNotice({
        type: 'error',
        text: error.status === 409 ? `Too late: ${error.message.charAt(0).toLowerCase()}${error.message.slice(1)}.` : error.message,
      });
      if (error.status === 409) load();
    } finally {
      setBusySeatId(null);
    }
  }

  async function pay() {
    const seatIds = mySeats.map((seat) => seat.id);
    paymentKey.current = paymentKey.current ?? uuid();
    setPaying(true);
    setPayError(null);
    try {
      const result = await api.book(showId, seatIds, paymentKey.current);
      paymentKey.current = null;
      setBooking(result);
      setHolds({});
      setShow((current) => seatIds.reduce((s, id) => replaceSeat(s, id, 'SOLD', false), current));
    } catch (error) {
      if (error.status === 0) {
        setPayError(`${error.message} Retrying is safe: you won't be charged twice.`);
      } else {
        paymentKey.current = null;
        setPayError(error.message);
        if (error.status === 409) load();
      }
    } finally {
      setPaying(false);
    }
  }

  const onHoldExpired = useCallback(() => {
    setNotice({ type: 'info', text: 'Your hold ran out, so those seats are free again. Pick them again if they are still available.' });
    load();
  }, [load]);

  if (loadError) {
    return (
      <EmptyState
        icon={CalendarX2}
        title={loadError.status === 404 ? 'Show not found' : "Couldn't load this show"}
        text={loadError.message}
        action={
          <Link to="/" className={secondaryButtonClass}>
            <ArrowLeft className="h-4 w-4" aria-hidden="true" /> All shows
          </Link>
        }
      />
    );
  }

  if (!show) {
    return (
      <div role="status" aria-label="Loading show" className="space-y-6">
        <Skeleton className="h-9 w-2/3" />
        <Skeleton className="h-5 w-1/3" />
        <Skeleton className="h-80 w-full rounded-2xl" />
      </div>
    );
  }

  const badge = LIVE_BADGE[live];

  return (
    <>
      <Link to="/" className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-gray-500 transition hover:text-brand-700">
        <ArrowLeft className="h-4 w-4" aria-hidden="true" /> All shows
      </Link>

      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">{show.title}</h1>
          <div className="mt-3 flex flex-wrap gap-x-5 gap-y-1.5 text-sm text-gray-600">
            <span className="flex items-center gap-2">
              <CalendarDays className="h-4 w-4 text-gray-400" aria-hidden="true" />
              {showTime(show.startsAt)}
            </span>
            <span className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-gray-400" aria-hidden="true" />
              {show.venue}
            </span>
          </div>
        </div>
        <Badge tone={badge.tone} title="Seat changes from other visitors appear instantly">
          <span className={`h-1.5 w-1.5 rounded-full ${badge.dot}`} aria-hidden="true" />
          {badge.label}
        </Badge>
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <Card className="p-4 sm:p-8">
          {show.rows.length === 0 ? (
            <p className="py-12 text-center text-sm text-gray-500">Seats for this show are not on sale yet.</p>
          ) : (
            <SeatMap rows={show.rows} onSeatClick={toggleSeat} busySeatId={busySeatId} />
          )}
          <div className="mt-8 flex flex-col gap-4 border-t border-gray-100 pt-6 sm:flex-row sm:items-center sm:justify-between">
            <SeatLegend />
            <p className="text-sm text-gray-500">
              {priceTiers(show.rows).map((tier, i) => (
                <span key={tier.rows}>
                  {i > 0 && <span className="mx-1.5 text-gray-300">·</span>}
                  Rows {tier.rows} <span className="font-semibold text-gray-700">{tier.price}</span>
                </span>
              ))}
            </p>
          </div>
        </Card>

        <div className="space-y-4 lg:sticky lg:top-24">
          {notice && <Notice type={notice.type}>{notice.text}</Notice>}
          {booking ? (
            <Card className="p-5 sm:p-6">
              <div className="flex items-center gap-3">
                <span className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                  <PartyPopper className="h-5 w-5" aria-hidden="true" />
                </span>
                <div>
                  <h2 className="text-base font-semibold text-gray-900">You're going!</h2>
                  <p className="text-sm text-gray-500">Your seats are booked.</p>
                </div>
              </div>
              <div className="mt-5">
                <TicketCard booking={booking} highlight />
              </div>
              <div className="mt-5 grid grid-cols-2 gap-3">
                <Link to="/bookings" className={primaryButtonClass}>
                  <Ticket className="h-4 w-4" aria-hidden="true" /> My bookings
                </Link>
                <button type="button" onClick={() => setBooking(null)} className={secondaryButtonClass}>
                  Book more
                </button>
              </div>
            </Card>
          ) : (
            <CheckoutPanel
              seats={mySeats}
              onRelease={(seat) => toggleSeat(seat)}
              onPay={pay}
              onExpire={onHoldExpired}
              paying={paying}
              error={payError}
              busySeatId={busySeatId}
            />
          )}
        </div>
      </div>
    </>
  );
}
