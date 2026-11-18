import { afterEach, describe, expect, it, vi } from 'vitest';
import { uuid } from './uuid.js';

const V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('uuid', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('makes a random v4 UUID', () => {
    expect(uuid()).toMatch(V4);
    expect(uuid()).not.toBe(uuid());
  });

  it('still works on plain http pages, where crypto.randomUUID does not exist', () => {
    const realCrypto = globalThis.crypto;
    vi.stubGlobal('crypto', { getRandomValues: (bytes) => realCrypto.getRandomValues(bytes) });

    expect(uuid()).toMatch(V4);
    expect(uuid()).not.toBe(uuid());
  });
});
