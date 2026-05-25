package com.jfontdev.trackstack.repository;

import com.jfontdev.trackstack.model.Transition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Transition} persistence operations.
 * <p>
 * Provides standard CRUD via {@link JpaRepository} and query methods
 * for discovering transitions from/to specific tracks.
 * <p>
 * All methods return transitions ordered by rating descending so that
 * the best-rated transitions appear first — the most common use case
 * when a DJ is looking for what to play next.
 */
public interface TransitionRepository extends JpaRepository<Transition, Long> {

    /**
     * Finds all transitions starting from the given source track,
     * ordered by rating descending (best first).
     *
     * @param sourceTrackId the track being played first
     * @return list of transitions from this track, highest rated first
     */
    List<Transition> findBySourceTrackIdOrderByRatingDesc(Long sourceTrackId);

    /**
     * Finds all transitions ending at the given target track,
     * ordered by rating descending.
     *
     * @param targetTrackId the track being transitioned into
     * @return list of transitions into this track, highest rated first
     */
    List<Transition> findByTargetTrackIdOrderByRatingDesc(Long targetTrackId);

    /**
     * Finds transitions from a specific source track that meet a minimum
     * quality threshold, ordered by rating descending.
     * <p>
     * Useful for "show me only the good transitions" filtering.
     *
     * @param sourceTrackId the track being played first
     * @param minRating     minimum acceptable rating (inclusive)
     * @return list of qualifying transitions, highest rated first
     */
    List<Transition> findBySourceTrackIdAndRatingGreaterThanEqualOrderByRatingDesc(
            Long sourceTrackId, Integer minRating);

    /**
     * Finds the single best-rated transition from a source track.
     * <p>
     * Returns empty if no transitions exist from this track.
     *
     * @param sourceTrackId the track being played first
     * @return the highest-rated transition, or empty
     */
    Optional<Transition> findTopBySourceTrackIdOrderByRatingDesc(Long sourceTrackId);

    /**
     * Checks if a directed transition between two tracks already exists.
     * <p>
     * Used to prevent duplicate entries when logging transitions.
     *
     * @param sourceTrackId the track being played first
     * @param targetTrackId the track being transitioned into
     * @return true if this exact transition exists
     */
    boolean existsBySourceTrackIdAndTargetTrackId(Long sourceTrackId, Long targetTrackId);

    /**
     * Finds a specific directed transition by its source and target tracks.
     *
     * @param sourceTrackId the track being played first
     * @param targetTrackId the track being transitioned into
     * @return the transition, or empty if not found
     */
    Optional<Transition> findBySourceTrackIdAndTargetTrackId(Long sourceTrackId, Long targetTrackId);
}
