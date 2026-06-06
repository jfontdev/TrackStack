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

## Incremental Development Rule

> All feature implementations must be split into **small, reviewable increments**.
> Each increment is designed to be reviewed independently, approved, and then merged.
> This prevents large, hard-to-review PRs and ensures every architectural decision is intentional.

**How it works:**
1. A feature is broken into logical sub-phases (e.g., foundation → DTOs → service → controller → tests)
2. Each sub-phase is implemented, tested, and reviewed before the next one starts
3. The project plan is updated after each increment to reflect current status
4. No single increment should exceed ~200 lines of changed code
5. The AI must propose the increment plan and get user approval before coding

**This rule applies to ALL AI code generation.**

If generating code, follow architecture.md and conventions.md.
