import { guestId } from './guest.js';

// Where the Spring Boot API lives. Empty in development: Vite forwards /api, /actuator and /ws to localhost:8080.
// In production (Vercel) VITE_API_URL is the Render address, e.g. https://seatlock-api.onrender.com
export const API_BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

/** WebSocket address for live seat updates: same server as the API, ws:// or wss://. */
export function wsUrl() {
  const base = API_BASE || window.location.origin;
  return `${base.replace(/^http/, 'ws')}/ws`;
}

/** An error the API answered with (409 seat taken, 404 ...), or status 0 when the server couldn't be reached. */
export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(path, { method = 'GET', body, headers = {} } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers: {
        'X-Guest-Id': guestId(),
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Could not reach the server. Check your connection and try again.');
  }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.detail ?? `Something went wrong (${response.status})`);
  }
  return data;
}

export const api = {
  shows: () => request('/api/shows'),
  show: (showId) => request(`/api/shows/${showId}`),
  myHolds: (showId) => request(`/api/shows/${showId}/holds/mine`),
  hold: (showId, seatId) => request(`/api/shows/${showId}/seats/${seatId}/hold`, { method: 'PUT' }),
  release: (showId, seatId) => request(`/api/shows/${showId}/seats/${seatId}/hold`, { method: 'DELETE' }),
  /** Same idempotencyKey on a retry = the server returns the same booking instead of charging again. */
  book: (showId, seatIds, idempotencyKey) =>
    request(`/api/shows/${showId}/bookings`, {
      method: 'POST',
      body: { seatIds },
      headers: { 'Idempotency-Key': idempotencyKey },
    }),
  myBookings: () => request('/api/bookings/mine'),
};

/** True when the API answers its health check with status UP. Also wakes up the free server. */
export async function isApiUp() {
  try {
    const response = await fetch(`${API_BASE}/actuator/health`);
    if (!response.ok) return false;
    const body = await response.json();
    return body.status === 'UP';
  } catch {
    return false;
  }
}
