package com.jfontdev.trackstack.dto.playlist;

import com.jfontdev.trackstack.dto.track.TrackResponseDTO;

import java.util.List;

/**
 * Response DTO representing a playlist returned by the API.
 * <p>
 * Includes the playlist's metadata and its associated tracks. This DTO is the
 * single representation of a playlist in all API responses (create, update,
 * get by ID, list).
 *
 * @param id          the playlist's unique identifier
 * @param name        the playlist name
 * @param description the playlist description
 * @param tracks      the tracks belonging to this playlist
 */
public record PlaylistResponseDTO(
        Long id,
        String name,
        String description,
        List<TrackResponseDTO> tracks
) {
}
