package com.jfontdev.trackstack.dto.playlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistRequestDTO(
                @NotBlank(message = "Name must not be empty") String name,
                @Size(max = 500, message = "Description must not exceed 500 characters") String description) {
}