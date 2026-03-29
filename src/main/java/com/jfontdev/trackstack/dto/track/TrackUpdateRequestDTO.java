package com.jfontdev.trackstack.dto.track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for fully updating (PUT) an existing track.
 * <p>
 * All required fields must be provided because PUT semantics replace the
 * entire resource. Fields that are nullable on the entity (bpm, key) remain
 * nullable here -- omitting them sets them to null on the entity.
 *
 * @param title    the track title (required)
 * @param artist   the track artist (required)
 * @param bpm      the beats per minute (optional)
 * @param key      the musical key (optional)
 * @param duration the track duration (required)
 * @param genre    the track genre (optional)
 */
public record TrackUpdateRequestDTO(@NotBlank(message = "Title must not be empty") String title,
                @NotBlank(message = "Artist must not be empty") String artist,
                @Positive(message = "BPM must be positive if provided") Double bpm,
                String key,
                @NotBlank(message = "Duration must not be empty") @Pattern(regexp = "^\\d+:[0-5]\\d$", message = "Duration must be in mm:ss format") String duration,
                @Size(min = 1, message = "Genre must not be empty if provided") String genre) {
}
