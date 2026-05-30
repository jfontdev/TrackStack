package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.setlist.SetlistReorderRequestDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistRequestDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistResponseDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistSlotRequestDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistUpdateRequestDTO;
import com.jfontdev.trackstack.service.SetlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing setlists and their slots.
 * <p>
 * Provides endpoints for building, modifying, and progressing setlists
 * through their lifecycle (DRAFT → READY → PERFORMED). Slot management
 * endpoints allow adding, removing, updating, and reordering tracks
 * within a setlist.
 * <p>
 * This controller is the primary interface for Phase 03 of TrackStack:
 * Set Planning. All endpoints delegate business logic to the
 * {@link SetlistService}.
 */
@RestController
@RequestMapping("/api/setlists")
public class SetlistController {

    private final SetlistService setlistService;

    /**
     * Constructs a new {@code SetlistController} with the required service.
     *
     * @param setlistService the service handling setlist business logic
     */
    public SetlistController(SetlistService setlistService) {
        this.setlistService = setlistService;
    }

    /**
     * Creates a new setlist in DRAFT status.
     * <p>
     * Optionally includes initial slots for the setlist. Each slot
     * references an existing track by ID.
     *
     * @param dto the validated request body containing setlist metadata and optional slots
     * @return 201 Created with the newly created setlist
     */
    @PostMapping
    public ResponseEntity<SetlistResponseDTO> create(@Valid @RequestBody SetlistRequestDTO dto) {
        SetlistResponseDTO response = setlistService.createSetlist(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a setlist by its ID with all ordered slots.
     *
     * @param id the setlist's unique identifier
     * @return 200 OK with the setlist details, or 404 if not found
     */
    @GetMapping("/{id}")
    public SetlistResponseDTO getById(@PathVariable Long id) {
        return setlistService.getSetlistById(id);
    }

    /**
     * Retrieves all setlists, optionally filtered by status.
     * <p>
     * Results are ordered by creation date descending (newest first).
     *
     * @param status optional status filter (DRAFT, READY, PERFORMED)
     * @return 200 OK with a list of setlists
     */
    @GetMapping
    public List<SetlistResponseDTO> getAll(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return setlistService.getSetlistsByStatus(status);
        }
        return setlistService.getAllSetlists();
    }

    /**
     * Fully updates an existing setlist's metadata (PUT semantics).
     * <p>
     * Replaces the name and description. Does not modify slots or status.
     *
     * @param id  the setlist's unique identifier
     * @param dto the validated request body containing new metadata
     * @return 200 OK with the updated setlist, or 404 if not found
     */
    @PutMapping("/{id}")
    public SetlistResponseDTO update(@PathVariable Long id,
                                     @Valid @RequestBody SetlistUpdateRequestDTO dto) {
        return setlistService.updateSetlist(id, dto);
    }

    /**
     * Deletes a setlist and all of its slots.
     *
     * @param id the setlist's unique identifier
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        setlistService.deleteSetlist(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a new slot to an existing setlist.
     *
     * @param id  the setlist's unique identifier
     * @param dto the validated request body containing slot details
     * @return 200 OK with the updated setlist
     */
    @PostMapping("/{id}/slots")
    public SetlistResponseDTO addSlot(@PathVariable Long id,
                                      @Valid @RequestBody SetlistSlotRequestDTO dto) {
        return setlistService.addSlot(id, dto);
    }

    /**
     * Removes a slot from a setlist.
     *
     * @param setlistId the setlist's unique identifier
     * @param slotId    the slot's unique identifier
     * @return 200 OK with the updated setlist
     */
    @DeleteMapping("/{setlistId}/slots/{slotId}")
    public SetlistResponseDTO removeSlot(@PathVariable Long setlistId,
                                          @PathVariable Long slotId) {
        return setlistService.removeSlot(setlistId, slotId);
    }

    /**
     * Updates an existing slot within a setlist.
     *
     * @param setlistId the setlist's unique identifier
     * @param slotId    the slot's unique identifier
     * @param dto       the validated request body containing new slot details
     * @return 200 OK with the updated setlist
     */
    @PutMapping("/{setlistId}/slots/{slotId}")
    public SetlistResponseDTO updateSlot(@PathVariable Long setlistId,
                                          @PathVariable Long slotId,
                                          @Valid @RequestBody SetlistSlotRequestDTO dto) {
        return setlistService.updateSlot(setlistId, slotId, dto);
    }

    /**
     * Reorders the slots of a setlist.
     * <p>
     * The request body provides an ordered list of slot IDs that defines
     * the new play sequence. All existing slots must be included exactly once.
     *
     * @param id  the setlist's unique identifier
     * @param dto the validated request body containing the new slot order
     * @return 200 OK with the updated setlist
     */
    @PutMapping("/{id}/slots/reorder")
    public SetlistResponseDTO reorderSlots(@PathVariable Long id,
                                            @Valid @RequestBody SetlistReorderRequestDTO dto) {
        return setlistService.reorderSlots(id, dto.slotIds());
    }

    /**
     * Marks a setlist as ready for performance.
     * <p>
     * Transitions status from DRAFT to READY.
     *
     * @param id the setlist's unique identifier
     * @return 200 OK with the updated setlist
     */
    @PostMapping("/{id}/ready")
    public SetlistResponseDTO markReady(@PathVariable Long id) {
        return setlistService.markReady(id);
    }

    /**
     * Marks a setlist as performed.
     * <p>
     * Transitions status to PERFORMED and records the performance date.
     *
     * @param id the setlist's unique identifier
     * @return 200 OK with the updated setlist
     */
    @PostMapping("/{id}/performed")
    public SetlistResponseDTO markPerformed(@PathVariable Long id) {
        return setlistService.markPerformed(id);
    }
}
