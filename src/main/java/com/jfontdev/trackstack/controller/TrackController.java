package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.track.TrackPatchRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackPageResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackUpdateRequestDTO;
import com.jfontdev.trackstack.service.TrackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing tracks.
 * <p>
 * Provides endpoints for full CRUD operations on tracks, audio library
 * scanning, and track discovery with filtering and pagination.
 * This controller delegates all business logic to the {@link TrackService}
 * and only handles HTTP concerns (request binding, status codes,
 * response formatting).
 */
@RestController
@RequestMapping("/api/tracks")
public class TrackController {

    private final TrackService trackService;

    /**
     * Constructs a new {@code TrackController} with the required service.
     *
     * @param trackService the service handling track business logic
     */
    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    /**
     * Creates a new track.
     *
     * @param dto the validated request body containing track details
     * @return 201 Created with the newly created track
     */
    @PostMapping
    public ResponseEntity<TrackResponseDTO> create(@Valid @RequestBody TrackRequestDTO dto) {
        TrackResponseDTO response = trackService.createTrack(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a track by its ID.
     *
     * @param id the track's unique identifier
     * @return 200 OK with the track details, or 404 if not found
     */
    @GetMapping("/{id}")
    public TrackResponseDTO getById(@PathVariable Long id) {
        return trackService.getTrackById(id);
    }

    /**
     * Retrieves tracks using pagination, sorting, and optional filters.
     * <p>
     * Default query values are {@code page=0}, {@code size=20}, and
     * {@code sort=title,asc}. Optional filters can narrow the result set by
     * BPM range, musical key, and genre.
     * </p>
     *
     * @param page       zero-based page index (default 0)
     * @param size       page size (default 20)
     * @param sort       sort expression in the format "field,direction"
     *                   (default "title,asc")
     * @param bpmMin     optional minimum BPM filter (inclusive)
     * @param bpmMax     optional maximum BPM filter (inclusive)
     * @param musicalKey optional exact key filter
     * @param genre      optional exact genre filter
     * @return 200 OK with a page of tracks
     */
    @GetMapping
    public TrackPageResponseDTO getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort,
            @RequestParam(required = false) Double bpmMin,
            @RequestParam(required = false) Double bpmMax,
            @RequestParam(name = "key", required = false) String musicalKey,
            @RequestParam(required = false) String genre) {
        return trackService.getAllTracks(page, size, sort, bpmMin, bpmMax, musicalKey, genre);
    }

    /**
     * Scans the configured music directory and imports any new audio files
     * into the track library.
     *
     * @return 200 OK with the number of tracks imported
     */
    @PostMapping("/scan")
    public ResponseEntity<Integer> scanLibrary() {
        int importedCount = trackService.scanLibrary();
        return ResponseEntity.ok(importedCount);
    }

    /**
     * Fully updates an existing track (PUT semantics).
     * <p>
     * All fields in the request body replace the existing values.
     *
     * @param id  the track's unique identifier
     * @param dto the validated request body containing the new track details
     * @return 200 OK with the updated track, or 404 if not found
     */
    @PutMapping("/{id}")
    public TrackResponseDTO update(@PathVariable Long id, @Valid @RequestBody TrackUpdateRequestDTO dto) {
        return trackService.updateTrack(id, dto);
    }

    /**
     * Partially updates an existing track (PATCH semantics).
     * <p>
     * Only non-null fields in the request body are applied to the existing track.
     *
     * @param id  the track's unique identifier
     * @param dto the request body containing the fields to update
     * @return 200 OK with the updated track, or 404 if not found
     */
    @PatchMapping("/{id}")
    public TrackResponseDTO patch(@PathVariable Long id, @Valid @RequestBody TrackPatchRequestDTO dto) {
        return trackService.patchTrack(id, dto);
    }

    /**
     * Deletes a track by its ID.
     *
     * @param id the track's unique identifier
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trackService.deleteTrack(id);
        return ResponseEntity.noContent().build();
    }
}
