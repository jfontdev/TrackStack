package com.jfontdev.trackstack.dto.setlist;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for a single slot within a setlist.
 * <p>
 * Each slot references one track and contributes to the set's energy arc.
 *
 * @param trackId   the track placed in this slot (required)
 * @param slotOrder the position in the sequence (required)
 * @param energy    optional energy level 1-5
 * @param notes     optional free-text notes
 */
public record SetlistSlotRequestDTO(
        @NotNull(message = "Track ID is required") Long trackId,
        @NotNull(message = "Slot order is required") Integer slotOrder,
        @Min(value = 1, message = "Energy must be at least 1")
        @Max(value = 5, message = "Energy must be at most 5") Integer energy,
        @Size(max = 2000, message = "Notes must not exceed 2000 characters") String notes) {
}
