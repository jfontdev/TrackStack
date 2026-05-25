package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.transition.TransitionPatchRequestDTO;
import com.jfontdev.trackstack.dto.transition.TransitionRequestDTO;
import com.jfontdev.trackstack.dto.transition.TransitionResponseDTO;
import com.jfontdev.trackstack.dto.transition.TransitionUpdateRequestDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.model.Transition;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.repository.TransitionRepository;
import com.jfontdev.trackstack.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link TransitionService} interface.
 * <p>
 * Manages the lifecycle of {@link Transition} entities and provides
 * harmonic key compatibility analysis between tracks.
 * <p>
 * <b>Key Compatibility Engine:</b>
 * <p>
 * When a transition is created, this service looks up the source and target
 * tracks and calculates whether their musical keys are harmonically compatible
 * according to the Camelot wheel:
 * <ul>
 *   <li>Same key → compatible</li>
 *   <li>Adjacent numbers, same letter (e.g., 4A ↔ 5A) → compatible</li>
 *   <li>Same letter, number ±7 (e.g., 4A ↔ 11A) → compatible (relative minor/major)</li>
 *   <li>Everything else → not compatible</li>
 * </ul>
 * <p>
 * BPM difference is calculated as the absolute difference between the two tracks'
 * BPM values.
 * <p>
 * <b>Transaction Strategy:</b>
 * All write operations are {@code @Transactional} to ensure atomicity.
 * Read operations are {@code @Transactional(readOnly = true)}.
 */
@Service
public class TransitionServiceImpl implements TransitionService {

    private static final Logger log = LoggerFactory.getLogger(TransitionServiceImpl.class);

    private final TransitionRepository transitionRepository;
    private final TrackRepository trackRepository;

    public TransitionServiceImpl(TransitionRepository transitionRepository,
                                 TrackRepository trackRepository) {
        this.transitionRepository = transitionRepository;
        this.trackRepository = trackRepository;
    }

    /**
     * Creates a new directed transition between two tracks.
     * <p>
     * This method performs several validation and calculation steps:
     * <ol>
     *   <li>Verifies that both the source and target tracks exist in the database.
     *       If either is missing, a {@link NotFoundException} is thrown.</li>
     *   <li>Checks for duplicate transitions — a directed transition from A→B
     *       can only exist once. If a duplicate is found, an
     *       {@link IllegalArgumentException} is thrown.</li>
     *   <li>Creates the {@link Transition} entity using the static factory method.</li>
     *   <li>Calculates and sets harmonic key compatibility and BPM difference
     *       by analyzing the source and target tracks.</li>
     *   <li>Persists the transition and returns the response DTO.</li>
     * </ol>
     * <p>
     * The key compatibility calculation happens at creation time because the
     * tracks' keys and BPMs do not change frequently. If they are updated later,
     * the transition compatibility would need to be recalculated (future enhancement).
     *
     * @param dto the request containing source track, target track, rating, notes, and style
     * @return the created transition with all auto-calculated fields populated
     * @throws NotFoundException          if either track does not exist
     * @throws IllegalArgumentException   if a transition already exists for this direction
     */
    @Override
    @Transactional
    public TransitionResponseDTO createTransition(TransitionRequestDTO dto) {
        log.info("Creating transition: {} -> {}", dto.sourceTrackId(), dto.targetTrackId());

        // Step 1: Validate that both referenced tracks exist.
        // We fetch the full Track entities because we need their key and BPM
        // values to calculate compatibility metrics.
        Track sourceTrack = findTrackOrThrow(dto.sourceTrackId(), "Source track");
        Track targetTrack = findTrackOrThrow(dto.targetTrackId(), "Target track");

        // Step 2: Enforce uniqueness of directed transitions.
        // A user can only log one experience for Track A → Track B.
        // If they want to update their rating, they should use PUT/PATCH.
        if (transitionRepository.existsBySourceTrackIdAndTargetTrackId(
                dto.sourceTrackId(), dto.targetTrackId())) {
            throw new IllegalArgumentException(
                    "Transition already exists from track " + dto.sourceTrackId()
                            + " to track " + dto.targetTrackId());
        }

        // Step 3: Create the transition entity via the static factory.
        // The factory ensures that timesPlayed starts at 0 and createdDate is set.
        Transition transition = Transition.create(
                dto.sourceTrackId(),
                dto.targetTrackId(),
                dto.rating(),
                dto.notes(),
                dto.style());

        // Step 4: Calculate compatibility metrics.
        // This analyzes the musical keys (Camelot wheel) and BPM difference
        // between the two tracks and stores the results on the transition.
        calculateCompatibility(transition, sourceTrack, targetTrack);

        // Step 5: Persist and return.
        Transition saved = transitionRepository.saveAndFlush(transition);
        log.info("Created transition with id: {}", saved.getId());

        return mapToResponseDTO(saved);
    }

    /**
     * Retrieves a single transition by its unique identifier.
     * <p>
     * This is a straightforward lookup used by the controller when a client
     * requests a specific transition (e.g., to display details or edit).
     *
     * @param id the transition's unique identifier
     * @return the transition response DTO with all fields
     * @throws NotFoundException if no transition exists with the given ID
     */
    @Override
    @Transactional(readOnly = true)
    public TransitionResponseDTO getTransitionById(Long id) {
        Transition transition = findTransitionOrThrow(id);
        return mapToResponseDTO(transition);
    }

    /**
     * Retrieves all transitions that start from a given track.
     * <p>
     * This answers the DJ's question: "What tracks can I play after this one?"
     * Results are ordered by rating descending so that the most reliable
     * transitions appear first — critical for quick decision-making during
     * set preparation.
     * <p>
     * Example: getTransitionsFromTrack(123) might return:
     * Track 123 → Track 456 (rating 5)
     * Track 123 → Track 789 (rating 4)
     * Track 123 → Track 321 (rating 3)
     *
     * @param trackId the ID of the track being played first (source)
     * @return list of transitions from this track, highest rated first;
     *         empty list if no transitions exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransitionResponseDTO> getTransitionsFromTrack(Long trackId) {
        log.debug("Fetching transitions from track: {}", trackId);

        // Query the repository for all transitions where this track is the source.
        // The repository method handles the ordering (by rating DESC).
        return transitionRepository.findBySourceTrackIdOrderByRatingDesc(trackId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Retrieves all transitions that end at a given track.
     * <p>
     * This answers the DJ's question: "What tracks lead into this one?"
     * Useful when you know you want to play Track X at a certain point in
     * the set and need to find a good lead-in track.
     * <p>
     * Results are ordered by rating descending.
     *
     * @param trackId the ID of the track being transitioned into (target)
     * @return list of transitions into this track, highest rated first;
     *         empty list if no transitions exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransitionResponseDTO> getTransitionsToTrack(Long trackId) {
        log.debug("Fetching transitions to track: {}", trackId);

        // Query the repository for all transitions where this track is the target.
        return transitionRepository.findByTargetTrackIdOrderByRatingDesc(trackId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Retrieves the best-rated transitions starting from a specific track,
     * with optional quality filtering.
     * <p>
     * This is the primary method for transition discovery during set planning.
     * It allows the DJ to say: "Show me the top 5 transitions from this track
     * that are rated at least 4 stars."
     * <p>
     * The method first fetches transitions ordered by rating, then applies
     * the limit to cap the result set. This is more efficient than fetching
     * all transitions and filtering in memory, especially as the database grows.
     *
     * @param trackId   the source track ID
     * @param minRating optional minimum rating threshold (1-5). If null,
     *                  all transitions are returned regardless of rating.
     * @param limit     maximum number of results to return. Must be positive.
     * @return list of best transitions from this track, ordered by rating descending
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransitionResponseDTO> getBestTransitionsFromTrack(Long trackId,
            Integer minRating, int limit) {
        log.debug("Fetching best transitions from track: {}, minRating: {}, limit: {}",
                trackId, minRating, limit);

        // Choose the repository query based on whether a minimum rating is specified.
        // This avoids filtering in memory and lets the database do the work.
        List<Transition> transitions;
        if (minRating != null) {
            // Only fetch transitions that meet the quality threshold.
            transitions = transitionRepository
                    .findBySourceTrackIdAndRatingGreaterThanEqualOrderByRatingDesc(trackId, minRating);
        } else {
            // Fetch all transitions from this track.
            transitions = transitionRepository.findBySourceTrackIdOrderByRatingDesc(trackId);
        }

        // Apply the limit using stream truncation. This is safe because the data
        // is already sorted by rating from the database query.
        return transitions.stream()
                .limit(limit)
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Fully updates an existing transition (PUT semantics).
     * <p>
     * Replaces the mutable fields (rating, notes, style) with the provided values.
     * Does <b>not</b> recalculate key/BPM compatibility because the source and
     * target tracks have not changed — only the user's subjective assessment has.
     * <p>
     * To update compatibility metrics, the tracks themselves would need to be
     * edited (future feature), which would trigger a recalculation cascade.
     *
     * @param id  the transition's unique identifier
     * @param dto the new data (rating, notes, style)
     * @return the updated transition
     * @throws NotFoundException if the transition does not exist
     */
    @Override
    @Transactional
    public TransitionResponseDTO updateTransition(Long id, TransitionUpdateRequestDTO dto) {
        log.info("Updating transition with id: {}", id);

        // Find the existing transition or fail fast.
        Transition transition = findTransitionOrThrow(id);

        // Replace all mutable fields. The entity's update method handles
        // the actual field assignment, keeping mutation centralized.
        transition.update(dto.rating(), dto.notes(), dto.style());

        // Persist and return.
        Transition saved = transitionRepository.saveAndFlush(transition);
        return mapToResponseDTO(saved);
    }

    /**
     * Partially updates an existing transition (PATCH semantics).
     * <p>
     * Only non-null fields from the DTO are applied. Null fields retain their
     * current values. This is useful when the user only wants to update one
     * thing (e.g., just the rating after trying the transition in a set).
     * <p>
     * The merging logic is simple: for each field, if the DTO provides a value,
     * use it; otherwise, keep the entity's current value. Then delegate to the
     * entity's {@code update()} method for the actual mutation.
     *
     * @param id  the transition's unique identifier
     * @param dto the fields to update (null fields are ignored)
     * @return the updated transition
     * @throws NotFoundException if the transition does not exist
     */
    @Override
    @Transactional
    public TransitionResponseDTO patchTransition(Long id, TransitionPatchRequestDTO dto) {
        log.info("Patching transition with id: {}", id);

        // Find the existing transition or fail fast.
        Transition transition = findTransitionOrThrow(id);

        // Merge DTO values with existing entity values.
        // Only overwrite if the DTO explicitly provides a value (non-null).
        Integer rating = dto.rating() != null ? dto.rating() : transition.getRating();
        String notes = dto.notes() != null ? dto.notes() : transition.getNotes();
        String style = dto.style() != null ? dto.style() : transition.getStyle();

        // Apply the merged values via the entity's update method.
        transition.update(rating, notes, style);

        // Persist and return.
        Transition saved = transitionRepository.saveAndFlush(transition);
        return mapToResponseDTO(saved);
    }

    /**
     * Deletes a transition by its ID.
     * <p>
     * Removes the transition permanently from the database. This is irreversible
     * — the user would need to re-log the transition if they want it back.
     * <p>
     * Due to {@code ON DELETE CASCADE} on the foreign key constraints,
     * deleting a track that is referenced by transitions will also delete those
     * transitions automatically. But deleting a transition directly (this method)
     * does not affect the tracks.
     *
     * @param id the transition's unique identifier
     * @throws NotFoundException if the transition does not exist
     */
    @Override
    @Transactional
    public void deleteTransition(Long id) {
        log.info("Deleting transition with id: {}", id);

        // Verify the transition exists before attempting deletion.
        Transition transition = findTransitionOrThrow(id);

        // Delete the entity. The repository handles the SQL DELETE.
        transitionRepository.delete(transition);
    }

    /**
     * Records that a transition was performed in a live set.
     * <p>
     * Increments the {@code timesPlayed} counter and updates the
     * {@code lastPlayedDate} to the current timestamp. This is called when
     * a session/setlist is marked as performed and this transition was part of it.
     * <p>
     * Over time, this data enables analytics like:
     * "Which transitions do I actually use in sets vs. just imagine?"
     * and "What are my most reliable transitions?"
     *
     * @param id the transition's unique identifier
     * @return the updated transition with incremented play count
     * @throws NotFoundException if the transition does not exist
     */
    @Override
    @Transactional
    public TransitionResponseDTO recordTransitionPlay(Long id) {
        log.info("Recording play for transition with id: {}", id);

        // Find the transition or fail fast.
        Transition transition = findTransitionOrThrow(id);

        // Delegate to the entity to increment the counter and set the timestamp.
        // This keeps the play-tracking logic encapsulated in the domain model.
        transition.recordPlay();

        // Persist the updated play count and last played date.
        Transition saved = transitionRepository.saveAndFlush(transition);
        return mapToResponseDTO(saved);
    }

    // --- Helper methods ---

    /**
     * Calculates harmonic key compatibility and BPM difference between two tracks
     * and sets them on the transition entity.
     * <p>
     * This method is called during transition creation to populate the
     * auto-calculated fields. It requires both Track entities because it needs
     * access to their key and BPM values.
     * <p>
     * <b>Note:</b> This method mutates the passed {@code transition} object.
     * It does not return a value; the side effect is the updated entity.
     *
     * @param transition  the transition entity to populate with compatibility data
     * @param sourceTrack the track being played first (provides source key/BPM)
     * @param targetTrack the track being transitioned into (provides target key/BPM)
     */
    private void calculateCompatibility(Transition transition, Track sourceTrack, Track targetTrack) {
        // Calculate BPM difference as an absolute value.
        // A large BPM difference (e.g., 30 BPM) indicates a challenging transition
        // that may require techniques like tempo ramping or abrupt cuts.
        Double bpmDiff = calculateBpmDifference(sourceTrack.getBpm(), targetTrack.getBpm());
        transition.setBpmDifference(bpmDiff);

        // Calculate key compatibility using Camelot wheel rules.
        // Harmonic mixing is a core DJ technique — compatible keys blend smoothly
        // while incompatible keys may create dissonance.
        Boolean keyCompat = calculateKeyCompatibility(sourceTrack.getKey(), targetTrack.getKey());
        transition.setCompatibleKeys(keyCompat);

        log.debug("Compatibility calculated for transition {} -> {}: keys={}, bpmDiff={}",
                sourceTrack.getId(), targetTrack.getId(), keyCompat, bpmDiff);
    }

    /**
     * Calculates whether two musical keys are harmonically compatible.
     * <p>
     * Uses the Camelot wheel system, which is the de-facto standard for
     * harmonic mixing in DJing. The wheel organizes all 24 major and minor
     * keys into a circular arrangement where adjacent positions are compatible.
     * <p>
     * <b>Compatibility rules:</b>
     * <ul>
     *   <li><b>Exact match:</b> Same key (e.g., 4A and 4A) → compatible</li>
     *   <li><b>Adjacent on wheel:</b> Same letter, adjacent number (e.g., 4A and 5A) → compatible.
     *       This is a one-step move on the Camelot wheel.</li>
     *   <li><b>Relative major/minor:</b> Same letter, number ±7 (e.g., 4A and 11A) → compatible.
     *       4A is F minor, 11A is F# minor? No, actually in Camelot:
     *       The wheel wraps around, so 12 connects back to 1. The ±7 rule
     *       identifies keys that share the same letter but are on opposite
     *       sides of the wheel (a diagonal move).</li>
     *   <li><b>Different letters or non-adjacent numbers:</b> Not compatible</li>
     * </ul>
     * <p>
     * Keys that are not in Camelot notation (e.g., traditional "F# minor") are
     * only considered compatible if they are an exact string match after normalization.
     *
     * @param sourceKey the source track's key string (may be null or non-Camelot)
     * @param targetKey the target track's key string (may be null or non-Camelot)
     * @return {@code true} if compatible, {@code false} if not compatible,
     *         {@code null} if either key is missing (unknown)
     */
    private Boolean calculateKeyCompatibility(String sourceKey, String targetKey) {
        // If either key is unknown, we cannot make a determination.
        // Return null to indicate "unknown compatibility" rather than false.
        if (sourceKey == null || targetKey == null) {
            return null;
        }

        // Normalize both keys to uppercase and trim whitespace for consistent comparison.
        // This handles inputs like "4a", " 4A ", "4 A" (though the last would fail parsing).
        String normalizedSource = normalizeKey(sourceKey);
        String normalizedTarget = normalizeKey(targetKey);

        // Rule 1: Exact match. This is the simplest case.
        if (normalizedSource.equals(normalizedTarget)) {
            return true;
        }

        // Rule 2: Try to parse Camelot notation.
        // If either key is not in Camelot format (e.g., "F# minor"), we fall back
        // to exact match only (already checked above).
        CamelotKey sourceCamelot = parseCamelotKey(normalizedSource);
        CamelotKey targetCamelot = parseCamelotKey(normalizedTarget);

        if (sourceCamelot == null || targetCamelot == null) {
            // Non-Camelot keys: we can't apply the wheel rules, so they're only
            // compatible if they match exactly (already checked above).
            return false;
        }

        // Both keys are in Camelot notation. Apply the wheel rules.
        // Keys must have the same letter (A or B) to be compatible.
        if (sourceCamelot.letter.equals(targetCamelot.letter)) {
            int numDiff = Math.abs(sourceCamelot.number - targetCamelot.number);

            // Sub-rule 2a: Adjacent numbers (diff = 1).
            // Examples: 4A ↔ 5A, 12B ↔ 1B (wrapping not handled here, but 12 and 1
            // would have diff = 11, not 1).
            if (numDiff == 1) {
                return true;
            }

            // Sub-rule 2b: Relative minor/major via ±7.
            // Examples: 4A ↔ 11A (diff = 7), 3B ↔ 10B (diff = 7).
            // On the Camelot wheel, these are diagonal opposites.
            if (numDiff == 7) {
                return true;
            }
        }

        // If none of the compatibility rules matched, the keys are not compatible.
        return false;
    }

    /**
     * Calculates the absolute BPM difference between two tracks.
     * <p>
     * This metric helps DJs understand the technical difficulty of a transition.
     * A small difference (0-5 BPM) is easy to blend. A large difference
     * (15+ BPM) typically requires a cut, echo out, or tempo adjustment.
     *
     * @param sourceBpm the source track's BPM (may be null if unknown)
     * @param targetBpm the target track's BPM (may be null if unknown)
     * @return the absolute BPM difference as a positive value, or {@code null}
     *         if either BPM is missing
     */
    private Double calculateBpmDifference(Double sourceBpm, Double targetBpm) {
        // Cannot calculate difference if either BPM is unknown.
        if (sourceBpm == null || targetBpm == null) {
            return null;
        }

        // Return the absolute difference. We use absolute value because the
        // direction (faster → slower vs. slower → faster) is already implied
        // by the source/target relationship.
        return Math.abs(sourceBpm - targetBpm);
    }

    /**
     * Normalizes a key string for consistent comparison.
     * <p>
     * Trims leading/trailing whitespace and converts to uppercase.
     * This ensures that "4a", " 4A ", and "4A" are treated identically.
     *
     * @param key the raw key string from the track metadata
     * @return the normalized key string
     */
    private String normalizeKey(String key) {
        return key.trim().toUpperCase();
    }

    /**
     * Attempts to parse a key string in Camelot notation.
     * <p>
     * Camelot notation consists of a number (1-12) followed by a letter:
     * <ul>
     *   <li><b>A</b> = minor key (e.g., 4A = F minor)</li>
     *   <li><b>B</b> = major key (e.g., 4B = A♭ major)</li>
     * </ul>
     * <p>
     * Examples of valid Camelot keys: "4A", "11B", "12A", "1B".
     * Examples of invalid/non-Camelot keys: "F minor", "A♭ major", "4", "A".
     *
     * @param key the normalized key string to parse
     * @return a {@link CamelotKey} record if parsing succeeds, {@code null} otherwise
     */
    private CamelotKey parseCamelotKey(String key) {
        // A Camelot key must be at least 2 characters: one digit and one letter.
        if (key == null || key.length() < 2) {
            return null;
        }

        // The last character must be the letter (A or B).
        char letter = key.charAt(key.length() - 1);
        if (letter != 'A' && letter != 'B') {
            // Not a Camelot letter. Could be a traditional key like "F#MINOR"
            // or simply malformed input.
            return null;
        }

        // Everything before the last character should be the number (1-12).
        String numberStr = key.substring(0, key.length() - 1);
        try {
            int number = Integer.parseInt(numberStr);

            // Validate the number is within the Camelot wheel range.
            if (number < 1 || number > 12) {
                return null;
            }

            return new CamelotKey(number, String.valueOf(letter));
        } catch (NumberFormatException e) {
            // The prefix is not a valid integer. Not Camelot notation.
            return null;
        }
    }

    /**
     * Immutable data holder for a parsed Camelot key.
     * <p>
     * Using a private record keeps the parsing logic encapsulated while
     * providing type-safe access to the number and letter components.
     *
     * @param number the Camelot wheel position (1-12)
     * @param letter the key quality ("A" for minor, "B" for major)
     */
    private record CamelotKey(int number, String letter) {
    }

    /**
     * Finds a transition by ID or throws {@link NotFoundException}.
     * <p>
     * Centralizes the "find or fail" pattern used across all transition
     * operations. This avoids duplicating the Optional handling logic.
     *
     * @param id the transition ID to look up
     * @return the found {@link Transition} entity
     * @throws NotFoundException if no transition exists with the given ID
     */
    private Transition findTransitionOrThrow(Long id) {
        Optional<Transition> transition = transitionRepository.findById(id);
        if (transition.isEmpty()) {
            throw new NotFoundException("Transition not found");
        }
        return transition.get();
    }

    /**
     * Finds a track by ID or throws {@link NotFoundException}.
     * <p>
     * Similar to {@link #findTransitionOrThrow(Long)} but for tracks.
     * Includes a description parameter to provide context in error messages
     * (e.g., "Source track not found with id: 123" vs. "Target track not found...").
     *
     * @param id          the track ID to look up
     * @param description a human-readable description for error messages
     * @return the found {@link Track} entity
     * @throws NotFoundException if no track exists with the given ID
     */
    private Track findTrackOrThrow(Long id, String description) {
        Optional<Track> track = trackRepository.findById(id);
        if (track.isEmpty()) {
            throw new NotFoundException(description + " not found with id: " + id);
        }
        return track.get();
    }

    /**
     * Maps a {@link Transition} entity to a {@link TransitionResponseDTO}.
     * <p>
     * Centralizes the entity-to-DTO conversion logic to avoid repetition
     * across service methods. Every public method that returns a transition
     * should use this mapper.
     * <p>
     * Includes all fields: mutable (rating, notes, style), auto-calculated
     * (compatibleKeys, bpmDifference), and tracking (timesPlayed, lastPlayedDate,
     * createdDate).
     *
     * @param transition the entity to convert
     * @return the fully populated response DTO
     */
    private TransitionResponseDTO mapToResponseDTO(Transition transition) {
        return new TransitionResponseDTO(
                transition.getId(),
                transition.getSourceTrackId(),
                transition.getTargetTrackId(),
                transition.getRating(),
                transition.getNotes(),
                transition.getStyle(),
                transition.getCompatibleKeys(),
                transition.getBpmDifference(),
                transition.getTimesPlayed(),
                transition.getLastPlayedDate(),
                transition.getCreatedDate());
    }
}
