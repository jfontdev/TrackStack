package com.jfontdev.trackstack.repository;

import com.jfontdev.trackstack.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository for {@link Track} persistence operations.
 * <p>
 * Extends {@link JpaRepository} for standard CRUD behavior and
 * {@link JpaSpecificationExecutor} for dynamic filtering used by
 * pageable list endpoints.
 * </p>
 */
public interface TrackRepository extends JpaRepository<Track, Long>, JpaSpecificationExecutor<Track> {

    /**
     * Checks if a track exists with the given file path.
     *
     * @param filePath the file path to check
     * @return true if a track with this file path exists
     */
    boolean existsByFilePath(String filePath);
}
