import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ShowPage from './ShowPage.jsx';
import { mockApi, SHOW } from '../test/mockApi.js';

// Capture the live-update callback so tests can play "another visitor".
let liveUpdate = null;
vi.mock('../lib/live.js', () => ({
  useSeatUpdates: (showId, onUpdate) => {
    liveUpdate = onUpdate;
    return 'live';
  },
}));

const inFiveMinutes = () => new Date(Date.now() + 5 * 60 * 1000).toISOString();

const BOOKING = {
  id: 42,
  showId: 1,
  showTitle: SHOW.title,
  venue: SHOW.venue,
  startsAt: SHOW.startsAt,
  seats: ['A1'],
  totalCents: 4500,
  createdAt: '2030-01-01T00:00:00Z',
};

function renderShow() {
  return render(
    <MemoryRouter initialEntries={['/shows/1']}>
      <Routes>
        <Route path="/shows/:showId" element={<ShowPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

function baseRoutes(extra = []) {
  return [
    ...extra,
    ['GET', /^\/api\/shows\/1$/, () => ({ body: SHOW })],
    ['GET', /^\/api\/shows\/1\/holds\/mine$/, () => ({ body: [] })],
    ['PUT', /^\/api\/shows\/1\/seats\/11\/hold$/, () => ({ body: { seatId: 11, expiresAt: inFiveMinutes() } })],
  ];
}

describe('ShowPage', () => {
  it('holds a clicked seat and shows it in the checkout with a countdown', async () => {
    mockApi(baseRoutes());
    renderShow();

    await userEvent.click(await screen.findByRole('button', { name: 'Seat A1, $45.00, available' }));

    expect(await screen.findByRole('button', { name: 'Seat A1, $45.00, held by you, click to release' })).toBeInTheDocument();
    expect(screen.getByText('Seat A1')).toBeInTheDocument();
    expect(screen.getByRole('timer')).toHaveTextContent(/^[45]:\d\d$/);
    expect(screen.getByRole('button', { name: 'Pay $45.00' })).toBeEnabled();
  });

  it('retries a failed payment with the same Idempotency-Key, so it can never book twice', async () => {
    let attempt = 0;
    const fetchSpy = mockApi(baseRoutes([
      ['POST', /^\/api\/shows\/1\/bookings$/, () => {
        attempt += 1;
        if (attempt === 1) throw new TypeError('Failed to fetch');
        return { status: 201, body: BOOKING };
      }],
    ]));
    renderShow();
    await userEvent.click(await screen.findByRole('button', { name: /Seat A1, .*available/ }));

    await userEvent.click(await screen.findByRole('button', { name: 'Pay $45.00' }));
    expect(await screen.findByText(/Retrying is safe/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Pay $45.00' }));

    expect(await screen.findByText("You're going!")).toBeInTheDocument();
    const payments = fetchSpy.mock.calls.filter(([, init]) => init?.method === 'POST');
    expect(payments).toHaveLength(2);
    const keys = payments.map(([, init]) => init.headers['Idempotency-Key']);
    expect(keys[0]).toMatch(/^[0-9a-f-]{36}$/);
    expect(keys[1]).toBe(keys[0]);
    expect(JSON.parse(payments[1][1].body)).toEqual({ seatIds: [11] });
  });

  it('explains when someone else got the seat first', async () => {
    mockApi([
      ['PUT', /^\/api\/shows\/1\/seats\/21\/hold$/, () => ({ status: 409, body: { detail: 'Seat B1 is held by someone else' } })],
      ...baseRoutes(),
    ]);
    renderShow();

    await userEvent.click(await screen.findByRole('button', { name: /Seat B1, .*available/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Too late: seat B1 is held by someone else.');
  });

  it('updates the seat map live when another visitor holds or buys a seat', async () => {
    mockApi(baseRoutes());
    renderShow();
    await screen.findByRole('button', { name: /Seat A1, .*available/ });

    act(() => liveUpdate({ seatId: 21, status: 'HELD' }));
    expect(await screen.findByRole('button', { name: 'Seat B1, $20.00, held by someone else' })).toBeDisabled();

    act(() => liveUpdate({ seatId: 21, status: 'SOLD' }));
    expect(await screen.findByRole('button', { name: 'Seat B1, $20.00, sold' })).toBeDisabled();
  });

  it('shows the booked ticket after paying', async () => {
    mockApi(baseRoutes([['POST', /^\/api\/shows\/1\/bookings$/, () => ({ status: 201, body: BOOKING })]]));
    renderShow();
    await userEvent.click(await screen.findByRole('button', { name: /Seat A1, .*available/ }));

    await userEvent.click(await screen.findByRole('button', { name: 'Pay $45.00' }));

    const ticket = await screen.findByRole('article', { name: /Booking 42/ });
    expect(within(ticket).getByText('#000042')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Seat A1, $45.00, sold' })).toBeDisabled();
  });
});
