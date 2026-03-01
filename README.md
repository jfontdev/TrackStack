# TrackStack API

TrackStack is a Spring Boot backend API for managing DJ tracks, playlists, and tags.

This project is intentionally built as a learning-driven, enterprise-style modular monolith using modern Spring Boot
practices.

---

## Tech Stack

- Java 25
- Spring Boot 3.x
- Spring Web (REST)
- Spring Data JPA
- PostgreSQL
- Flyway (database migrations)
- Spring Cache (in-memory for now)
- Testcontainers (integration testing)
- Rest Assured (integration testing)
- Swagger / OpenAPI

---

## Architecture Overview

This project follows a layered architecture:

Controller → Service → Repository → Database

- Controllers handle HTTP transport only.
- Services contain business logic and orchestration.
- Repositories are pure CRUD (Spring Data JPA).
- Entities encapsulate creation logic via static factory methods.
- DTOs define API boundaries.

No entity is exposed directly via controllers.

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

- Caching is applied at the service layer.
- Read operations use `@Cacheable`.
- Write operations invalidate cache via `@CacheEvict`.
- In-memory cache is used for now (can be replaced by Redis later).

---

## Testing Strategy

- Integration tests use Testcontainers.
- API tests use Rest Assured.
- Flyway migrations run during test startup.
- Full stack is tested: Controller → Service → Repository → DB.
- No mocking repositories in integration tests.

---

## Non-Goals

- No microservices.
- No reactive stack.
- No premature distributed caching.
- No entity exposure in API layer.

This project prioritizes clarity, correctness, and real-world backend practices.
