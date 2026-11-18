import { describe, expect, it } from 'vitest';
import { countdown, money } from './format.js';

describe('format', () => {
  it('shows cents as dollars', () => {
    expect(money(4500)).toBe('$45.00');
    expect(money(7550)).toBe('$75.50');
  });

  it('shows time left as m:ss and never goes negative', () => {
    expect(countdown(5 * 60 * 1000)).toBe('5:00');
    expect(countdown(65_000)).toBe('1:05');
    expect(countdown(400)).toBe('0:01');
    expect(countdown(-3000)).toBe('0:00');
  });
});
