package com.jfontdev.trackstack.dto.track;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for partially updating (PATCH) an existing track.
 * <p>
 * All fields are nullable because PATCH semantics allow updating only a subset
 * of fields. The service layer merges non-null values from this DTO with the
 * existing entity state before persisting.
 *
 * @param title           the new track title, or null to keep current
 * @param artist          the new track artist, or null to keep current
 * @param album           the new track album, or null to keep current
 * @param bpm             the new beats per minute, or null to keep current
 * @param key             the new musical key, or null to keep current
 * @param durationSeconds the new duration in seconds, or null to keep current
 * @param genre           the new track genre, or null to keep current
 * @param filePath        the new file path, or null to keep current
 * @param fileFormat      the new file format, or null to keep current
 * @param bitrate         the new bitrate, or null to keep current
 * @param energy          the new energy level, or null to keep current
 */
public record TrackPatchRequestDTO(
        @Size(min = 1, message = "Title must not be empty if provided") String title,
        @Size(min = 1, message = "Artist must not be empty if provided") String artist,
        @Size(min = 1, message = "Album must not be empty if provided") String album,
        @Positive(message = "BPM must be positive if provided") Double bpm,
        String key,
        @Positive(message = "Duration must be positive if provided") Integer durationSeconds,
        @Size(min = 1, message = "Genre must not be empty if provided") String genre,
        @Size(min = 1, message = "File path must not be empty if provided") String filePath,
        String fileFormat,
        @Positive(message = "Bitrate must be positive if provided") Integer bitrate,
        Integer energy) {
}
