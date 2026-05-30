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
- Redis with RedisJSON module
- Spring Cache + custom RedisJSON serializer
- JAudioTagger (audio metadata extraction)
- Spring AI + Ollama (local LLM)
- Testcontainers (PostgreSQL + Redis)
- Rest Assured for integration testing

## Architecture Rules

- Controller → Service → Repository.
- No entity exposure in controllers.
- Repositories remain pure CRUD.
- Services contain business logic, DTO mapping, and AI orchestration.
- Optional handled explicitly with guard clauses.
- Entities use factory methods.
- Avoid dedicated mapper classes; do manual Entity↔DTO mapping in services.
- Caching at service layer only, using Redis with RedisJSON.

## Non-Goals

- No microservices.
- No reactive stack.
- No front-end (API-only).
- No Hibernate auto schema generation.
- No magic frameworks.
- AI is enhancement, not required for core functionality.

## Code Style Rules

- Explicit Optional handling with guard clauses.
- Constructor injection only.
- No functional chaining for readability.
- Clear and minimal abstraction.
- No premature optimization.

If generating code, follow architecture.md and conventions.md.
