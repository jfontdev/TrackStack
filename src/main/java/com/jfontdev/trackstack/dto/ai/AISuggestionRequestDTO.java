package com.jfontdev.trackstack.dto.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for AI-powered transition suggestions.
 * <p>
 * Provides context to the AI model about the current track and the desired
 * vibe for the next track in the set.
 *
 * @param trackId             the current track being played (required)
 * @param vibe                optional description of desired mood/energy shift
 * @param excludeRecentlyPlayed whether to exclude tracks played in last 2 weeks
 * @param limit               maximum number of suggestions (default 3)
 */
public record AISuggestionRequestDTO(
        @NotNull(message = "Track ID is required") Long trackId,
        @Size(max = 500, message = "Vibe description must not exceed 500 characters") String vibe,
        Boolean excludeRecentlyPlayed,
        Integer limit) {
}
