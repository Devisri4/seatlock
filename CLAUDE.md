# SeatLock — notes for Claude

Personal portfolio project by Devi (GitHub: Devisri4), built to get SDE / Java / Full Stack roles.
The plan, decisions and step status live in [docs/PLAN.md](docs/PLAN.md) — read it first and update the
status column when a step is finished.

## How to work here
- One step at a time. Explain simply; don't add work that isn't in the plan.
- Verify before saying something works (run the build/tests and show the result).
- One commit per finished step. Tell Devi when to push; Devi pushes (gh: switch to Devisri4 first).
- Commits are authored by Devi only: NO `Co-Authored-By` or any other Claude/AI attribution line in
  commit messages or PR descriptions.
- Free tools only. Never commit keys — they come from environment variables / git-ignored `.env`.
- Keep this project fully separate from any employer code.
- Match the style of Devi's RepoScout project (Downloads\reposcout): same stack versions, CI layout,
  Docker + Render setup, README style.
- UI: professional level, following Devi's vehicle-store design system (Downloads\vehicle-store: Tailwind 3,
  slate greys, Inter, brand colour scale, `shadow-card` cards, sticky blurred navbar, shared `components/ui.jsx`)
  plus ECOMMERCE-APP's `@layer components` classes. SeatLock brand is indigo; seat colours are
  `seat-available` / `seat-held` / `seat-sold` in `frontend/tailwind.config.js`.

## Build
- Company laptop: nothing can be installed. No Node.js and no local Docker.
- Java 21 is at `C:\Java\jdk-21.0.12.1+1` (PATH default is Java 17):
  `$env:JAVA_HOME='C:\Java\jdk-21.0.12.1+1'; cd backend; .\mvnw.cmd verify`
- Tests needing Postgres/Redis (Testcontainers) and the React build run in GitHub Actions CI.
