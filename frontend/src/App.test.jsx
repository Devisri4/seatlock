import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import App from './App.jsx';
import { mockApi } from './test/mockApi.js';

vi.mock('./lib/live.js', () => ({ useSeatUpdates: () => 'live' }));

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  );
}

const SHOWS = [
  { id: 1, title: 'Midnight Jazz Session', venue: 'Blue Room', startsAt: '2030-01-02T15:30:00Z', totalSeats: 96, availableSeats: 90, priceFromCents: 2000 },
  { id: 2, title: 'Indie Rock Live', venue: 'The Warehouse', startsAt: '2030-01-08T14:30:00Z', totalSeats: 96, availableSeats: 0, priceFromCents: 2000 },
];

describe('App', () => {
  it('lists upcoming shows with seats left and the lowest price', async () => {
    mockApi([
      ['GET', /^\/actuator\/health$/, () => ({ body: { status: 'UP' } })],
      ['GET', /^\/api\/shows$/, () => ({ body: SHOWS })],
    ]);

    renderAt('/');

    expect(await screen.findByRole('heading', { name: 'Midnight Jazz Session' })).toBeInTheDocument();
    expect(screen.getByText('90 of 96 seats left')).toBeInTheDocument();
    expect(screen.getByText('From $20.00')).toBeInTheDocument();
    expect(screen.getAllByText('Sold out').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: /Midnight Jazz Session/ })).toHaveAttribute('href', '/shows/1');
    expect(await screen.findByText('API online')).toBeInTheDocument();
  });

  it('explains when the server cannot be reached', async () => {
    mockApi([
      ['GET', /^\/actuator\/health$/, () => ({ status: 503 })],
      ['GET', /^\/api\/shows$/, () => {
        throw new TypeError('Failed to fetch');
      }],
    ]);

    renderAt('/');

    expect(await screen.findByText("Couldn't load shows")).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Try again/ })).toBeInTheDocument();
    expect(await screen.findByText('API offline')).toBeInTheDocument();
  });

  it('shows an empty state when there are no bookings yet', async () => {
    mockApi([
      ['GET', /^\/actuator\/health$/, () => ({ body: { status: 'UP' } })],
      ['GET', /^\/api\/bookings\/mine$/, () => ({ body: [] })],
    ]);

    renderAt('/bookings');

    expect(await screen.findByText('No bookings yet')).toBeInTheDocument();
  });

  it('has a not-found page', () => {
    mockApi([['GET', /^\/actuator\/health$/, () => ({ body: { status: 'UP' } })]]);

    renderAt('/nowhere');

    expect(screen.getByText('Page not found')).toBeInTheDocument();
  });
});
