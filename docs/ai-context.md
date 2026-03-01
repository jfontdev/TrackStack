# AI Context Anchor

This file exists to guide AI code generation tools (Copilot, etc.).

## Project Goals

- Build an enterprise-style Spring Boot backend.
- Learn proper layered architecture.
- Practice caching, migrations, and integration testing.
- Avoid shortcuts and framework magic.

## Stack Decisions

- Java 25
- Spring Boot 3.x
- JPA + PostgreSQL
- Flyway migrations only
- In-memory caching (for now)
- Testcontainers for integration testing
- Rest Assured for integration testing

## Architecture Rules

- Controller → Service → Repository.
- No entity exposure in controllers.
- Repositories remain pure CRUD.
- Services contain business logic.
- Optional handled explicitly with guard clauses.
- Entities use factory methods.
- Avoid dedicated mapper classes; do manual Entity↔DTO mapping in services.

## Non-Goals

- No microservices.
- No reactive stack.
- No Hibernate auto schema generation.
- No magic frameworks.

## Code Style Rules

- Explicit Optional handling with guard clauses.
- Constructor injection only.
- No functional chaining for readability.
- Clear and minimal abstraction.
- No premature optimization.

If generating code, follow architecture.md and conventions.md.
