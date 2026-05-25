package com.jfontdev.trackstack.dto.transition;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for fully updating (PUT) an existing transition.
 * <p>
 * All mutable fields must be provided because PUT replaces the entire
 * resource. Auto-calculated fields (compatibleKeys, bpmDifference) are
 * not included — they are managed by the service layer.
 *
 * @param rating the new quality rating 1-5 (required)
 * @param notes  the new free-text observations (optional)
 * @param style  the new transition technique (optional)
 */
public record TransitionUpdateRequestDTO(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5") Integer rating,
        @Size(max = 2000, message = "Notes must not exceed 2000 characters") String notes,
        @Size(max = 50, message = "Style must not exceed 50 characters") String style) {
}
