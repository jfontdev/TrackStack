package com.jfontdev.trackstack.dto.setlist;

import java.util.List;

/**
 * Response DTO representing a setlist exported for external use.
 * <p>
 * Provides structured metadata suitable for downstream tools, XDJ-AZ
 * reference, or backup. Includes the full track sequence with all
 * relevant mixing metadata (key, BPM, energy, duration).
 *
 * @param setlistId           the setlist's unique identifier
 * @param setlistName         the display name
 * @param status              the lifecycle status
 * @param totalDurationSeconds summed duration of all tracks
 * @param slots               ordered list of export slots
 */
public record SetlistExportDTO(
        Long setlistId,
        String setlistName,
        String status,
        Integer totalDurationSeconds,
        List<ExportSlot> slots) {

    /**
     * A single slot in the exported setlist.
     *
     * @param slotOrder       the position in the sequence
     * @param trackId         the track ID
     * @param trackTitle      the track title
     * @param trackArtist     the track artist
     * @param trackKey        the musical key
     * @param trackBpm        the BPM
     * @param energy          the slot's energy level (1-5), may be null
     * @param notes           free-text notes
     * @param durationSeconds the track duration in seconds, may be null
     */
    public record ExportSlot(
            Integer slotOrder,
            Long trackId,
            String trackTitle,
            String trackArtist,
            String trackKey,
            Double trackBpm,
            Integer energy,
            String notes,
            Integer durationSeconds) {
    }
}
