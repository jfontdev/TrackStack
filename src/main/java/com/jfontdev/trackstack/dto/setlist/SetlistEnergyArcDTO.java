package com.jfontdev.trackstack.dto.setlist;

import java.util.List;

/**
 * Response DTO representing the energy arc visualization of a setlist.
 * <p>
 * Provides an ordered sequence of energy points (one per slot) plus
 * aggregate statistics that describe the overall energy progression.
 *
 * @param setlistId   the setlist's unique identifier
 * @param setlistName the display name of the setlist
 * @param points      ordered list of energy points, one per slot
 * @param stats       aggregate statistics describing the energy arc
 */
public record SetlistEnergyArcDTO(
        Long setlistId,
        String setlistName,
        List<EnergyPoint> points,
        EnergyStats stats) {

    /**
     * A single data point in the energy arc, representing one slot.
     *
     * @param slotOrder        the position in the setlist sequence
     * @param trackId          the track placed in this slot
     * @param trackTitle       the track's title
     * @param trackArtist      the track's artist
     * @param energy           the slot's energy level (1-5), may be null
     * @param bpm              the track's BPM, may be null
     * @param key              the track's musical key, may be null
     * @param durationSeconds  the track's duration in seconds, may be null
     */
    public record EnergyPoint(
            Integer slotOrder,
            Long trackId,
            String trackTitle,
            String trackArtist,
            Integer energy,
            Double bpm,
            String key,
            Integer durationSeconds) {
    }

    /**
     * Aggregate statistics describing the overall energy progression.
     *
     * @param averageEnergy the arithmetic mean of all non-null energy values
     * @param peakEnergy    the highest energy value in the setlist
     * @param lowEnergy     the lowest energy value in the setlist
     * @param energyTrend   a qualitative label: "BUILD", "PEAK", "COOLDOWN", or "FLAT"
     */
    public record EnergyStats(
            Double averageEnergy,
            Integer peakEnergy,
            Integer lowEnergy,
            String energyTrend) {
    }
}
