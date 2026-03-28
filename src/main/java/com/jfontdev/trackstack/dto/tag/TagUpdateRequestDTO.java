package com.jfontdev.trackstack.dto.tag;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for fully updating (PUT) an existing tag.
 * <p>
 * PUT semantics require all fields to be provided. The tag name must not be
 * blank and must remain unique (enforced at the database level).
 *
 * @param name the new tag name (required, must be unique)
 */
public record TagUpdateRequestDTO(
                @NotBlank(message = "Name must not be empty") String name) {
}
