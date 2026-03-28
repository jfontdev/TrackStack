package com.jfontdev.trackstack.dto.tag;

/**
 * Request DTO for partially updating (PATCH) an existing tag.
 * <p>
 * All fields are nullable because PATCH semantics allow updating only a subset
 * of fields. The service layer merges non-null values with the existing entity
 * state before persisting.
 *
 * @param name the new tag name, or null to keep the current value
 */
public record TagPatchRequestDTO(
        String name
) {
}
