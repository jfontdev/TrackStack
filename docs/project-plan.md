# TrackStack – Project Plan

## 🎯 Project Purpose

TrackStack is an enterprise-style Spring Boot backend built to:

- Master Spring Boot 3.x deeply
- Practice clean layered architecture
- Implement real-world backend patterns
- Learn caching, transactions, and integration testing
- Build a portfolio-grade backend repository

This is not a tutorial project. It is structured intentionally to simulate real backend engineering.

---

# 🏗️ Architectural Principles

- Layered architecture (Controller → Service → Repository)
- Entities use static factory methods
- Repositories remain pure CRUD
- Explicit Optional handling (no chained functional style)
- DTOs define API boundaries
- No entity exposure in controllers
- Flyway-only schema management
- Constructor injection only
- Caching at service layer only
- No microservices
- No reactive stack
- No Hibernate auto schema generation

---

# 📦 Project Phases

---

## ✅ Phase 01 – Project Setup

**Goal:** Establish clean baseline.

- Spring Boot 3.x
- PostgreSQL (local)
- Flyway migrations
- Swagger / OpenAPI
- Proper application profiles
- No `ddl-auto=update`
- GitHub repo initialized

Status: ✅ Completed

---

## ✅ Phase 02 – Domain & Persistence

**Goal:** Model the domain correctly.

- Track entity
- Tag entity
- Playlist entity
- Many-to-many relationships
- Join tables
- Flyway migrations
- Pure `JpaRepository` usage

Status: ✅ Completed

---

## ✅ Phase 03 – Application Layer & REST API

**Goal:** Implement clean service & controller layers.

- Request / Response DTOs
- Explicit Optional handling
- Service interfaces + implementations
- Controllers
- Global exception handling
- No mappers (manual mapping inside services)

Status: ✅ Completed

---

## 🚀 Phase 04 – Caching (Current Phase)

**Goal:** Introduce controlled performance optimization.

- Enable Spring caching
- Apply `@Cacheable` to read operations
- Use `@CacheEvict` on write operations
- In-memory cache (default Spring CacheManager)
- Cache only `getById` methods initially
- Document caching strategy in README

Deliverables:

- Clean caching annotations
- No caching in controllers or repositories
- No premature Redis integration

---

## 🔜 Phase 05 – Integration Testing

**Goal:** Test full stack behavior.

- Testcontainers with PostgreSQL
- Flyway migrations executed during tests
- BaseIntegrationTest class
- Controller → Service → Repository → DB coverage
- Database cleanup strategy between tests

Deliverables:

- TrackControllerIntegrationTest
- TagControllerIntegrationTest
- PlaylistControllerIntegrationTest

---

## 🔜 Phase 06 – Update & Delete Operations

**Goal:** Complete CRUD cycle properly.

- PUT endpoints
- DELETE endpoints
- Cache invalidation on writes
- Proper transaction boundaries
- Validation rules

---

## 🔜 Phase 07 – Pagination & Filtering

**Goal:** Production-ready API patterns.

- Pageable endpoints
- Sorting
- Filter by BPM / Key / Genre
- Repository query methods
- Possibly Specifications

---

## 🔜 Phase 08 – Advanced Caching Strategy

**Goal:** Prepare for scalability.

- Introduce Caffeine or Redis
- Add TTL policies
- Cache invalidation strategy review
- Analyze read-heavy endpoints

---

## 🔜 Phase 09 – Observability

**Goal:** Production readiness.

- Actuator endpoints
- Health checks
- Metrics
- Logging strategy

---

## 🔜 Phase 10 – Security Layer

**Goal:** Enterprise-level API security.

- Spring Security
- JWT authentication
- Role-based access
- Endpoint protection

---

# 🧪 Testing Strategy

- Unit tests for service logic where needed
- Integration tests for full stack
- No mocking repositories in integration tests
- Real PostgreSQL via Testcontainers
- Schema validated via Flyway

---

# 📊 Non-Goals

- No microservices
- No reactive programming
- No distributed cache (yet)
- No front-end
- No premature abstraction
- No code generation tools

---

# 🧠 Learning Objectives

By completing this roadmap, the project will demonstrate:

- Deep Spring Boot understanding
- Proper layering discipline
- Database migration management
- Cache strategy design
- Integration testing mastery
- Production-ready backend architecture

---

# 🏁 Success Criteria

The project will be considered “enterprise-ready” when:

- Full CRUD operations exist
- Caching strategy is documented and tested
- Integration tests cover main flows
- Clear architectural documentation exists
- No accidental framework magic
- All decisions are intentional and documented

---

# 📌 Current Phase

👉 Phase 04 – Caching
