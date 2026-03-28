package com.jfontdev.trackstack.dto.track;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for partially updating (PATCH) an existing track.
 * <p>
 * All fields are nullable because PATCH semantics allow updating only a subset
 * of fields. The service layer merges non-null values from this DTO with the
 * existing entity state before persisting. Nullable-friendly validations ensure
 * that if a field IS provided, it meets domain invariants (no empty strings,
 * no negative BPM, correct duration format).
 *
 * @param title    the new track title, or null to keep the current value
 * @param artist   the new track artist, or null to keep the current value
 * @param bpm      the new beats per minute, or null to keep the current value
 * @param key      the new musical key, or null to keep the current value
 * @param duration the new track duration, or null to keep the current value
 */
public record TrackPatchRequestDTO(@Size(min = 1, message = "Title must not be empty if provided") String title,
        @Size(min = 1, message = "Artist must not be empty if provided") String artist,
        @Positive(message = "BPM must be positive if provided") Double bpm,
        String key,
        @Pattern(regexp = "^\\d+:\\d{2}$", message = "Duration must be in mm:ss format if provided") String duration) {
}
