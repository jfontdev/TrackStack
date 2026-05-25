package com.jfontdev.trackstack.dto.track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for fully updating (PUT) an existing track.
 * <p>
 * All fields must be provided because PUT semantics replace the
 * entire resource.
 *
 * @param title           the track title (required)
 * @param artist          the track artist (optional)
 * @param album           the track album (optional)
 * @param bpm             the beats per minute (optional)
 * @param key             the musical key (optional)
 * @param durationSeconds the track duration in seconds (optional)
 * @param genre           the track genre (optional)
 * @param filePath        the file path (required)
 * @param fileFormat      the file format (optional)
 * @param bitrate         the bitrate in kbps (optional)
 * @param energy          the energy level 1-5 (optional)
 */
public record TrackUpdateRequestDTO(
        @NotBlank(message = "Title must not be empty") String title,
        String artist,
        String album,
        @Positive(message = "BPM must be positive if provided") Double bpm,
        String key,
        @Positive(message = "Duration must be positive if provided") Integer durationSeconds,
        @Size(min = 1, message = "Genre must not be empty if provided") String genre,
        @NotBlank(message = "File path must not be empty") String filePath,
        String fileFormat,
        @Positive(message = "Bitrate must be positive if provided") Integer bitrate,
        Integer energy) {
}
