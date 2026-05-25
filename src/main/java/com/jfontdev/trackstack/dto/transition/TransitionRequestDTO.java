package com.jfontdev.trackstack.dto.transition;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new transition.
 * <p>
 * Required fields enforce the minimum data needed to log a transition
 * experience. Optional fields allow capturing additional context.
 *
 * @param sourceTrackId the track being played first (required)
 * @param targetTrackId the track being transitioned into (required)
 * @param rating        user's quality rating 1-5 (required)
 * @param notes         free-text observations (optional)
 * @param style         transition technique used (optional)
 */
public record TransitionRequestDTO(
        @NotNull(message = "Source track ID is required") Long sourceTrackId,
        @NotNull(message = "Target track ID is required") Long targetTrackId,
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5") Integer rating,
        @Size(max = 2000, message = "Notes must not exceed 2000 characters") String notes,
        @Size(max = 50, message = "Style must not exceed 50 characters") String style) {
}
