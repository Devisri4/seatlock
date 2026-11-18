import { vi } from 'vitest';

/**
 * Fakes the backend: routes is a list of [method, pathRegex, handler]. A handler returns
 * { status, body } or throws (to simulate the network failing). Returns the fetch spy.
 */
export function mockApi(routes) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (url, init = {}) => {
    const method = init.method ?? 'GET';
    const path = new URL(url, 'http://localhost').pathname;
    const route = routes.find(([m, pattern]) => m === method && pattern.test(path));
    if (!route) throw new Error(`No mock for ${method} ${path}`);
    const { status = 200, body = null } = await route[2]({ path, init });
    return {
      ok: status >= 200 && status < 300,
      status,
      json: async () => body,
    };
  });
}

export const SHOW = {
  id: 1,
  title: 'Midnight Jazz Session',
  venue: 'Blue Room, Riverside Hall',
  startsAt: '2030-01-02T15:30:00Z',
  rows: [
    {
      label: 'A',
      seats: [
        { id: 11, number: 1, priceCents: 4500, status: 'AVAILABLE', heldByYou: false },
        { id: 12, number: 2, priceCents: 4500, status: 'HELD', heldByYou: false },
        { id: 13, number: 3, priceCents: 4500, status: 'SOLD', heldByYou: false },
      ],
    },
    {
      label: 'B',
      seats: [{ id: 21, number: 1, priceCents: 2000, status: 'AVAILABLE', heldByYou: false }],
    },
  ],
};
