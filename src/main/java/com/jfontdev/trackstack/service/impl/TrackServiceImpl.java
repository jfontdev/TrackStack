package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackPatchRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackUpdateRequestDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Tag;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.repository.TagRepository;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
 * - Write operations (create, update, patch, delete, and relationship changes)
 * evict the entire "tracks" cache to ensure that subsequent reads do not
 * return stale data.
 * <p>
 * <b>Transaction Strategy:</b>
 * All write operations are annotated with {@code @Transactional} (read-write)
 * to ensure proper rollback on failure. Read operations such as
 * {@code getTrackById} and {@code getAllTracks} are explicitly annotated with
 * {@code @Transactional(readOnly = true)} to clearly mark them as read-only
 * and to integrate cleanly with Spring's transaction management.
 */
@Service
public class TrackServiceImpl implements TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackServiceImpl.class);

    private final TrackRepository trackRepository;
    private final TagRepository tagRepository;

    /**
     * Constructs a new {@code TrackServiceImpl} with the required repositories.
     * <p>
     * We inject both {@link TrackRepository} and {@link TagRepository} because
     * this service manages the Track-Tag relationship (the owning side).
     *
     * @param trackRepository the repository used for database operations on tracks
     * @param tagRepository   the repository used to look up tags for relationship
     *                        management
     */
    public TrackServiceImpl(TrackRepository trackRepository, TagRepository tagRepository) {
        this.trackRepository = trackRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> We evict all entries in the "tracks" cache because
     * adding a new track invalidates the result of {@code getAllTracks()}.
     */
    @Override
    @CacheEvict(value = "tracks", allEntries = true)
    @Transactional
    public TrackResponseDTO createTrack(TrackRequestDTO dto) {
        log.info("Evicting 'tracks' cache. Creating new track: {}", dto.title());
        Track track = Track.create(
                dto.title(),
                dto.artist(),
                dto.bpm(),
                dto.key(),
                dto.duration());

        Track savedTrack = trackRepository.saveAndFlush(track);

        return mapToResponseDTO(savedTrack);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Caching:</b> The result of this method is cached. If the track is
     * requested again with the same ID, the cached value is returned instead
     * of querying the database.
     */
    @Override
    @Cacheable(value = "tracks", key = "#id")
    @Transactional(readOnly = true)
    public TrackResponseDTO getTrackById(Long id) {
        log.info("Cache miss for 'tracks' with id: {}. Fetching from database.", id);
        Track foundTrack = findTrackOrThrow(id);

        return mapToResponseDTO(foundTrack);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Caching:</b> The entire list of tracks is cached. This is highly efficient
     * for read-heavy workloads, but requires careful eviction whenever a track
     * is added, updated, or deleted to prevent stale data.
     */
    @Override
    @Cacheable(value = "tracks")
    @Transactional(readOnly = true)
    public List<TrackResponseDTO> getAllTracks() {
        log.info("Cache miss for 'tracks' list. Fetching all tracks from database.");
        return trackRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tracks" cache because
     * updating a track invalidates both the individual entry and the list. It also
     * evicts the "playlists" cache because tracked changes affect playlist
     * responses.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public TrackResponseDTO updateTrack(Long id, TrackUpdateRequestDTO dto) {
        log.info("Evicting 'tracks' and 'playlists' caches. Updating track with id: {}", id);
        Track track = findTrackOrThrow(id);

        track.update(dto.title(), dto.artist(), dto.bpm(), dto.key(), dto.duration());
        Track savedTrack = trackRepository.saveAndFlush(track);

        return mapToResponseDTO(savedTrack);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Merges non-null fields from the patch DTO with the existing entity's values,
     * then delegates to the entity's {@code update} method.
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tracks" cache. It also
     * evicts the "playlists" cache because tracked changes affect playlist
     * responses.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public TrackResponseDTO patchTrack(Long id, TrackPatchRequestDTO dto) {
        log.info("Evicting 'tracks' and 'playlists' caches. Patching track with id: {}", id);
        Track track = findTrackOrThrow(id);

        String title = dto.title() != null ? dto.title() : track.getTitle();
        String artist = dto.artist() != null ? dto.artist() : track.getArtist();
        Double bpm = dto.bpm() != null ? dto.bpm() : track.getBpm();
        String key = dto.key() != null ? dto.key() : track.getKey();
        String duration = dto.duration() != null ? dto.duration() : track.getDuration();

        track.update(title, artist, bpm, key, duration);
        Track savedTrack = trackRepository.saveAndFlush(track);

        return mapToResponseDTO(savedTrack);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tracks" cache and
     * "playlists"
     * cache because deleting a track invalidates both the tracks list cache
     * and any playlist that contained this track.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public void deleteTrack(Long id) {
        log.info("Evicting 'tracks' and 'playlists' caches. Deleting track with id: {}", id);
        Track track = findTrackOrThrow(id);

        trackRepository.delete(track);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tracks" cache because
     * changing a track's tags invalidates cached track representations. It also
     * evicts the "playlists" cache because playlists include track tags in their
     * representation.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public TrackResponseDTO addTagToTrack(Long trackId, Long tagId) {
        log.info("Evicting 'tracks' and 'playlists' caches. Adding tag {} to track {}", tagId, trackId);
        Track track = findTrackOrThrow(trackId);
        Tag tag = findTagOrThrow(tagId);

        track.addTag(tag);
        Track savedTrack = trackRepository.saveAndFlush(track);

        return mapToResponseDTO(savedTrack);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tracks" cache because
     * changing a track's tags invalidates cached track representations. It also
     * evicts the "playlists" cache because playlists include track tags in their
     * representation.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public TrackResponseDTO removeTagFromTrack(Long trackId, Long tagId) {
        log.info("Evicting 'tracks' and 'playlists' caches. Removing tag {} from track {}", tagId, trackId);
        Track track = findTrackOrThrow(trackId);
        Tag tag = findTagOrThrow(tagId);

        track.removeTag(tag);
        Track savedTrack = trackRepository.saveAndFlush(track);

        return mapToResponseDTO(savedTrack);
    }

    /**
     * Finds a track by ID or throws a {@link NotFoundException}.
     * <p>
     * This is an internal helper that centralizes the Optional handling
     * pattern used across all methods that require an existing track.
     *
     * @param id the track ID to look up
     * @return the found Track entity
     * @throws NotFoundException if no track exists with the given ID
     */
    private Track findTrackOrThrow(Long id) {
        Optional<Track> track = trackRepository.findById(id);

        if (track.isEmpty()) {
            throw new NotFoundException("Track not found");
        }

        return track.get();
    }

    /**
     * Finds a tag by ID or throws a {@link NotFoundException}.
     * <p>
     * Used by relationship management methods that need to look up tags.
     *
     * @param id the tag ID to look up
     * @return the found Tag entity
     * @throws NotFoundException if no tag exists with the given ID
     */
    private Tag findTagOrThrow(Long id) {
        Optional<Tag> tag = tagRepository.findById(id);

        if (tag.isEmpty()) {
            throw new NotFoundException("Tag not found.");
        }

        return tag.get();
    }

    /**
     * Maps a {@link Track} entity to a {@link TrackResponseDTO}.
     * <p>
     * This centralizes the entity-to-DTO mapping logic to avoid repetition
     * across service methods. The mapping includes the track's associated tags.
     * The tags are sorted by name to guarantee deterministic API responses
     * despite the underlying set's undefined iteration order.
     *
     * @param track the entity to map
     * @return the corresponding response DTO
     */
    private TrackResponseDTO mapToResponseDTO(Track track) {
        List<TagResponseDTO> tagDTOs = track.getTags().stream()
                .map(tag -> new TagResponseDTO(tag.getId(), tag.getName()))
                .sorted(Comparator.comparing(TagResponseDTO::name))
                .toList();

        return new TrackResponseDTO(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getBpm(),
                track.getKey(),
                track.getDuration(),
                tagDTOs);
    }
}
