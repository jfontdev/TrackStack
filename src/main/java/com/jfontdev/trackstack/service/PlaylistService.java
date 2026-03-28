package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.playlist.PlaylistPatchRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistUpdateRequestDTO;

import java.util.List;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Playlist}
 * entities.
 * <p>
 * This interface defines the contract for playlist-related business operations,
 * including full CRUD, partial updates, and track relationship management.
 * By using an interface, we decouple the controller from the specific
 * implementation, making the code easier to test and maintain.
 */
public interface PlaylistService {

    /**
     * Creates a new playlist based on the provided request data.
     *
     * @param dto the data transfer object containing the playlist details
     * @return a response DTO containing the newly created playlist's details
     */
    PlaylistResponseDTO createPlaylist(PlaylistRequestDTO dto);

    /**
     * Retrieves a playlist by its unique identifier.
     *
     * @param id the unique identifier of the playlist
     * @return a response DTO containing the playlist's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the playlist
     *                                                             is not found
     */
    PlaylistResponseDTO getPlaylistById(Long id);

    /**
     * Retrieves all playlists in the system.
     *
     * @return a list of response DTOs representing all playlists
     */
    List<PlaylistResponseDTO> getAllPlaylists();

    /**
     * Fully updates an existing playlist (PUT semantics).
     * <p>
     * All fields are replaced with the values from the request DTO.
     * The description field is nullable, so omitting it clears the description.
     *
     * @param id  the unique identifier of the playlist to update
     * @param dto the data transfer object containing the new playlist details
     * @return a response DTO containing the updated playlist's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the playlist
     *                                                             is not found
     */
    PlaylistResponseDTO updatePlaylist(Long id, PlaylistUpdateRequestDTO dto);

    /**
     * Partially updates an existing playlist (PATCH semantics).
     * <p>
     * Only non-null fields from the request DTO are applied. Fields that are
     * null in the DTO retain their current values.
     *
     * @param id  the unique identifier of the playlist to patch
     * @param dto the data transfer object containing the fields to update
     * @return a response DTO containing the updated playlist's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the playlist
     *                                                             is not found
     */
    PlaylistResponseDTO patchPlaylist(Long id, PlaylistPatchRequestDTO dto);

    /**
     * Deletes a playlist by its unique identifier.
     * <p>
     * The playlist is removed from the database along with all its join table
     * associations (track relationships) thanks to ON DELETE CASCADE on the
     * foreign keys. The tracks themselves are not deleted.
     *
     * @param id the unique identifier of the playlist to delete
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the playlist
     *                                                             is not found
     */
    void deletePlaylist(Long id);

    /**
     * Adds a track to a playlist.
     * <p>
     * If the track is already in the playlist, this operation is idempotent
     * (no error is thrown, the association simply remains).
     *
     * @param playlistId the unique identifier of the playlist
     * @param trackId    the unique identifier of the track to add
     * @return a response DTO containing the updated playlist's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the playlist
     *                                                             or track is not found
     */
    PlaylistResponseDTO addTrackToPlaylist(Long playlistId, Long trackId);

    /**
     * Removes a track from a playlist.
     * <p>
     * If the track is not currently in the playlist, this operation is
     * idempotent (no error is thrown).
     *
     * @param playlistId the unique identifier of the playlist
     * @param trackId    the unique identifier of the track to remove
     * @return a response DTO containing the updated playlist's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the playlist
     *                                                             or track is not found
     */
    PlaylistResponseDTO removeTrackFromPlaylist(Long playlistId, Long trackId);
}
