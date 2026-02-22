package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;

import java.util.List;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Track}
 * entities.
 * <p>
 * This interface defines the contract for track-related business operations.
 * By using an interface, we decouple the controller from the specific
 * implementation,
 * making the code easier to test and maintain.
 */
public interface TrackService {

    /**
     * Creates a new track based on the provided request data.
     *
     * @param dto the data transfer object containing the track details
     * @return a response DTO containing the newly created track's details
     */
    TrackResponseDTO createTrack(TrackRequestDTO dto);

    /**
     * Retrieves a track by its unique identifier.
     *
     * @param id the unique identifier of the track
     * @return a response DTO containing the track's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the track is
     *                                                             not found
     */
    TrackResponseDTO getTrackById(Long id);

    /**
     * Retrieves all tracks in the system.
     *
     * @return a list of response DTOs representing all tracks
     */
    List<TrackResponseDTO> getAllTracks();
}
