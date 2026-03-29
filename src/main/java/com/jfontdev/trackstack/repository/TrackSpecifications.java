package com.jfontdev.trackstack.repository;

import com.jfontdev.trackstack.model.Track;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable JPA specifications for querying {@link Track} entities.
 * <p>
 * These predicates are composed in the service layer to implement
 * dynamic filtering without pushing business logic into controllers
 * or repositories.
 * </p>
 */
public final class TrackSpecifications {

    /**
     * Utility class constructor.
     * <p>
     * This class only exposes static factory methods.
     * </p>
     */
    private TrackSpecifications() {
    }

    /**
     * Builds a predicate for tracks whose BPM is greater than or equal to
     * the provided value.
     *
     * @param bpmMin the minimum BPM (inclusive)
     * @return a specification applying the minimum BPM filter
     */
    public static Specification<Track> hasBpmGreaterThanOrEqualTo(Double bpmMin) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("bpm"), bpmMin);
    }

    /**
     * Builds a predicate for tracks whose BPM is less than or equal to
     * the provided value.
     *
     * @param bpmMax the maximum BPM (inclusive)
     * @return a specification applying the maximum BPM filter
     */
    public static Specification<Track> hasBpmLessThanOrEqualTo(Double bpmMax) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("bpm"), bpmMax);
    }

    /**
     * Builds a case-insensitive equality predicate for musical key.
     *
     * @param musicalKey the key value to match
     * @return a specification applying the key filter
     */
    public static Specification<Track> hasMusicalKey(String musicalKey) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("key")),
                musicalKey.toLowerCase());
    }

    /**
     * Builds a case-insensitive equality predicate for genre.
     *
     * @param genre the genre value to match
     * @return a specification applying the genre filter
     */
    public static Specification<Track> hasGenre(String genre) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("genre")),
                genre.toLowerCase());
    }
}
