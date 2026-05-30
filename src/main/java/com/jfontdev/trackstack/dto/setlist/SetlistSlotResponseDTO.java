package com.jfontdev.trackstack.dto.setlist;

/**
 * Response DTO representing a single slot within a setlist.
 * <p>
 * Includes the slot's position, referenced track, energy level, and notes.
 *
 * @param id        the slot's unique identifier
 * @param setlistId the parent setlist ID
 * @param trackId   the track placed in this slot
 * @param slotOrder the position in the sequence
 * @param energy    energy level 1-5
 * @param notes     free-text notes
 */
public record SetlistSlotResponseDTO(
        Long id,
        Long setlistId,
        Long trackId,
        Integer slotOrder,
        Integer energy,
        String notes) {
}
