package com.jfontdev.trackstack.dto.playlist;

/**
 * Request DTO for partially updating (PATCH) an existing playlist.
 * <p>
 * All fields are nullable because PATCH semantics allow updating only a subset
 * of fields. The service layer merges non-null values with the existing entity
 * state before persisting.
 *
 * @param name        the new playlist name, or null to keep the current value
 * @param description the new playlist description, or null to keep the current value
 */
public record PlaylistPatchRequestDTO(
        String name,
        String description
) {
}
