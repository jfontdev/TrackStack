package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.transition.TransitionPatchRequestDTO;
import com.jfontdev.trackstack.dto.transition.TransitionRequestDTO;
import com.jfontdev.trackstack.dto.transition.TransitionResponseDTO;
import com.jfontdev.trackstack.dto.transition.TransitionUpdateRequestDTO;

import java.util.List;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Transition}
 * entities.
 * <p>
 * Defines the contract for transition-related business operations including
 * CRUD, partial updates, and discovery of transitions between tracks.
 * <p>
 * All operations that create or update transitions automatically calculate
 * harmonic key compatibility and BPM differences by looking up the associated
 * tracks from the database.
 */
public interface TransitionService {

    /**
     * Creates a new transition between two tracks.
     * <p>
     * Automatically calculates {@code compatibleKeys} and {@code bpmDifference}
     * by looking up the source and target tracks. Throws
     * {@link com.jfontdev.trackstack.exception.NotFoundException} if either
     * track does not exist.
     *
     * @param dto the request containing transition details
     * @return the created transition with auto-calculated fields populated
     */
    TransitionResponseDTO createTransition(TransitionRequestDTO dto);

    /**
     * Retrieves a transition by its ID.
     *
     * @param id the transition's unique identifier
     * @return the transition details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    TransitionResponseDTO getTransitionById(Long id);

    /**
     * Retrieves all transitions starting from a specific track,
     * ordered by rating descending (best first).
     *
     * @param trackId the source track ID
     * @return list of transitions from this track, highest rated first
     */
    List<TransitionResponseDTO> getTransitionsFromTrack(Long trackId);

    /**
     * Retrieves all transitions ending at a specific track,
     * ordered by rating descending.
     *
     * @param trackId the target track ID
     * @return list of transitions into this track, highest rated first
     */
    List<TransitionResponseDTO> getTransitionsToTrack(Long trackId);

    /**
     * Retrieves the best-rated transitions starting from a specific track.
     * <p>
     * Optionally filters by minimum rating threshold. Results are ordered
     * by rating descending.
     *
     * @param trackId   the source track ID
     * @param minRating optional minimum rating (1-5), or null for all
     * @param limit     maximum number of results to return
     * @return list of best transitions from this track
     */
    List<TransitionResponseDTO> getBestTransitionsFromTrack(Long trackId,
            Integer minRating, int limit);

    /**
     * Fully updates an existing transition (PUT semantics).
     * <p>
     * Replaces rating, notes, and style. Does not recalculate key/BPM
     * compatibility since the tracks haven't changed.
     *
     * @param id  the transition's unique identifier
     * @param dto the new transition data
     * @return the updated transition
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    TransitionResponseDTO updateTransition(Long id, TransitionUpdateRequestDTO dto);

    /**
     * Partially updates an existing transition (PATCH semantics).
     * <p>
     * Only non-null fields from the DTO are applied to the existing transition.
     *
     * @param id  the transition's unique identifier
     * @param dto the fields to update
     * @return the updated transition
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    TransitionResponseDTO patchTransition(Long id, TransitionPatchRequestDTO dto);

    /**
     * Deletes a transition by its ID.
     *
     * @param id the transition's unique identifier
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    void deleteTransition(Long id);

    /**
     * Records that a transition was performed in a live set.
     * <p>
     * Increments the {@code timesPlayed} counter and updates
     * {@code lastPlayedDate} to now.
     *
     * @param id the transition's unique identifier
     * @return the updated transition
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    TransitionResponseDTO recordTransitionPlay(Long id);
}
