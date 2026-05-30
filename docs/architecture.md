# Architecture

## Layer Responsibilities

### Controller Layer

- Handles HTTP requests and responses.
- Performs input validation using `@Valid`.
- Returns DTOs only.
- Does not contain business logic.

### Service Layer

- Contains business rules.
- Orchestrates repository calls.
- Handles Optional explicitly with guard clauses.
- Performs mapping from Entity → DTO.
- Owns caching behavior.
- Orchestrates AI calls (local LLM via Spring AI + Ollama) with rule-based fallback.

### Repository Layer

- Extends `JpaRepository`.
- No custom business logic.
- No caching logic.
- Pure CRUD operations.

### Entity Layer

- JPA annotated.
- Contains static factory methods for creation.
- No dependency on controllers or DTOs.

---

### AI Orchestration Layer

- Uses Spring AI + Ollama for local LLM integration.
- AI calls are wrapped with fallback to rule-based logic when Ollama is unavailable.
- Core API functionality works 100% without AI.
- AI-specific endpoints are conditional on `ai.enabled` property.

## Data Flow

Request DTO → Service → Entity Factory → Repository → Database  
Database → Repository → Service → Response DTO → Controller

AI path: Service → Spring AI → Ollama (local LLM) → AI-enhanced response  
Fallback path: Service → Rule-based logic → Response DTO

---

## Error Handling

- `NotFoundException` for missing entities.
- Global exception handler maps exceptions to HTTP responses.
- Validation errors return 400.

---

## Caching Rules

- Only applied on service read methods.
- Keys based on method parameters.
- Writes must invalidate cache.
- List/item invalidation strategy will be defined as list endpoints grow.
- No caching at controller or repository level.
- Redis with RedisJSON module stores values as native JSON objects.
- Custom `RedisJSONSerializer` wraps values in a `{"_type": "...", "payload": {...}}` envelope.
- `RedisJSONService` provides client-side JSONPath-like queries on cached data.

---

## Database

- PostgreSQL.
- Schema managed only by Flyway.
- Hibernate set to validate schema.
- No `ddl-auto=update` in any environment.
- Current schema: tracks, transitions, setlists, setlist_slots (V10).
