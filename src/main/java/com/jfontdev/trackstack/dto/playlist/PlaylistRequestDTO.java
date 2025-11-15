package com.jfontdev.trackstack.dto.playlist;

import jakarta.validation.constraints.NotBlank;

public record PlaylistRequestDTO(
        @NotBlank String name,
        String description
) {
}