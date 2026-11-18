// Guests don't sign up. The browser makes a random id once and keeps it, so holds and bookings
// survive a page reload. If storage is blocked (private mode), the id lasts until the tab closes.
import { uuid } from './uuid.js';

const KEY = 'seatlock.guestId';
let memoryId = null;

function newId() {
  return uuid();
}

export function guestId() {
  try {
    let id = window.localStorage.getItem(KEY);
    if (!id) {
      id = newId();
      window.localStorage.setItem(KEY, id);
    }
    return id;
  } catch {
    memoryId = memoryId ?? newId();
    return memoryId;
  }
}
