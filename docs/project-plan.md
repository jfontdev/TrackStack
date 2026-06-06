# TrackStack – Project Plan

## 🎯 Project Purpose

TrackStack is an enterprise-style Spring Boot backend built to:

- Master Spring Boot 3.x deeply
- Practice clean layered architecture
- Implement real-world backend patterns
- Learn caching, transactions, and integration testing
- Build a portfolio-grade backend repository
- **NEW:** Serve as a personal DJ Set Planner & Performance Intelligence Platform

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
- **NEW:** AI as enhancement, not core dependency
- **NEW:** Graceful degradation (AI down → rule-based fallback)

> **See `docs/ai-context.md` for the Incremental Development Rule** — all AI-generated code must follow it.

---

# 📦 Project Phases

---

## ✅ Phase 00 – Project Reset & Foundation

**Goal:** Clean slate from generic music API to DJ-focused domain.

- Spring Boot 3.x
- PostgreSQL (local)
- Flyway migrations (V1 drops old schema, creates DJ domain)
- Swagger / OpenAPI
- Proper application profiles
- No `ddl-auto=update`
- GitHub repo initialized
- **Decision:** Start fresh schema (drop old Tracks/Tags/Playlists tables)

Status: ✅ Completed (metadata, architecture docs)

---

## ✅ Phase 01 – Smart Track Library (Minimal)

**Goal:** Auto-populate from your real music files quickly, then move on.

- File Scanner Service: Scan `C:\Users\jordi\Documents\MEGA\Musica DJ`
- Read audio metadata via JAudioTagger (BPM, Key, Genre, Duration)
- Use folder structure as genre taxonomy
- Track entity with file path linking
- Duplicate detection
- Search & Filter: BPM range, Key, Genre, Energy
- **Decision:** Scan ALL files recursively; folder name = genre tag
- **Decision:** Energy is hybrid (manual override with future AI suggestions)

**Endpoints:**
- `POST /api/tracks/scan` - Trigger directory scan
- `GET /api/tracks?bpmFrom=130&bpmTo=140&key=11B&genre=Techno`
- `GET /api/tracks/{id}`

**Tech:** JAudioTagger library, Jackson for metadata parsing

**Duration:** 1-2 weeks (minimal viable, don't over-engineer)

**Status:** ✅ Completed. Merged in PR #1 (feat/00-Project-Reset).

---

## ✅ Phase 02 – Transition Graph (Priority) [COMPLETE]

**Goal:** Replace `mezclas.md` with a queryable, intelligent transition database.

**Decision:** This is the priority phase. Build fast after Phase 01.

**Core Features:**
- Log Transitions: `POST /api/transitions`
  - `{sourceTrackId, targetTrackId, rating, notes, style}`
  - Auto-calculates key compatibility, BPM diff
- Directed transitions: A→B is separate from B→A (different ratings)
- View transitions per track
- Transition stats: most reliable, your "signature" transitions
- Key/BPM Compatibility Engine: rule-based suggestions

**Endpoints:**
- `POST /api/transitions` (log a transition)
- `GET /api/transitions/from/{trackId}`
- `GET /api/transitions/to/{trackId}`
- `GET /api/transitions/best?trackId=123&limit=5`
- `PUT /api/transitions/{id}`
- `PATCH /api/transitions/{id}`
- `DELETE /api/transitions/{id}`
- `POST /api/transitions/{id}/record-play`

**Duration:** 2 weeks

**Status:** ✅ Complete. All CRUD endpoints working with Camelot wheel key compatibility.

---

## ✅ Phase 02.5 – AI Transition Suggestions (Priority) [COMPLETE]

**Goal:** Smart transition suggestions via Ollama (Gemma 4 26b MOE).

**Decision:** Priority feature. Implement alongside Phase 02.

**Setup:**
- `spring-ai-starter-model-ollama` (Spring AI 1.0.0)
- Local network endpoint: configurable (e.g., `http://amd-ai-hx390:11434`)
- Model: `gemma4:26b` (primary), fallback to rule-based

**Endpoint:**
- `POST /api/ai/transitions/suggest`
  - Input: `{trackId: 123, vibe: "maintain energy but add melody", excludeRecentlyPlayed: true}`
  - AI queries library context, suggests 3 tracks with reasoning
  - **Fallback:** If Ollama down, use rule-based key/BPM matching

**Prompt Strategy:**
- Fetch 50 relevant tracks from core service
- Include transition history in prompt context
- Request structured JSON response
- Parse and return DTO

**Tech:** Spring AI Ollama, `ChatModel` API, structured JSON parsing

**Status:** ✅ Complete. AI suggestions with automatic rule-based fallback.

**Duration:** 1-2 weeks (parallel with Phase 02)

---

## ✅ Phase 03 – Set Planning + RedisJSON Cache Refactor

**Goal:** Build setlists with intelligence AND migrate Redis caching to RedisJSON.

**Status:** COMPLETE. Both Part A and Part B done.

### Part A: Set Planning

- Setlist Builder: ordered slots with track sequence
- Setlist status: DRAFT → READY → PERFORMED
- Slot CRUD, reordering
- Preparation time tracking (entity field, logic in Phase 04)

**Endpoints (COMPLETED):**
- `POST /api/setlists` (create with slots)
- `GET /api/setlists` (all, filter by status)
- `GET /api/setlists/{id}`
- `PUT /api/setlists/{id}`
- `DELETE /api/setlists/{id}`
- `POST /api/setlists/{id}/slots`
- `PUT /api/setlists/{id}/slots/{slotId}`
- `DELETE /api/setlists/{id}/slots/{slotId}`
- `PUT /api/setlists/{id}/slots/reorder`
- `POST /api/setlists/{id}/ready`
- `POST /api/setlists/{id}/performed`

**Phase 03.5 endpoints (now COMPLETE):**
- `GET /api/setlists/{id}/energy-arc` — energy progression visualization
- `GET /api/setlists/{id}/validate-transitions` — track-to-track transition validation
- `POST /api/setlists/{id}/export` — text/JSON export for XDJ-AZ

### Part B: RedisJSON Cache Refactor (COMPLETE)

**Goal:** Replace GenericJackson2JsonRedisSerializer with native RedisJSON.

**Completed:**
- ✅ Switched Docker Compose to `redis/redis-stack:latest`
- ✅ Switched Testcontainers to `redis/redis-stack:latest`
- ✅ Created `RedisJSONSerializer` — stores clean JSON with lightweight `_type` envelope (no `@class` hack)
- ✅ Updated `CacheConfig` — removed `PolymorphicTypeValidator` complexity
- ✅ Created `RedisJSONService` with client-side JSONPath query support (e.g., `$.bpm > 130`)
- ✅ Integration tests verify caching and JSONPath queries work correctly

**Tech:** RedisJSON module, `redis/redis-stack` Docker image, Jackson `JsonNode`

**AI Enhancement (future Phase 03.5+):**
- `POST /api/ai/setlists/analyze` - AI critiques energy arc and flow
- `POST /api/ai/setlists/generate` - AI generates setlist from natural language

**Duration:** 2-3 weeks (Phase 03 + 03.5 combined)

---

## ✅ Phase 03.5 – Set Planning Intelligence (Incrementally)

**Goal:** Implement the three features that were moved from Phase 03, but do it incrementally with reviewable chunks.

**Status:** COMPLETE. All 5 increments done.

### Increment 1: Foundation — Extract Shared Compatibility Engine ✅ COMPLETE
- Extract `calculateKeyCompatibility` and `calculateBpmDifference` from `TransitionServiceImpl` into a new `TransitionCompatibilityEngine` component
- Update `TransitionServiceImpl` to delegate to the new engine
- Add unit tests for `TransitionCompatibilityEngine` in isolation
- **Why first:** This is shared infrastructure needed by both feature 2 and existing AI/rule-based fallback
- **Review focus:** Clean extraction, no behavior change, tests still pass
- **Status:** ✅ Done. 14 unit tests passing. Main code compiles.

### Increment 2: Feature A — Energy Arc Visualization ✅ COMPLETE
- DTO: `SetlistEnergyArcDTO` with `points` and `stats`
- Service: `getEnergyArc(Long setlistId)` in `SetlistService`
- Controller: `GET /api/setlists/{id}/energy-arc`
- Integration test: verify ordered points, stats (avg, peak, trend)
- **Review focus:** DTO design, stat calculation accuracy, REST semantics
- **Status:** ✅ Done. DTO, service, controller, and tests implemented. 50 main source files compile.

### Increment 3: Feature B — Track-to-Track Transition Validation ✅ COMPLETE
- DTO: `SetlistTransitionValidationDTO` with per-pair key/BPM/transition status
- Service: `validateTransitions(Long setlistId)` in `SetlistService`
- Reuses `TransitionCompatibilityEngine` from Increment 1
- Queries `TransitionRepository` to check if logged transitions exist
- Controller: `GET /api/setlists/{id}/validate-transitions`
- Integration test: verify key compatibility warnings, BPM jump warnings, missing transition warnings
- **Review focus:** Reuse of shared engine, clean DTO composition, warning accuracy
- **Status:** ✅ Done. 51 main source files compile.

### Increment 4: Feature C — Setlist Export for XDJ-AZ ✅ COMPLETE
- DTO: `SetlistExportDTO` for JSON variant
- Service: `exportSetlist(Long setlistId, String format)` in `SetlistService`
- Controller: `POST /api/setlists/{id}/export?format={json|text}`
- Text format: plain text playlist with numbered tracks, BPM/key, energy
- JSON format: structured metadata for downstream tools
- Integration test: verify both JSON and text outputs
- **Review focus:** Export format design, content-type handling, text format readability
- **Status:** ✅ Done. 52 main source files compile.

### Increment 5: Documentation & Project Plan Update ✅ COMPLETE
- Update this `project-plan.md` to mark Phase 03.5 as complete
- Update `Current Phase` section to reflect Phase 03.5 is done
- Move the three features from "MOVED to Phase 03.5 / 04" to "COMPLETED" under Phase 03.5
- **Review focus:** Documentation accuracy, no drift from implementation
- **Status:** ✅ Done. All docs updated.

---

## 🔜 Phase 04 – Performance Journal

**Goal:** Track what actually happened vs. planned.

- Session recording: practice or Mixcloud upload
- Link to setlist (optional), add Mixcloud URL
- Record actual tracks played vs. planned
- Session ratings & notes
- Performance stats by genre

**Endpoints:**
- `POST /api/sessions`
- `PUT /api/sessions/{id}/played-tracks`
- `GET /api/sessions/{id}/comparison` (planned vs actual)
- `GET /api/sessions/stats`

**AI Enhancement (Phase 04.5):**
- `POST /api/ai/sessions/insights` - AI analyzes session history patterns

**Duration:** 1-2 weeks

---

## 🔜 Phase 05 – Analytics & Intelligence

**Goal:** Insights your markdown could never provide.

- Play frequency report by genre/time
- Forgotten gems (not played in X months, fit current trends)
- Genre evolution over time
- Transition success rate by genre pair
- Preparation time tracking by genre
- Key distribution analysis

**Endpoints:**
- `GET /api/analytics/play-frequency?genre=Techno&months=6`
- `GET /api/analytics/forgotten-gems?months=6&limit=10`
- `GET /api/analytics/genre-trends`
- `GET /api/analytics/transition-success`

**AI Enhancement (Phase 05.5):**
- `POST /api/ai/analytics/narrative` - AI-generated narrative report

**Duration:** 2 weeks

---

## 🔜 Phase 06 – Integrations

**Goal:** Connect to external tools.

- Rekordbox XML playlist import
- Mixcloud URL linking
- File watchers for `Musica DJ` folder (auto-add new tracks)

**Future (not initial plan):**
- Real-time Rekordbox sync (if encryption bypassed)
- Spotify/SoundCloud integration

**Duration:** 1-2 weeks

---

# 🧪 Testing Strategy

- Unit tests for service logic where needed
- Integration tests for full stack
- No mocking repositories in integration tests
- Real PostgreSQL via Testcontainers
- Schema validated via Flyway
- **NEW:** AI component tests use mocked Ollama responses
- **NEW:** Test AI fallback logic (rule-based when Ollama unavailable)
- **NEW:** `@EnabledIf("ai.enabled")` for AI-specific tests

---

# 📊 Non-Goals

- No microservices
- No reactive programming
- No distributed cache (yet)
- No front-end
- No premature abstraction
- No code generation tools
- **NEW:** AI is not required for core functionality to work
- **NEW:** No vector database (RAG-lite via prompt context)

---

# 🧠 Learning Objectives

By completing this roadmap, the project will demonstrate:

- Deep Spring Boot understanding
- Proper layering discipline
- Database migration management
- Cache strategy design
- Integration testing mastery
- **NEW:** Audio file processing (JAudioTagger)
- **NEW:** Local AI integration (Spring AI + Ollama)
- **NEW:** Prompt engineering for structured outputs
- **NEW:** Graceful degradation and fallback strategies
- Production-ready backend architecture

---

# 🏁 Success Criteria

The project will be considered "enterprise-ready" when:

- Full CRUD operations exist
- Caching strategy is documented and tested
- Integration tests cover main flows
- Clear architectural documentation exists
- No accidental framework magic
- All decisions are intentional and documented
- **NEW:** AI features enhance but don't block core functionality
- **NEW:** Track library auto-scanned from real files
- **NEW:** Transition suggestions work with and without AI

---

# 📌 Current Phase

👉 Phase 04 – Performance Journal

**Phase 03 COMPLETED:**
- Part A: Set Planning — setlists, slots, CRUD, reordering, lifecycle (14 tests)
- Part B: RedisJSON Cache Refactor — redis/redis-stack, clean JSON serializer, JSONPath queries (5 tests)
- Total: 41 tests, all passing

**Phase 03.5 COMPLETED:**
- ✅ Increment 1: Extract `TransitionCompatibilityEngine` (shared foundation)
- ✅ Increment 2: `GET /api/setlists/{id}/energy-arc` — energy arc visualization
- ✅ Increment 3: `GET /api/setlists/{id}/validate-transitions` — transition validation
- ✅ Increment 4: `POST /api/setlists/{id}/export` — setlist export
- ✅ Increment 5: Documentation & plan update

**Next immediate action:** Implement Phase 04 — session recording, planned vs actual tracking, performance stats.

---

# 🤖 AI Integration Summary

  ## Hardware
- AMD AI HX390 MiniPC (separate machine on local network)
- Models: Gemma 4 26b MOE (primary), Gemma 4 e4b (fallback)

## Architecture
- AI endpoints call core services for library context
- No AI-only data paths
- `@ConditionalOnProperty("ai.enabled")` for toggling
- Ollama URL configurable per environment (`application-local.yml` for your setup)

## Fallback Strategy
- If Ollama unavailable: AI endpoints use rule-based logic
- Core API works 100% without AI

## Endpoints
- `POST /api/ai/transitions/suggest` - Smart transition suggestions
- `POST /api/ai/setlists/analyze` - Setlist critique
- `POST /api/ai/setlists/generate` - Natural language setlist generation
- `POST /api/ai/sessions/insights` - Session pattern analysis
- `POST /api/ai/analytics/narrative` - Narrative analytics report

## Tech Stack Addition
- `spring-ai-ollama-spring-boot-starter`
- Ollama running on local network (configurable URL, e.g., `http://amd-ai-hx390:11434`)
- `@Async` for background AI processing
- `ollama.base-url` in `application.yml` for environment-specific configuration

---

# 🎵 Domain Decisions Summary

| Decision | Choice |
|----------|--------|
| Schema | Start fresh (drop old, create new) |
| File scanning | Recursive all folders; folder name = genre |
| Transitions | Directed (A→B separate from B→A) |
| Energy | Hybrid: manual with AI suggestions |
| AI fallback | Rule-based when Ollama down |
| Priority | Phase 02/02.5 first, fill Phase 01 minimally |
