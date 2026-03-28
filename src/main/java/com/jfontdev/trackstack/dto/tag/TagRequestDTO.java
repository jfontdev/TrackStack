package com.jfontdev.trackstack.dto.tag;

import jakarta.validation.constraints.NotBlank;

public record TagRequestDTO(
                @NotBlank(message = "Name must not be empty") String name) {
}