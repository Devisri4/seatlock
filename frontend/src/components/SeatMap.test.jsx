import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import SeatMap, { priceTiers } from './SeatMap.jsx';
import { SHOW } from '../test/mockApi.js';

describe('SeatMap', () => {
  it('describes every seat for screen readers', () => {
    render(<SeatMap rows={SHOW.rows} onSeatClick={() => {}} />);

    expect(screen.getByRole('button', { name: 'Seat A1, $45.00, available' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Seat A2, $45.00, held by someone else' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Seat A3, $45.00, sold' })).toBeDisabled();
  });

  it('lets you click available seats and your own held seats only', async () => {
    const onSeatClick = vi.fn();
    const rows = [{ label: 'A', seats: [{ ...SHOW.rows[0].seats[0], status: 'HELD', heldByYou: true }, SHOW.rows[0].seats[2]] }];
    render(<SeatMap rows={rows} onSeatClick={onSeatClick} />);

    await userEvent.click(screen.getByRole('button', { name: /A1.*held by you/ }));
    await userEvent.click(screen.getByRole('button', { name: /A3.*sold/ }));

    expect(onSeatClick).toHaveBeenCalledTimes(1);
    expect(onSeatClick).toHaveBeenCalledWith(expect.objectContaining({ id: 11 }), 'A');
  });

  it('works out price tiers from the rows', () => {
    const rows = ['A', 'B', 'C', 'D'].map((label, i) => ({ label, seats: [{ priceCents: i < 2 ? 4500 : 2000 }] }));

    expect(priceTiers(rows)).toEqual([
      { rows: 'A–B', price: '$45.00' },
      { rows: 'C–D', price: '$20.00' },
    ]);
  });
});
