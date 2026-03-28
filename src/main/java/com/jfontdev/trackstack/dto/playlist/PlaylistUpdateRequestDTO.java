package com.jfontdev.trackstack.dto.playlist;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for fully updating (PUT) an existing playlist.
 * <p>
 * PUT semantics require all fields to be provided. The description field
 * is nullable on the entity, so omitting it (or sending null) clears the
 * description.
 *
 * @param name        the new playlist name (required)
 * @param description the new playlist description (optional)
 */
public record PlaylistUpdateRequestDTO(
        @NotBlank String name,
        String description
) {
}
