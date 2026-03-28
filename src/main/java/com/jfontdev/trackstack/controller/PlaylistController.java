package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.playlist.PlaylistPatchRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistUpdateRequestDTO;
import com.jfontdev.trackstack.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing playlists.
 * <p>
 * Provides endpoints for full CRUD operations on playlists, as well as
 * track relationship management. This controller delegates all business
 * logic to the {@link PlaylistService} and only handles HTTP concerns
 * (request binding, status codes, response formatting).
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    /**
     * Constructs a new {@code PlaylistController} with the required service.
     *
     * @param playlistService the service handling playlist business logic
     */
    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    /**
     * Creates a new playlist.
     *
     * @param dto the validated request body containing playlist details
     * @return 201 Created with the newly created playlist
     */
    @PostMapping
    public ResponseEntity<PlaylistResponseDTO> create(@Valid @RequestBody PlaylistRequestDTO dto) {
        PlaylistResponseDTO response = playlistService.createPlaylist(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a playlist by its ID.
     *
     * @param id the playlist's unique identifier
     * @return 200 OK with the playlist details (including tracks), or 404 if not found
     */
    @GetMapping("/{id}")
    public PlaylistResponseDTO getById(@PathVariable Long id) {
        return playlistService.getPlaylistById(id);
    }

    /**
     * Retrieves all playlists.
     *
     * @return 200 OK with a list of all playlists (each including their tracks)
     */
    @GetMapping
    public List<PlaylistResponseDTO> getAll() {
        return playlistService.getAllPlaylists();
    }

    /**
     * Fully updates an existing playlist (PUT semantics).
     * <p>
     * All fields in the request body replace the existing values.
     *
     * @param id  the playlist's unique identifier
     * @param dto the validated request body containing the new playlist details
     * @return 200 OK with the updated playlist, or 404 if not found
     */
    @PutMapping("/{id}")
    public PlaylistResponseDTO update(@PathVariable Long id, @Valid @RequestBody PlaylistUpdateRequestDTO dto) {
        return playlistService.updatePlaylist(id, dto);
    }

    /**
     * Partially updates an existing playlist (PATCH semantics).
     * <p>
     * Only non-null fields in the request body are applied to the existing playlist.
     *
     * @param id  the playlist's unique identifier
     * @param dto the request body containing the fields to update
     * @return 200 OK with the updated playlist, or 404 if not found
     */
    @PatchMapping("/{id}")
    public PlaylistResponseDTO patch(@PathVariable Long id, @RequestBody PlaylistPatchRequestDTO dto) {
        return playlistService.patchPlaylist(id, dto);
    }

    /**
     * Deletes a playlist by its ID.
     * <p>
     * The tracks themselves are not deleted -- only the playlist and its
     * track associations are removed (handled by ON DELETE CASCADE at the
     * database level).
     *
     * @param id the playlist's unique identifier
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a track to a playlist.
     *
     * @param id      the playlist's unique identifier
     * @param trackId the track's unique identifier
     * @return 200 OK with the updated playlist (including the new track), or 404 if
     *         either the playlist or track is not found
     */
    @PutMapping("/{id}/tracks/{trackId}")
    public PlaylistResponseDTO addTrack(@PathVariable Long id, @PathVariable Long trackId) {
        return playlistService.addTrackToPlaylist(id, trackId);
    }

    /**
     * Removes a track from a playlist.
     *
     * @param id      the playlist's unique identifier
     * @param trackId the track's unique identifier
     * @return 200 OK with the updated playlist (without the removed track), or 404 if
     *         either the playlist or track is not found
     */
    @DeleteMapping("/{id}/tracks/{trackId}")
    public PlaylistResponseDTO removeTrack(@PathVariable Long id, @PathVariable Long trackId) {
        return playlistService.removeTrackFromPlaylist(id, trackId);
    }
}
