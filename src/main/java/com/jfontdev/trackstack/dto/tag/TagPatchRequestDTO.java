package com.jfontdev.trackstack.dto.tag;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for partially updating (PATCH) an existing tag.
 * <p>
 * All fields are nullable because PATCH semantics allow updating only a subset
 * of fields. The service layer merges non-null values with the existing entity
 * state before persisting. Nullable-friendly validations ensure that if a field
 * IS provided, it meets domain invariants (no empty strings).
 *
 * @param name the new tag name, or null to keep the current value
 */
public record TagPatchRequestDTO(
        @Size(min = 1, message = "Name must not be empty if provided") String name
) {
}
