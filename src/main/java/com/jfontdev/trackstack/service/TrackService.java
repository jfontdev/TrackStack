package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.track.TrackPatchRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackPageResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackUpdateRequestDTO;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Track}
 * entities.
 * <p>
 * This interface defines the contract for track-related business operations,
 * including full CRUD, partial updates, and audio library scanning.
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
     * Retrieves tracks using pagination, sorting, and optional filters.
     *
     * @param page       the zero-based page index
     * @param size       the page size
     * @param sort       sort expression in the format "field,direction"
     * @param bpmMin     optional minimum BPM (inclusive)
     * @param bpmMax     optional maximum BPM (inclusive)
     * @param musicalKey optional exact musical key filter
     * @param genre      optional exact genre filter
     * @return a cache-safe paginated response containing tracks and page metadata
     */
    TrackPageResponseDTO getAllTracks(int page,
            int size,
            String sort,
            Double bpmMin,
            Double bpmMax,
            String musicalKey,
            String genre);

    /**
     * Scans the configured music directory and imports new tracks.
     *
     * @return the number of tracks imported
     */
    int scanLibrary();

    /**
     * Fully updates an existing track (PUT semantics).
     *
     * @param id  the unique identifier of the track to update
     * @param dto the data transfer object containing the new track details
     * @return a response DTO containing the updated track's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the track is
     *                                                             not found
     */
    TrackResponseDTO updateTrack(Long id, TrackUpdateRequestDTO dto);

    /**
     * Partially updates an existing track (PATCH semantics).
     *
     * @param id  the unique identifier of the track to patch
     * @param dto the data transfer object containing the fields to update
     * @return a response DTO containing the updated track's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the track is
     *                                                             not found
     */
    TrackResponseDTO patchTrack(Long id, TrackPatchRequestDTO dto);

    /**
     * Deletes a track by its unique identifier.
     *
     * @param id the unique identifier of the track to delete
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the track is
     *                                                             not found
     */
    void deleteTrack(Long id);
}
