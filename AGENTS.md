# TrackStack — AI Agent Guide

> **Welcome, AI agent.** This is your primary entry point.
> Read this file first, then follow the cascade below.

## 🎯 Mission

Build a portfolio-grade Spring Boot backend for DJ set planning and performance intelligence.
- Enterprise-style architecture, not a tutorial project.
- Every decision must be intentional and documented.
- No shortcuts, no framework magic.

## 📚 Document Cascade (Read in Order)

| Priority | File | Purpose | When to Read |
|----------|------|---------|--------------|
| **1** | `AGENTS.md` (this file) | Primary entry point, critical rules | **Always first** |
| **2** | `docs/ai-context.md` | AI coding manifesto: stack, architecture, style | Before writing any code |
| **3** | `docs/project-plan.md` | Roadmap, phases, current status | Before planning features |
| **4** | `docs/architecture.md` | Deep architecture decisions | When designing new components |
| **5** | `docs/conventions.md` | Naming conventions, package structure | When creating new files |
| **6** | `README.md` | Human-readable setup & overview | When you need build/run commands |

## ⚠️ Critical Rules (Always Apply)

### 1. Incremental Development (MANDATORY)
All feature implementations must be split into **small, reviewable increments**.
- Each increment: ~200 lines of changed code max
- Each increment must be tested before proceeding
- Propose the plan, get approval, then code
- Update `docs/project-plan.md` after each increment

**Full details in `docs/ai-context.md` § Incremental Development Rule**

### 2. Layered Architecture (MANDATORY)
```
Controller → Service → Repository
```
- No entity exposure in controllers (DTOs only)
- Repositories remain pure CRUD
- Services contain business logic + DTO mapping
- No dedicated mapper classes — manual mapping in services

### 3. Technology Stack (LOCKED)
| Layer | Technology |
|-------|------------|
| Language | Java 25 |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL |
| Migrations | Flyway only (no `ddl-auto`) |
| Cache | Redis with RedisJSON module |
| AI | Spring AI + Ollama (local LLM) |
| Testing | Testcontainers (PostgreSQL + Redis), Rest Assured |
| Audio | JAudioTagger |

### 4. Code Style (MANDATORY)
- Constructor injection only
- Explicit `Optional` handling with guard clauses
- No functional chaining (`.map().filter()` pipelines)
- Static factory methods for entity creation
- `Optional.isEmpty()` / `Optional.get()` pattern (no `orElseThrow`)
- No `@Data` / Lombok — explicit getters/setters

### 5. AI Features (GRACEFUL DEGRADATION)
- AI is enhancement, **not required** for core functionality
- All AI endpoints must have rule-based fallback
- `@ConditionalOnProperty("ai.enabled")` for toggling
- Core API works 100% without AI

## 🧪 Testing Requirements

- Integration tests for all new endpoints (Rest Assured + Testcontainers)
- Unit tests for pure logic components (no Spring context)
- No mocking repositories in integration tests
- Real PostgreSQL + Redis via Testcontainers
- Schema validated via Flyway
- AI tests: mock Ollama responses, test fallback logic

## 🚀 Getting Started Checklist

Before writing any code:
1. [ ] Read `docs/ai-context.md` (5 min)
2. [ ] Read `docs/project-plan.md` — current phase (3 min)
3. [ ] Check if a similar endpoint exists (read existing controller/service)
4. [ ] Propose increment plan to user
5. [ ] Wait for approval before coding

## 🗂️ Project Structure

```
trackstack/
├── AGENTS.md                 ← You are here
├── docs/
│   ├── ai-context.md           ← AI coding rules
│   ├── project-plan.md         ← Roadmap & phases
│   ├── architecture.md         ← Deep architecture
│   └── conventions.md          ← Naming & structure
├── src/
│   ├── main/
│   │   └── java/com/jfontdev/trackstack/
│   │       ├── controller/     ← REST endpoints (DTOs in/out)
│   │       ├── service/         ← Business logic + interfaces
│   │       ├── service/impl/    ← Service implementations
│   │       ├── repository/      ← Pure CRUD JPA
│   │       ├── model/           ← JPA entities (factory methods)
│   │       ├── dto/             ← Request/Response DTOs
│   │       ├── config/          ← Spring config, serializers
│   │       └── exception/       ← Custom exceptions
│   └── test/
│       └── java/com/jfontdev/trackstack/
│           ├── *IntegrationTest.java  ← Full-stack tests
│           └── service/        ← Unit tests
└── src/main/resources/
    ├── db/migration/           ← Flyway migrations
    └── application*.yml        ← Config per environment
```

## 📝 When to Update Documentation

| When you change... | Update this file |
|-------------------|------------------|
| Technology stack | `docs/ai-context.md` |
| Phase status / roadmap | `docs/project-plan.md` |
| Architecture pattern | `docs/architecture.md` |
| Naming convention | `docs/conventions.md` |
| AI agent rules | `AGENTS.md` + `docs/ai-context.md` |

## 🛑 Non-Negotiables

- No microservices
- No reactive stack
- No front-end (API-only)
- No Hibernate `ddl-auto`
- No code generation tools
- No vector database (RAG-lite via prompt context)
- No premature abstraction

## 💡 AI-Specific Tips

- **Rest Assured**: Use `given().when().then()` pattern for integration tests
- **Testcontainers**: PostgreSQL + Redis containers auto-wire via `@ServiceConnection`
- **RedisJSON**: Use `RedisJSONSerializer` for clean JSON (no `@class` hack)
- **Camelot Wheel**: Use `TransitionCompatibilityEngine` for key/BPM calculations
- **Flyway**: All schema changes go in `db/migration/V{N}__description.sql`

## 🤖 Current Phase (Auto-Updated)

> **Phase 04 – Performance Journal**
> 
> - Phase 03.5 COMPLETE: All 5 increments done.
> - Next: Session recording, planned vs actual tracking, performance stats.

**Next immediate action:** Implement Phase 04 — session recording and performance journal.

---

*This file is maintained for AI agents. Humans should read `README.md`.*
