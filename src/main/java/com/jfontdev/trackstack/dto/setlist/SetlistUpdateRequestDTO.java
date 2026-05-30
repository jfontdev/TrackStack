package com.jfontdev.trackstack.dto.setlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for fully updating an existing setlist (PUT semantics).
 * <p>
 * Replaces the name and description of the setlist. Does not modify slots
 * or status — use dedicated endpoints for those operations.
 *
 * @param name        the new display name (required)
 * @param description the new description
 */
public record SetlistUpdateRequestDTO(
        @NotBlank(message = "Setlist name must not be empty")
        @Size(max = 255, message = "Name must not exceed 255 characters") String name,
        @Size(max = 2000, message = "Description must not exceed 2000 characters") String description) {
}
