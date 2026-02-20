# Conventions

## Naming

- Services end with `Service` and `ServiceImpl`.
- Controllers end with `Controller`.
- DTOs use `RequestDTO` and `ResponseDTO`.
- Repositories extend `JpaRepository`.

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

## Entity Creation

Entities must use static factory methods:

```java
public static Track create(...) {
    return new Track(...);
}
```

Controllers must never instantiate entities directly.

---

## Caching

- Use `@Cacheable` on read methods.
- Use `@CacheEvict` on write methods.
- Cache names must match entity plural (e.g., "tracks").

---

## Logging

- No `System.out.println`.
- Use `Logger`.
- Avoid noisy logs.

---

## Formatting

- Constructor injection only.
- No field injection.
- No Lombok (unless explicitly added later).

---

## Mapping

- Keep entity <-> DTO mapping in the service layer.
- Use small private mapper methods for readability.
