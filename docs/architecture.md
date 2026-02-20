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

## Data Flow

Request DTO → Service → Entity Factory → Repository → Database  
Database → Repository → Service → Response DTO → Controller

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

---

## Database

- PostgreSQL.
- Schema managed only by Flyway.
- Hibernate set to validate schema.
- No `ddl-auto=update` in any environment.
