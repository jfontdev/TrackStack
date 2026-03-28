package com.jfontdev.trackstack.dto.track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TrackRequestDTO(@NotBlank(message = "Title must not be empty") String title,
        @NotBlank(message = "Artist must not be empty") String artist,
        @Positive(message = "BPM must be positive if provided") Double bpm,
        String key,
        @NotBlank(message = "Duration must not be empty") @Pattern(regexp = "^\\d+:[0-5]\\d$", message = "Duration must be in mm:ss format") String duration) {
}
