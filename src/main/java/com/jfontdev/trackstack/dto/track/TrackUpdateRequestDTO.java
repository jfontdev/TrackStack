package com.jfontdev.trackstack.dto.track;

import jakarta.validation.constraints.NotBlank;

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
 */
public record TrackUpdateRequestDTO(@NotBlank String title,
                                    @NotBlank String artist,
                                    Double bpm,
                                    String key,
                                    @NotBlank String duration) {
}
