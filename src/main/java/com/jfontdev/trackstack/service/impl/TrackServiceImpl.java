package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link TrackService} interface.
 * <p>
 * This service handles the business logic for managing {@link Track} entities.
 * It acts as a bridge between the controller layer (which handles HTTP
 * requests)
 * and the repository layer (which handles database operations).
 * <p>
 * <b>Caching Strategy:</b>
 * We use Spring's caching abstraction to improve read performance.
 * - Read operations ({@code getTrackById}, {@code getAllTracks}) are cached
 * under the "tracks" cache.
 * - Write operations ({@code createTrack}) evict the entire "tracks" cache to
 * ensure
 * that subsequent reads (especially {@code getAllTracks}) do not return stale
 * data.
 */
@Service
public class TrackServiceImpl implements TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackServiceImpl.class);

    private final TrackRepository trackRepository;

    /**
     * Constructs a new {@code TrackServiceImpl} with the required repository.
     * We use constructor injection to ensure the repository is provided and
     * immutable.
     *
     * @param trackRepository the repository used for database operations on tracks
     */
    public TrackServiceImpl(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    /**
     * Creates a new track in the system.
     * <p>
     * This method maps the incoming DTO to a domain entity, saves it to the
     * database,
     * and then maps the saved entity back to a response DTO.
     * <p>
     * <b>Cache Eviction:</b> We evict all entries in the "tracks" cache because
     * adding
     * a new track invalidates the result of {@code getAllTracks()}.
     *
     * @param dto the data transfer object containing the details of the track to
     *            create
     * @return a response DTO containing the saved track's details, including its
     *         generated ID
     */

    @Override
    @CacheEvict(value = "tracks", allEntries = true)
    public TrackResponseDTO createTrack(TrackRequestDTO dto) {
        log.info("Evicting 'tracks' cache. Creating new track: {}", dto.title());
        Track track = Track.create(
                dto.title(),
                dto.artist(),
                dto.bpm(),
                dto.key(),
                dto.duration());

        Track savedTrack = trackRepository.saveAndFlush(track);

        return new TrackResponseDTO(
                savedTrack.getId(),
                savedTrack.getTitle(),
                savedTrack.getArtist(),
                savedTrack.getBpm(),
                savedTrack.getKey(),
                savedTrack.getDuration());
    }

    /**
     * Retrieves a track by its unique identifier.
     * <p>
     * <b>Caching:</b> The result of this method is cached. If the track is
     * requested again
     * with the same ID, the cached value is returned instead of querying the
     * database.
     *
     * @param id the unique identifier of the track to retrieve
     * @return a response DTO containing the track's details
     * @throws NotFoundException if no track is found with the provided ID
     */
    @Override
    @Cacheable(value = "tracks", key = "#id")
    public TrackResponseDTO getTrackById(Long id) {
        log.info("Cache miss for 'tracks' with id: {}. Fetching from database.", id);
        Optional<Track> track = trackRepository.findById(id);

        if (track.isEmpty()) {
            throw new NotFoundException("Track not found");
        }

        Track foundTrack = track.get();

        return new TrackResponseDTO(
                foundTrack.getId(),
                foundTrack.getTitle(),
                foundTrack.getArtist(),
                foundTrack.getBpm(),
                foundTrack.getKey(),
                foundTrack.getDuration());
    }

    /**
     * Retrieves all tracks currently stored in the system.
     * <p>
     * <b>Caching:</b> The entire list of tracks is cached. This is highly efficient
     * for
     * read-heavy workloads, but requires careful eviction (clearing the cache)
     * whenever
     * a track is added, updated, or deleted to prevent stale data.
     *
     * @return a list of response DTOs representing all tracks
     */
    @Override
    @Cacheable(value = "tracks")
    public List<TrackResponseDTO> getAllTracks() {
        log.info("Cache miss for 'tracks' list. Fetching all tracks from database.");
        return trackRepository.findAll().stream().map(track -> new TrackResponseDTO(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getBpm(),
                track.getKey(),
                track.getDuration())).toList();
    }
}
