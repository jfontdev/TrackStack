package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.setlist.SetlistEnergyArcDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistExportDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistRequestDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistResponseDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistSlotRequestDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistTransitionValidationDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistUpdateRequestDTO;

import java.util.List;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Setlist}
 * entities and their {@link com.jfontdev.trackstack.model.SetlistSlot} children.
 * <p>
 * Defines the contract for setlist-related business operations including
 * full CRUD, slot management, lifecycle transitions, and ordering.
 */
public interface SetlistService {

    /**
     * Creates a new setlist in DRAFT status with optional initial slots.
     * <p>
     * Each slot is validated to ensure the referenced track exists.
     *
     * @param dto the request containing setlist metadata and optional slots
     * @return the created setlist with populated slots
     */
    SetlistResponseDTO createSetlist(SetlistRequestDTO dto);

    /**
     * Retrieves a setlist by its unique identifier.
     *
     * @param id the setlist's unique identifier
     * @return the setlist with its ordered slots
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    SetlistResponseDTO getSetlistById(Long id);

    /**
     * Retrieves all setlists, ordered by creation date descending.
     *
     * @return list of all setlists, newest first
     */
    List<SetlistResponseDTO> getAllSetlists();

    /**
     * Retrieves setlists filtered by status, ordered by creation date descending.
     *
     * @param status the lifecycle status (DRAFT, READY, PERFORMED)
     * @return list of matching setlists, newest first
     */
    List<SetlistResponseDTO> getSetlistsByStatus(String status);

    /**
     * Fully updates an existing setlist's metadata (PUT semantics).
     * <p>
     * Replaces name and description. Does not modify slots or status.
     *
     * @param id  the setlist's unique identifier
     * @param dto the new metadata
     * @return the updated setlist
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    SetlistResponseDTO updateSetlist(Long id, SetlistUpdateRequestDTO dto);

    /**
     * Deletes a setlist and all of its slots.
     * <p>
     * Due to {@code ON DELETE CASCADE} on the foreign key, slots are
     * automatically cleaned up. This operation is irreversible.
     *
     * @param id the setlist's unique identifier
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    void deleteSetlist(Long id);

    /**
     * Adds a new slot to an existing setlist.
     * <p>
     * Validates that the referenced track exists. The slot order should
     * reflect the intended position in the sequence.
     *
     * @param setlistId the parent setlist ID
     * @param dto       the slot details
     * @return the updated setlist with the new slot included
     * @throws com.jfontdev.trackstack.exception.NotFoundException if setlist or track not found
     */
    SetlistResponseDTO addSlot(Long setlistId, SetlistSlotRequestDTO dto);

    /**
     * Removes a slot from its setlist.
     * <p>
     * Does not affect the track itself, only the slot reference.
     *
     * @param setlistId the parent setlist ID
     * @param slotId    the slot to remove
     * @return the updated setlist
     * @throws com.jfontdev.trackstack.exception.NotFoundException if setlist or slot not found
     */
    SetlistResponseDTO removeSlot(Long setlistId, Long slotId);

    /**
     * Updates an existing slot within a setlist.
     * <p>
     * Replaces the track, energy, and notes for the given slot.
     *
     * @param setlistId the parent setlist ID
     * @param slotId    the slot to update
     * @param dto       the new slot details
     * @return the updated setlist
     * @throws com.jfontdev.trackstack.exception.NotFoundException if setlist, slot, or track not found
     */
    SetlistResponseDTO updateSlot(Long setlistId, Long slotId, SetlistSlotRequestDTO dto);

    /**
     * Reorders the slots of a setlist to match the provided sequence.
     * <p>
     * The list of slot IDs defines the new play order. All existing slots
     * must be included exactly once.
     *
     * @param setlistId   the parent setlist ID
     * @param slotIdOrder ordered list of slot IDs defining the new sequence
     * @return the updated setlist with reordered slots
     * @throws com.jfontdev.trackstack.exception.NotFoundException if setlist not found
     * @throws IllegalArgumentException if the provided order is invalid
     */
    SetlistResponseDTO reorderSlots(Long setlistId, List<Long> slotIdOrder);

    /**
     * Marks a setlist as ready for performance.
     * <p>
     * Transitions status from DRAFT to READY.
     *
     * @param id the setlist's unique identifier
     * @return the updated setlist
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    SetlistResponseDTO markReady(Long id);

    /**
     * Marks a setlist as performed and records the performance date.
     * <p>
     * Transitions status from READY to PERFORMED.
     *
     * @param id the setlist's unique identifier
     * @return the updated setlist
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    SetlistResponseDTO markPerformed(Long id);

    /**
     * Retrieves the energy arc visualization for a setlist.
     * <p>
     * Returns an ordered sequence of energy points (one per slot) plus
     * aggregate statistics describing the overall energy progression.
     * Each point includes the slot's energy level and the track's metadata
     * (title, artist, BPM, key, duration).
     *
     * @param id the setlist's unique identifier
     * @return the energy arc data with points and statistics
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    SetlistEnergyArcDTO getEnergyArc(Long id);

    /**
     * Validates all track-to-track transitions within a setlist.
     * <p>
     * Analyzes every adjacent pair of tracks and reports on:
     * <ul>
     *   <li>Key compatibility (via Camelot wheel rules)</li>
     *   <li>BPM difference</li>
     *   <li>Whether a logged transition exists in the database</li>
     * </ul>
     *
     * @param id the setlist's unique identifier
     * @return the validation report with per-pair details and summary
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     */
    SetlistTransitionValidationDTO validateTransitions(Long id);

    /**
     * Exports a setlist in the requested format.
     * <p>
     * Supports two formats:
     * <ul>
     *   <li>{@code "json"} (default) — returns structured metadata as {@link SetlistExportDTO}</li>
     *   <li>{@code "text"} — returns a plain text playlist for quick reference</li>
     * </ul>
     *
     * @param id     the setlist's unique identifier
     * @param format the export format: "json" or "text"
     * @return the exported setlist data
     * @throws com.jfontdev.trackstack.exception.NotFoundException if not found
     * @throws IllegalArgumentException if format is not supported
     */
    SetlistExportDTO exportSetlist(Long id, String format);
}
