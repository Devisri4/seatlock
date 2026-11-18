const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

export function money(cents) {
  return usd.format(cents / 100);
}

/** "Thu, Nov 19 · 9:00 PM" in the viewer's own time zone. */
export function showTime(iso) {
  const date = new Date(iso);
  const day = date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
  const time = date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  return `${day} · ${time}`;
}

/** Milliseconds left -> "4:05". Never negative. */
export function countdown(ms) {
  const total = Math.max(0, Math.ceil(ms / 1000));
  const minutes = Math.floor(total / 60);
  const seconds = String(total % 60).padStart(2, '0');
  return `${minutes}:${seconds}`;
}
