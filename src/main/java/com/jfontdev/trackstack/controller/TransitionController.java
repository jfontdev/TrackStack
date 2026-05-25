package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.transition.TransitionPatchRequestDTO;
import com.jfontdev.trackstack.dto.transition.TransitionRequestDTO;
import com.jfontdev.trackstack.dto.transition.TransitionResponseDTO;
import com.jfontdev.trackstack.dto.transition.TransitionUpdateRequestDTO;
import com.jfontdev.trackstack.service.TransitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing transitions between tracks.
 * <p>
 * Provides endpoints for logging, querying, updating, and deleting
 * transitions. A transition represents a directed relationship from one
 * track (source) to another (target) with a user-assigned quality rating
 * and optional notes about the mixing experience.
 * <p>
 * This controller is the primary interface for Phase 02 of the TrackStack
 * evolution: replacing the user's {@code mezclas.md} notes with a
 * queryable, intelligent transition database.
 * <p>
 * All endpoints delegate business logic to the {@link TransitionService},
 * including automatic calculation of harmonic key compatibility and
 * BPM differences when transitions are created.
 */
@RestController
@RequestMapping("/api/transitions")
public class TransitionController {

    private final TransitionService transitionService;

    /**
     * Constructs a new {@code TransitionController} with the required service.
     *
     * @param transitionService the service handling transition business logic
     */
    public TransitionController(TransitionService transitionService) {
        this.transitionService = transitionService;
    }

    /**
     * Creates a new directed transition between two tracks.
     * <p>
     * The request must include the source track ID, target track ID, and a
     * quality rating (1-5). Optional notes and style can describe the
     * mixing experience.
     * <p>
     * Upon creation, the service automatically calculates:
     * <ul>
     *   <li>{@code compatibleKeys} — whether the tracks' keys are harmonically
     *       compatible per the Camelot wheel</li>
     *   <li>{@code bpmDifference} — the absolute BPM difference</li>
     * </ul>
     *
     * @param dto the validated request body containing transition details
     * @return 201 Created with the newly created transition
     */
    @PostMapping
    public ResponseEntity<TransitionResponseDTO> create(@Valid @RequestBody TransitionRequestDTO dto) {
        TransitionResponseDTO response = transitionService.createTransition(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a transition by its ID.
     *
     * @param id the transition's unique identifier
     * @return 200 OK with the transition details, or 404 if not found
     */
    @GetMapping("/{id}")
    public TransitionResponseDTO getById(@PathVariable Long id) {
        return transitionService.getTransitionById(id);
    }

    /**
     * Retrieves all transitions starting from a given track.
     * <p>
     * Answers: "What tracks can I play after this one?"
     * Results are ordered by rating descending (best transitions first).
     *
     * @param trackId the ID of the source track
     * @return 200 OK with a list of transitions from this track
     */
    @GetMapping("/from/{trackId}")
    public List<TransitionResponseDTO> getFromTrack(@PathVariable Long trackId) {
        return transitionService.getTransitionsFromTrack(trackId);
    }

    /**
     * Retrieves all transitions ending at a given track.
     * <p>
     * Answers: "What tracks lead into this one?"
     * Results are ordered by rating descending.
     *
     * @param trackId the ID of the target track
     * @return 200 OK with a list of transitions into this track
     */
    @GetMapping("/to/{trackId}")
    public List<TransitionResponseDTO> getToTrack(@PathVariable Long trackId) {
        return transitionService.getTransitionsToTrack(trackId);
    }

    /**
     * Retrieves the best-rated transitions starting from a specific track.
     * <p>
     * Supports filtering by minimum rating and capping the result count.
     * This is the primary endpoint for discovering what to play next.
     * <p>
     * Example: {@code GET /api/transitions/best?trackId=123&minRating=4&limit=5}
     *
     * @param trackId   the source track ID (required)
     * @param minRating optional minimum rating filter (1-5)
     * @param limit     maximum number of results to return (default 10)
     * @return 200 OK with a list of best transitions
     */
    @GetMapping("/best")
    public List<TransitionResponseDTO> getBest(
            @RequestParam Long trackId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(defaultValue = "10") int limit) {
        return transitionService.getBestTransitionsFromTrack(trackId, minRating, limit);
    }

    /**
     * Fully updates an existing transition (PUT semantics).
     * <p>
     * Replaces the rating, notes, and style. Does not affect the source/target
     * tracks or auto-calculated compatibility fields.
     *
     * @param id  the transition's unique identifier
     * @param dto the validated request body containing new transition details
     * @return 200 OK with the updated transition
     */
    @PutMapping("/{id}")
    public TransitionResponseDTO update(@PathVariable Long id,
                                         @Valid @RequestBody TransitionUpdateRequestDTO dto) {
        return transitionService.updateTransition(id, dto);
    }

    /**
     * Partially updates an existing transition (PATCH semantics).
     * <p>
     * Only non-null fields in the request body are applied.
     *
     * @param id  the transition's unique identifier
     * @param dto the request body containing fields to update
     * @return 200 OK with the updated transition
     */
    @PatchMapping("/{id}")
    public TransitionResponseDTO patch(@PathVariable Long id,
                                        @Valid @RequestBody TransitionPatchRequestDTO dto) {
        return transitionService.patchTransition(id, dto);
    }

    /**
     * Deletes a transition by its ID.
     *
     * @param id the transition's unique identifier
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transitionService.deleteTransition(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Records that a transition was performed in a live set.
     * <p>
     * Increments the transition's play count and updates its last played date.
     * Called when a session or setlist containing this transition is marked
     * as performed.
     *
     * @param id the transition's unique identifier
     * @return 200 OK with the updated transition
     */
    @PostMapping("/{id}/record-play")
    public TransitionResponseDTO recordPlay(@PathVariable Long id) {
        return transitionService.recordTransitionPlay(id);
    }
}
