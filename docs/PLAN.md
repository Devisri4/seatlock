# SeatLock — Plan

SeatLock is a ticket booking system for shows and events, built around one hard problem:
**many people trying to book the same seat at the same moment, and only one of them getting it.**

## Features

| Feature | What the user sees | Engineering behind it |
|---|---|---|
| Browse shows | List of events, show times, seat map | REST API, Postgres |
| Live seat map | A seat turns grey for everyone the moment someone holds it | WebSockets (STOMP) |
| Seat hold with timer | Seats are held for 5 minutes, then released automatically | Redis `SET NX EX` (atomic, expires by itself) |
| No double booking | A seat can only ever be sold once | DB transaction + unique constraint + optimistic locking |
| Safe "Pay" button | Clicking Pay twice never books or charges twice | Idempotency key stored with the booking |
| My bookings | List of confirmed tickets | REST API |
| Rate limit | One visitor can't flood the API | Redis counter per visitor |

Payment is **simulated** (no real money, no Stripe) — the point is the booking flow, not payments.

## Architecture

```
React (Vercel)
   |  REST: browse, hold, pay        |  WebSocket: live seat updates
   v                                 v
Spring Boot API (Render)
   |                         |
   v                         v
Postgres (Neon)          Redis (Upstash)
shows, seats, bookings,  seat holds with expiry,
idempotency keys         rate-limit counters
```

## Key decisions (interview talking points)

| Decision | Why |
|---|---|
| Holds live in Redis, bookings in Postgres | Holds are short-lived and very frequent; Redis expiry removes them for free. Bookings are permanent and need transactions. |
| Redis `SET key value NX EX 300` for a hold | One atomic command: "take this seat only if nobody has it, and forget it after 5 minutes". No race between check and set. |
| Unique constraint on (show, seat) for confirmed bookings | Last line of defence: even if two requests slip past Redis, the database refuses the second one. |
| Idempotency key on payment | Network retries and double clicks are normal; the same key returns the same booking instead of a new one. |
| Concurrency test as proof | A JUnit test fires 500 parallel requests at one seat and asserts exactly 1 booking. This number goes in the README. |
| Guest sessions instead of full sign-up | Keeps scope small; auth was already shown in other projects. |
| Free tools only | Neon, Upstash Redis, Render, Vercel free tiers; Testcontainers for Postgres/Redis in CI. |

## Build steps

| # | Step | Done when | Status |
|---|---|---|---|
| 1 | Repo, Spring Boot + React skeletons, CI, Flyway schema (shows, seats, bookings) | Build + tests green in CI | Done |
| 2 | Shows and seat map API (read-only), seed demo data | API tests pass | Done |
| 3 | Seat hold in Redis: hold, release, auto-expire (Testcontainers Redis in tests) | Tests pass | Next |
| 4 | Booking + payment: transaction, unique constraint, idempotency key | Tests pass | |
| 5 | Concurrency proof: 500 parallel requests on one seat -> exactly 1 booking | Test passes in CI | |
| 6 | WebSockets: live seat updates to every open browser | Two browsers see each other's holds | |
| 7 | React UI: show list, seat map, hold timer, checkout, my bookings | Works in the browser | |
| 8 | Rate limit, deploy (Neon + Upstash + Render + Vercel), README, screenshots | Live demo link | |

Done when the live site lets two people fight over one seat, only one gets it, and CI proves it with the
500-request test.
