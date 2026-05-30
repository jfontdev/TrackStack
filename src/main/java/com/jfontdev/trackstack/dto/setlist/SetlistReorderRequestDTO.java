package com.jfontdev.trackstack.dto.setlist;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for reordering the slots within a setlist.
 * <p>
 * The ordered list of slot IDs defines the new play sequence.
 * All existing slots must be included exactly once.
 *
 * @param slotIds ordered list of slot IDs defining the new sequence
 */
public record SetlistReorderRequestDTO(
        @NotEmpty(message = "Slot order list must not be empty") List<Long> slotIds) {
}
