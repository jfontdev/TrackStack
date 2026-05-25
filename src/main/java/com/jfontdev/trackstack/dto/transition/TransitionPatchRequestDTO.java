package com.jfontdev.trackstack.dto.transition;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for partially updating (PATCH) an existing transition.
 * <p>
 * All fields are nullable because PATCH semantics allow updating only a
 * subset of fields. The service layer merges non-null values with the
 * existing entity state before persisting.
 *
 * @param rating the new quality rating 1-5, or null to keep current
 * @param notes  the new free-text observations, or null to keep current
 * @param style  the new transition technique, or null to keep current
 */
public record TransitionPatchRequestDTO(
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5") Integer rating,
        @Size(max = 2000, message = "Notes must not exceed 2000 characters") String notes,
        @Size(max = 50, message = "Style must not exceed 50 characters") String style) {
}
