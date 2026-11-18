# SeatLock

[![CI](https://github.com/Devisri4/seatlock/actions/workflows/ci.yml/badge.svg)](https://github.com/Devisri4/seatlock/actions/workflows/ci.yml)

Ticket booking for shows and events, built around one hard problem:
**many people click the same seat at the same moment, and exactly one of them gets it.**

## The proof

A JUnit test fires **500 parallel HTTP requests at one seat** and checks the result. It runs on every push
against real Postgres 16 and Redis 7 (Testcontainers in GitHub Actions):

| Attack | Result | What stops it |
|---|---|---|
| 500 guests click and pay for the same seat | 1 booking · 499 × `409 Conflict` | Atomic Redis hold |
| The seat holder fires 500 payments at once | 1 booking · 499 × `409 Conflict` | Optimistic locking + primary key |
| One payment retried 500 times (same key) | 1 booking · every retry gets the same booking | Unique idempotency key |

## Features

- **Live seat map** — when anyone holds, releases or buys a seat, it changes colour in every open browser (WebSockets).
- **5-minute hold** — a clicked seat is yours while you pay, with a countdown. Walk away and it frees itself.
- **Never double booked** — three independent lines of defence (below); any one of them alone is enough.
- **Safe Pay button** — double clicks and network retries never book or charge twice.
- **My bookings** — ticket-style list. Guests don't sign up: the browser keeps a random guest id.
- **Rate limit** — 120 API requests per visitor per minute, counted in Redis; more get `429` with `Retry-After`.

Payment is simulated: the point is the booking flow, not payments.

## How a seat is protected

```
click seat ──► Redis  SET seat:42 <guest> NX PX 300000      only one guest can hold it; expires by itself
pay        ──► Postgres transaction
                 UPDATE seats SET status='SOLD', version=v+1 WHERE id=42 AND version=v   optimistic lock
                 INSERT booking_seats (seat_id PK)                                        a seat is sold once
                 INSERT bookings (idempotency_key UNIQUE)                                 a payment is made once
           ──► WebSocket  /topic/shows/7/seats  {"seatId":42,"status":"SOLD"}  to every open browser
```

Holds live in Redis because they are short-lived and frequent, and Redis expiry removes them for free.
Bookings live in Postgres because they are permanent and need transactions. Hold and release are small Lua
scripts, so "check who holds it, then change it" runs as one atomic step. The concurrency test also caught a
real check-then-act race (a seat sold during a hold attempt), now fixed by re-checking after the hold.

## Tech stack

| | |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, Spring WebSocket (STOMP), Lettuce |
| Data | PostgreSQL on Neon, Redis on Upstash (H2 and in-memory stores locally, no setup) |
| Frontend | React 18, React Router, Vite 5, Tailwind CSS, STOMP.js |
| Testing | JUnit 5, Testcontainers (Postgres + Redis), Vitest, Testing Library — 105 backend + 16 frontend tests |
| CI/CD and hosting | GitHub Actions, Docker, Render (API), Vercel (page) |

Security: per-visitor rate limit that can't be dodged with a fake `X-Forwarded-For`, CORS locked to the site,
keys only from environment variables, non-root Docker image.

## Run it locally

```bash
cd backend && ./mvnw spring-boot:run            # API on :8080, 4 demo shows, in-memory DB and holds
cd ../frontend && npm install && npm run dev    # page on :5173
```

Environment variables (all optional locally): `DATABASE_URL`, `REDIS_URL`, `FRONTEND_ORIGIN`, `VITE_API_URL`.
