# Copilot Instructions

These are the short rules Copilot must follow for this repo.
Canonical guidance lives in:

- [docs/ai-context.md](../docs/ai-context.md)
- [docs/architecture.md](../docs/architecture.md)
- [docs/conventions.md](../docs/conventions.md)

## Architecture and Flow

- Use Controller -> Service -> Repository layering.
- Controllers return DTOs only, no entities.
- Services hold business logic, mapping, and caching.
- Repositories are pure CRUD only.
- Entities use static factory methods for creation.

## Optional Handling

- Handle Optional explicitly with guard clauses and if/empty checks.
- Do not use chained orElseThrow for readability.

## Database and Migrations

- PostgreSQL with Flyway only.
- No Hibernate schema generation or ddl-auto update.

## Caching

- Cache only in service read methods.
- Evict cache on write methods.
- Cache names are plural entity names (e.g., "tracks").

## Code Style

- Constructor injection only, no field injection.
- No Lombok unless explicitly added later.
- Avoid functional chaining when it hurts readability.
- No System.out.println; use Logger.

## Documentation

- Full JavaDoc documentation is required on all Classes, Methods, Records, and Interfaces.
- The main point of this project is for learning, so documentation must be highly human-readable, explaining the "why" and "how" clearly.

## Testing

- Prefer integration tests with Testcontainers when touching persistence.
