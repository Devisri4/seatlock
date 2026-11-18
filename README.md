# SeatLock

[![CI](https://github.com/Devisri4/seatlock/actions/workflows/ci.yml/badge.svg)](https://github.com/Devisri4/seatlock/actions/workflows/ci.yml)

A ticket booking system for shows and events, built around one hard problem:
**many people trying to book the same seat at the same moment, and only one of them getting it.**

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL · Flyway · Redis · WebSockets · React 18 · Tailwind CSS · GitHub Actions

Work in progress — see [docs/PLAN.md](docs/PLAN.md) for the plan and status.

## Run locally

```bash
# Backend (uses an in-memory database, no setup needed) - http://localhost:8080/actuator/health
cd backend && ./mvnw spring-boot:run

# Frontend - http://localhost:5173
cd frontend && npm install && npm run dev
```
