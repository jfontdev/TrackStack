# TrackStack

TrackStack is a DJ Set Planner & Performance Intelligence Platform — a Spring Boot backend built to manage a DJ's track library, log transitions between tracks, plan setlists with energy arcs, and provide intelligent performance insights.

Originally started as a generic music API, the project was intentionally refocused around the real-world DJ workflow: scanning audio files, building a transition graph, planning setlists, and analyzing what actually happened in a set.

This project is intentionally built as a learning-driven, enterprise-style modular monolith using modern Spring Boot
practices.

---

## Tech Stack

- Java 25
- Spring Boot 3.x
- Spring Web (REST)
- Spring Data JPA
- PostgreSQL (local)
- Flyway (database migrations)
- Redis with RedisJSON module (via redis/redis-stack)
- Spring Cache + RedisJSON serializer
- JAudioTagger (audio metadata extraction)
- Spring AI + Ollama (local LLM integration)
- Testcontainers (PostgreSQL + Redis for integration tests)
- Rest Assured (API integration testing)
- Swagger / OpenAPI

---

## Architecture Overview

This project follows a layered architecture:

Controller → Service → Repository → Database (PostgreSQL)

### Domain

The backend models a complete DJ workflow:

- **Track Library** — auto-populated from audio files via JAudioTagger (BPM, key, genre, duration)
- **Transition Graph** — directed relationships between tracks with ratings, key compatibility (Camelot wheel), and BPM difference
- **Setlists** — ordered sequences of tracks with energy levels and lifecycle status (DRAFT → READY → PERFORMED)
- **AI Layer** — local LLM (Ollama) for transition suggestions and setlist analysis, with rule-based fallback when AI is unavailable

### Layers

- **Controllers** handle HTTP transport only. No entity exposure.
- **Services** contain business logic, DTO mapping, and AI orchestration.
- **Repositories** are pure CRUD (Spring Data JPA). No custom queries beyond Spring Data method names.
- **Entities** encapsulate creation logic via static factory methods.
- **DTOs** define API boundaries. Manual mapping in services.
- **Cache** is managed at the service layer via Spring Cache + RedisJSON.

---

## Design Principles

- Explicit over magic
- No premature abstraction
- Repositories remain pure CRUD
- Services orchestrate logic
- DTOs separate persistence from API
- Optional is handled explicitly (no chained functional style)
- No global mutable state
- No hidden ORM tricks

---

## Caching Strategy

- Caching is applied at the service layer only.
- Read operations use `@Cacheable`.
- Write operations invalidate cache via `@CacheEvict`.
- Redis with RedisJSON module stores values as native JSON objects.
- Custom `RedisJSONSerializer` wraps values in a lightweight `{"_type": "...", "payload": {...}}` envelope, avoiding Jackson's `@class` type annotation hacks.
- `RedisJSONService` provides client-side JSONPath-like queries on cached data (e.g., `$.bpm > 130`).

---

## Testing Strategy

- Integration tests use Testcontainers.
- API tests use Rest Assured.
- Flyway migrations run during test startup.
- Full stack is tested: Controller → Service → Repository → DB.
- No mocking repositories in integration tests.

---

## Domain Features

### Track Library
- Auto-scan audio files from a configured directory
- Extract metadata: BPM, musical key (Camelot), genre, duration, file format
- Folder structure used as genre taxonomy
- Duplicate detection by file path

### Transition Graph
- Log directed transitions between tracks (A→B is separate from B→A)
- Auto-calculate harmonic key compatibility (Camelot wheel rules)
- Auto-calculate BPM difference
- Rate transitions 1-5, with notes and mixing style

### Set Planning
- Build setlists with ordered slots
- Assign energy levels (1-5) per slot for energy arc planning
- Lifecycle: DRAFT → READY → PERFORMED
- Reorder slots, validate track existence

### AI Integration (Local)
- `POST /api/ai/transitions/suggest` — smart next-track suggestions with reasoning
- `POST /api/ai/setlists/analyze` — critique energy arc and flow
- `POST /api/ai/setlists/generate` — generate setlist from natural language
- Falls back to rule-based logic when Ollama is unavailable
- Core API works 100% without AI

## Non-Goals

- No microservices.
- No reactive stack.
- No front-end (API-only).
- No premature distributed caching.
- No entity exposure in API layer.
- AI is enhancement, not required for core functionality.

This project prioritizes clarity, correctness, and real-world backend practices.
