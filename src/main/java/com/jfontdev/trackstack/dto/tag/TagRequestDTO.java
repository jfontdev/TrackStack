package com.jfontdev.trackstack.dto.tag;

import jakarta.validation.constraints.NotBlank;

public record TagRequestDTO(
        @NotBlank String name
) {
}