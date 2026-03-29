package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackPatchRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackPageMetadataDTO;
import com.jfontdev.trackstack.dto.track.TrackPageResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackUpdateRequestDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Tag;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.repository.TagRepository;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.repository.TrackSpecifications;
import com.jfontdev.trackstack.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
 * - Read operation {@code getTrackById} is cached under the "tracks" cache.
 * - The pageable list operation {@code getAllTracks} is cached using a
 * cache-safe DTO response that avoids direct {@code PageImpl} serialization.
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
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title", "artist", "bpm", "key", "duration",
            "genre");

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
                dto.duration(),
                dto.genre());

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
     * <b>Caching:</b> The result of this method is cached using a cache-safe DTO
     * response that includes both the track list and pagination metadata. The cache
     * key is constructed from all query parameters to ensure that different filter
     * combinations are cached separately. This avoids issues with directly caching
     * {@code PageImpl} objects, which can cause serialization problems and cache
     * pollution due to their internal state and non-deterministic nature.
     * <p>
     * The method includes validation for paging parameters and BPM filters to
     * ensure
     * that only valid queries are processed, which also helps maintain cache
     * integrity by preventing caching of invalid requests.
     * <p>
     */
    @Override
    @Cacheable(value = "tracks", key = "'list-v2|page=' + #page + '|size=' + #size + '|sort=' + #sort + '|bpmMin=' + #bpmMin + '|bpmMax=' + #bpmMax + '|musicalKey=' + #musicalKey + '|genre=' + #genre")
    @Transactional(readOnly = true)
    public TrackPageResponseDTO getAllTracks(int page,
            int size,
            String sort,
            Double bpmMin,
            Double bpmMax,
            String musicalKey,
            String genre) {
        // We perform validation on paging parameters and BPM filters to ensure that
        // only valid queries are processed and cached.
        validatePageRequest(page, size);
        validateBpmRange(bpmMin, bpmMax);

        // We parse and validate the sort parameter to ensure it conforms to expected
        Sort parsedSort = parseSort(sort);

        // We normalize optional string filters to trim whitespace and treat empty
        // values as null
        Pageable pageable = PageRequest.of(page, size, parsedSort);

        // Normalizing filter values ensures consistent cache keys and avoids issues
        // with leading/trailing whitespace.
        String normalizedKey = normalizeFilterValue(musicalKey);
        String normalizedGenre = normalizeFilterValue(genre);

        log.info(
                "Cache miss for 'tracks' list query (page={}, size={}, sort={}, bpmMin={}, bpmMax={}, key={}, genre={}). Fetching from database.",
                page,
                size,
                sort,
                bpmMin,
                bpmMax,
                normalizedKey,
                normalizedGenre);

        Specification<Track> specification = Specification.unrestricted();

        // We only add specifications for filters that are provided to avoid unnecessary
        // predicates that could impact query performance.
        if (bpmMin != null) {
            specification = specification.and(TrackSpecifications.hasBpmGreaterThanOrEqualTo(bpmMin));
        }

        if (bpmMax != null) {
            specification = specification.and(TrackSpecifications.hasBpmLessThanOrEqualTo(bpmMax));
        }

        if (normalizedKey != null) {
            specification = specification.and(TrackSpecifications.hasMusicalKey(normalizedKey));
        }

        if (normalizedGenre != null) {
            specification = specification.and(TrackSpecifications.hasGenre(normalizedGenre));
        }

        // We execute the query with the constructed specification and pageable. The
        // result is a Page of Track entities, which we then map to a list of
        // TrackResponseDTOs. We also construct a TrackPageMetadataDTO to include
        // pagination information in the response. This approach allows us to return a
        // cache-safe DTO response that includes both the data and metadata without
        // exposing internal PageImpl details.
        Page<Track> tracksPage = trackRepository.findAll(specification, pageable);

        // We map the Track entities to TrackResponseDTOs. This mapping is done
        // in-memory after fetching the data, which is acceptable for the page size
        // limits we have in place. It also allows us to keep the caching layer simple
        // by caching the final DTO response rather than trying to cache complex JPA
        // Page objects.
        List<TrackResponseDTO> content = tracksPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .toList();

        // We construct the pagination metadata DTO from the Page object. This includes
        // the page size, current page number, total elements, and total pages. This
        // metadata is essential for clients to understand the pagination context of the
        // response and to navigate through pages effectively.
        TrackPageMetadataDTO pageMetadata = new TrackPageMetadataDTO(
                tracksPage.getSize(),
                tracksPage.getNumber(),
                tracksPage.getTotalElements(),
                tracksPage.getTotalPages());

        return new TrackPageResponseDTO(content, pageMetadata);
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

        track.update(dto.title(), dto.artist(), dto.bpm(), dto.key(), dto.duration(), dto.genre());
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
        String genre = dto.genre() != null ? dto.genre() : track.getGenre();

        track.update(title, artist, bpm, key, duration, genre);
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
     * Validates the requested page index and page size values.
     *
     * @param page the zero-based page index
     * @param size the page size
     * @throws IllegalArgumentException if paging values are outside accepted bounds
     */
    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
    }

    /**
     * Validates BPM filter values and range ordering.
     *
     * @param bpmMin the minimum BPM filter (inclusive)
     * @param bpmMax the maximum BPM filter (inclusive)
     * @throws IllegalArgumentException if values are non-positive or range is
     *                                  invalid
     */
    private void validateBpmRange(Double bpmMin, Double bpmMax) {
        if (bpmMin != null && bpmMin <= 0) {
            throw new IllegalArgumentException("bpmMin must be positive.");
        }

        if (bpmMax != null && bpmMax <= 0) {
            throw new IllegalArgumentException("bpmMax must be positive.");
        }

        if (bpmMin != null && bpmMax != null && bpmMin > bpmMax) {
            throw new IllegalArgumentException("bpmMin must be less than or equal to bpmMax.");
        }
    }

    /**
     * Parses and validates a sort expression in {@code field,direction} format.
     *
     * @param sort the sort expression
     * @return a validated {@link Sort} instance
     * @throws IllegalArgumentException if sort input is malformed or unsupported
     */
    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.ASC, "title");
        }

        String[] sortParts = sort.split(",");
        if (sortParts.length != 2) {
            throw new IllegalArgumentException("Sort must use the format field,direction.");
        }

        String sortField = sortParts[0].trim().toLowerCase();
        String sortDirection = sortParts[1].trim().toLowerCase();

        if (!StringUtils.hasText(sortField)) {
            throw new IllegalArgumentException("Sort field must not be empty.");
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortField + ".");
        }

        Sort.Direction direction;
        if ("asc".equals(sortDirection)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equals(sortDirection)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }

        return Sort.by(direction, sortField);
    }

    /**
     * Normalizes optional string filters by trimming whitespace.
     *
     * @param value the raw filter value from request parameters
     * @return trimmed value, or {@code null} when empty
     */
    private String normalizeFilterValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
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
                track.getGenre(),
                tagDTOs);
    }
}
