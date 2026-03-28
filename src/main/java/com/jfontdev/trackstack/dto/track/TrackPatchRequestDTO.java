package com.jfontdev.trackstack.dto.track;

/**
 * Request DTO for partially updating (PATCH) an existing track.
 * <p>
 * All fields are nullable because PATCH semantics allow updating only a subset
 * of fields. The service layer merges non-null values from this DTO with the
 * existing entity state before persisting.
 *
 * @param title    the new track title, or null to keep the current value
 * @param artist   the new track artist, or null to keep the current value
 * @param bpm      the new beats per minute, or null to keep the current value
 * @param key      the new musical key, or null to keep the current value
 * @param duration the new track duration, or null to keep the current value
 */
public record TrackPatchRequestDTO(String title,
                                   String artist,
                                   Double bpm,
                                   String key,
                                   String duration) {
}
