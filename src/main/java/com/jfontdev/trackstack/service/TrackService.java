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
 * including full CRUD, partial updates, and tag relationship management.
 * By using an interface, we decouple the controller from the specific
 * implementation, making the code easier to test and maintain.
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
     * <p>
     * This method powers the Phase 07 list endpoint and supports production-ready
     * query patterns without exposing persistence details to the controller.
     * </p>
     *
     * @param page       the zero-based page index
     * @param size       the page size
     * @param sort       sort expression in the format "field,direction"
     *                   (example: "title,asc")
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
     * Fully updates an existing track (PUT semantics).
     * <p>
     * All fields are replaced with the values from the request DTO.
     * Fields that are nullable on the entity (bpm, key) are set to null
     * if not provided.
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
     * <p>
     * Only non-null fields from the request DTO are applied to the existing
     * entity. Fields that are null in the DTO retain their current values.
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
     * <p>
     * The track is removed from the database along with all its join table
     * associations (tag relationships and playlist memberships) thanks to
     * ON DELETE CASCADE on the foreign keys.
     *
     * @param id the unique identifier of the track to delete
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the track is
     *                                                             not found
     */
    void deleteTrack(Long id);

    /**
     * Associates a tag with a track.
     * <p>
     * If the tag is already associated with the track, this operation is
     * idempotent (no error is thrown, the association simply remains).
     *
     * @param trackId the unique identifier of the track
     * @param tagId   the unique identifier of the tag to add
     * @return a response DTO containing the updated track's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the track or
     *                                                             tag is not found
     */
    TrackResponseDTO addTagToTrack(Long trackId, Long tagId);

    /**
     * Removes a tag association from a track.
     * <p>
     * If the tag is not currently associated with the track, this operation is
     * idempotent (no error is thrown).
     *
     * @param trackId the unique identifier of the track
     * @param tagId   the unique identifier of the tag to remove
     * @return a response DTO containing the updated track's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the track or
     *                                                             tag is not found
     */
    TrackResponseDTO removeTagFromTrack(Long trackId, Long tagId);
}
