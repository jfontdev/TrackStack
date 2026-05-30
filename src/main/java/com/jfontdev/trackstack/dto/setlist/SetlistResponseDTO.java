package com.jfontdev.trackstack.dto.setlist;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a setlist returned by the API.
 * <p>
 * Includes the setlist metadata, lifecycle status, and its ordered slots.
 * This is the single representation used in all API responses.
 *
 * @param id                     the setlist's unique identifier
 * @param name                   the display name
 * @param description            free-text description
 * @param status                 lifecycle status (DRAFT, READY, PERFORMED)
 * @param createdDate            when the setlist was created
 * @param updatedDate            when the setlist was last updated
 * @param performedDate          when the setlist was performed
 * @param totalDurationSeconds   summed duration of all tracks
 * @param preparationTimeMinutes time spent building the setlist
 * @param slots                  ordered list of slots
 */
public record SetlistResponseDTO(
        Long id,
        String name,
        String description,
        String status,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        LocalDateTime performedDate,
        Integer totalDurationSeconds,
        Integer preparationTimeMinutes,
        List<SetlistSlotResponseDTO> slots) {
}
