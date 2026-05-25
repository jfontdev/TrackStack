package com.jfontdev.trackstack.dto.transition;

import java.time.LocalDateTime;

/**
 * Response DTO representing a transition returned by the API.
 * <p>
 * Includes all fields of a transition, including auto-calculated compatibility
 * metrics and usage statistics. This is the single representation used in
 * all API responses.
 *
 * @param id              the transition's unique identifier
 * @param sourceTrackId   the track being played first
 * @param targetTrackId   the track being transitioned into
 * @param rating          user's quality rating (1-5)
 * @param notes           free-text observations
 * @param style           transition technique used
 * @param compatibleKeys  true if keys are harmonically compatible
 * @param bpmDifference   absolute BPM difference between tracks
 * @param timesPlayed     how many times this transition was performed
 * @param lastPlayedDate  most recent performance date
 * @param createdDate     when this transition was first logged
 */
public record TransitionResponseDTO(
        Long id,
        Long sourceTrackId,
        Long targetTrackId,
        Integer rating,
        String notes,
        String style,
        Boolean compatibleKeys,
        Double bpmDifference,
        Integer timesPlayed,
        LocalDateTime lastPlayedDate,
        LocalDateTime createdDate) {
}
