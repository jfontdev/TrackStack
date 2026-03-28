package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.track.TrackPatchRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackUpdateRequestDTO;
import com.jfontdev.trackstack.service.TrackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing tracks.
 * <p>
 * Provides endpoints for full CRUD operations on tracks, as well as
 * tag relationship management. This controller delegates all business
 * logic to the {@link TrackService} and only handles HTTP concerns
 * (request binding, status codes, response formatting).
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
     * Retrieves all tracks.
     *
     * @return 200 OK with a list of all tracks
     */
    @GetMapping
    public List<TrackResponseDTO> getAll() {
        return trackService.getAllTracks();
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
     * <p>
     * Also removes all tag associations and playlist memberships for this track
     * (handled by ON DELETE CASCADE at the database level).
     *
     * @param id the track's unique identifier
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trackService.deleteTrack(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Associates a tag with a track.
     *
     * @param id    the track's unique identifier
     * @param tagId the tag's unique identifier
     * @return 200 OK with the updated track (including the new tag), or 404 if
     *         either the track or tag is not found
     */
    @PutMapping("/{id}/tags/{tagId}")
    public TrackResponseDTO addTag(@PathVariable Long id, @PathVariable Long tagId) {
        return trackService.addTagToTrack(id, tagId);
    }

    /**
     * Removes a tag association from a track.
     *
     * @param id    the track's unique identifier
     * @param tagId the tag's unique identifier
     * @return 200 OK with the updated track (without the removed tag), or 404 if
     *         either the track or tag is not found
     */
    @DeleteMapping("/{id}/tags/{tagId}")
    public TrackResponseDTO removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        return trackService.removeTagFromTrack(id, tagId);
    }
}
