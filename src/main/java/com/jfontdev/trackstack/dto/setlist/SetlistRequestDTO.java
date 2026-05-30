package com.jfontdev.trackstack.dto.setlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a new setlist.
 * <p>
 * A setlist starts in DRAFT status and can optionally include initial slots.
 * Slots are ordered by their position in the list.
 *
 * @param name        the display name of the setlist (required)
 * @param description optional free-text description
 * @param slots       optional initial slots for the setlist
 */
public record SetlistRequestDTO(
        @NotBlank(message = "Setlist name must not be empty")
        @Size(max = 255, message = "Name must not exceed 255 characters") String name,
        @Size(max = 2000, message = "Description must not exceed 2000 characters") String description,
        List<SetlistSlotRequestDTO> slots) {
}
