package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;

import java.util.List;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Playlist}
 * entities.
 * <p>
 * This interface defines the contract for playlist-related business operations.
 * By using an interface, we decouple the controller from the specific
 * implementation,
 * making the code easier to test and maintain.
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
}