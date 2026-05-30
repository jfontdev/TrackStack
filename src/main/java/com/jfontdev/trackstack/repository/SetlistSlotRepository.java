package com.jfontdev.trackstack.repository;

import com.jfontdev.trackstack.model.SetlistSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link SetlistSlot} persistence operations.
 * <p>
 * Provides standard CRUD via {@link JpaRepository} and query methods
 * for working with the ordered slots within a setlist.
 */
public interface SetlistSlotRepository extends JpaRepository<SetlistSlot, Long> {

    /**
     * Finds all slots belonging to a specific setlist, ordered by
     * slot position ascending (the intended play order).
     *
     * @param setlistId the parent setlist ID
     * @return list of slots in play order
     */
    List<SetlistSlot> findBySetlistIdOrderBySlotOrderAsc(Long setlistId);

    /**
     * Deletes all slots belonging to a specific setlist.
     * <p>
     * Called when a setlist is deleted to clean up its slots.
     *
     * @param setlistId the parent setlist ID
     */
    void deleteBySetlistId(Long setlistId);
}
