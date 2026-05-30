package com.jfontdev.trackstack.repository;

import com.jfontdev.trackstack.model.Setlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link Setlist} persistence operations.
 * <p>
 * Provides standard CRUD via {@link JpaRepository} and query methods
 * for filtering setlists by their lifecycle status.
 */
public interface SetlistRepository extends JpaRepository<Setlist, Long> {

    /**
     * Finds all setlists with the given status, ordered by creation
     * date descending (newest first).
     *
     * @param status the setlist status (DRAFT, READY, PERFORMED)
     * @return list of matching setlists, newest first
     */
    List<Setlist> findByStatusOrderByCreatedDateDesc(String status);
}
