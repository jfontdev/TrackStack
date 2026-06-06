package com.jfontdev.trackstack.dto.setlist;

import java.util.List;

/**
 * Response DTO representing the transition validation report for a setlist.
 * <p>
 * Analyzes every adjacent pair of tracks in the setlist and reports on
 * transition quality: key compatibility, BPM jump, and whether a logged
 * transition exists in the database.
 *
 * @param setlistId the setlist's unique identifier
 * @param setlistName the display name of the setlist
 * @param pairs list of per-pair validation results
 * @param summary aggregate statistics across all pairs
 */
public record SetlistTransitionValidationDTO(
        Long setlistId,
        String setlistName,
        List<TransitionPairValidation> pairs,
        Summary summary) {

    /**
     * Validation result for a single track-to-track transition within the setlist.
     *
     * @param fromSlotOrder the source slot position
     * @param fromTrackId the source track ID
     * @param fromTrackTitle the source track title
     * @param fromTrackKey the source track musical key
     * @param fromTrackBpm the source track BPM
     * @param toSlotOrder the target slot position
     * @param toTrackId the target track ID
     * @param toTrackTitle the target track title
     * @param toTrackKey the target track musical key
     * @param toTrackBpm the target track BPM
     * @param keyCompatible whether the keys are harmonically compatible (null if unknown)
     * @param bpmDifference the absolute BPM difference (null if unknown)
     * @param knownTransition whether a logged transition exists in the database
     * @param transitionRating the rating of the logged transition (null if none)
     * @param warning a human-readable warning message, or null if no issues
     */
    public record TransitionPairValidation(
            Integer fromSlotOrder,
            Long fromTrackId,
            String fromTrackTitle,
            String fromTrackKey,
            Double fromTrackBpm,
            Integer toSlotOrder,
            Long toTrackId,
            String toTrackTitle,
            String toTrackKey,
            Double toTrackBpm,
            Boolean keyCompatible,
            Double bpmDifference,
            Boolean knownTransition,
            Integer transitionRating,
            String warning) {
    }

    /**
     * Aggregate statistics summarizing the validation report.
     *
     * @param totalPairs total number of adjacent pairs analyzed
     * @param compatibleKeyPairs number of pairs with compatible keys
     * @param incompatibleKeyPairs number of pairs with incompatible keys
     * @param unknownKeyPairs number of pairs where key compatibility is unknown
     * @param knownTransitionPairs number of pairs with a logged transition
     * @param missingTransitionPairs number of pairs with no logged transition
     * @param warningsCount total number of pairs with warnings
     */
    public record Summary(
            int totalPairs,
            int compatibleKeyPairs,
            int incompatibleKeyPairs,
            int unknownKeyPairs,
            int knownTransitionPairs,
            int missingTransitionPairs,
            int warningsCount) {
    }
}
