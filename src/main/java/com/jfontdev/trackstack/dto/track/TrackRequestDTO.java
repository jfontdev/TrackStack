package com.jfontdev.trackstack.dto.track;

import jakarta.validation.constraints.NotBlank;

public record TrackRequestDTO(@NotBlank String title,
                              @NotBlank String artist,
                              Double bpm,
                              String key,
                              @NotBlank String duration) {
}
