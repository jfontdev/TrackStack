# Conventions

## Naming

- Service interfaces must be suffixed `Service` (e.g., `UserService`).
- Concrete service implementations must be suffixed `ServiceImpl` (e.g., `UserServiceImpl`) and may live under
  `service/impl`.
- Controllers end with `Controller`.
- Controllers must return `ResponseEntity<T>` explicitly (not raw objects). This keeps status codes and headers under explicit control.
- DTOs use `RequestDTO` and `ResponseDTO`.
- Repositories extend `JpaRepository` and are suffixed `Repository` (e.g., `TrackRepository`).

---

## Optional Handling

Always handle Optional explicitly with guard clauses:

```java
Optional<Entity> entity = repository.findById(id);

if (entity.isEmpty()) {
    throw new NotFoundException("Entity not found.");
}

return entity.get();
```

Do NOT use chained `.orElseThrow()` for readability reasons.

Prefer early returns for invalid or missing data.

---

## Exception Handling

- Throw `NotFoundException` (or a domain-specific subclass) when an entity lookup by ID fails.
- Use `GlobalExceptionHandler` (@ControllerAdvice) to map exceptions to uniform HTTP error responses.
- Keep exception messages user-friendly but not overly verbose.
- Do not leak internal stack traces or database details in production error responses.

---

## Entity Creation

Entities must use static factory methods:

```java
public static Track create(...) {
    return new Track(...);
}
```

Controllers must never instantiate entities directly.

---

## DTO Validation

- Use Jakarta Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`) on DTO request fields.
- Controllers must annotate request DTO parameters with `@Valid`.
- Validation failures are handled by `GlobalExceptionHandler` and return HTTP 400 with a clear message.

---

## Transactions

- Annotate service write methods with `@Transactional` (class-level `@Transactional(readOnly = true)` is acceptable for read-heavy services).
- Keep transactions at the service layer, not in controllers or repositories.
- Avoid calling `@Transactional` methods from within the same class (self-invocation bypasses Spring proxying).

---

## Caching

- Use `@Cacheable` on read methods.
- Use `@CacheEvict` on write methods.
- Cache names must match entity plural (e.g., "tracks").

---

## Logging

- No `System.out.println`.
- Use SLF4J (`org.slf4j.Logger`) via `LoggerFactory.getLogger(ClassName.class)`.
- Avoid noisy logs; log at `DEBUG` or `TRACE` for high-frequency operations.

---

## Formatting

- Constructor injection only.
- No field injection.
- No Lombok (unless explicitly added later).

---

## Documentation

- **Full JavaDoc is mandatory** on all Classes, Methods, Records, and Interfaces.
- Since this is a learning project, documentation must be highly human-readable.
- Explain the *why* behind decisions, not just the *what*.
- Include `@param`, `@return`, and `@throws` tags where applicable.

---

## Mapping

- Keep entity <-> DTO mapping in the service layer.
- Do not use separate mapper classes or helpers; perform manual mapping inside service methods.

---

## Testing

- Integration tests extend `BaseIntegrationTest`.
- Use real PostgreSQL via Testcontainers; do not mock repositories in integration tests.
- AI-specific tests must be conditional on `@EnabledIf("ai.enabled")`.
- Use RestAssured for HTTP-level assertions.
- Name test methods descriptively: `shouldCreateSetlist_whenValidRequestProvided()`.
