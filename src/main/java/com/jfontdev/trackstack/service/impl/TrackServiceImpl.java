package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.track.TrackPatchRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackPageMetadataDTO;
import com.jfontdev.trackstack.dto.track.TrackPageResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackUpdateRequestDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.repository.TrackSpecifications;
import com.jfontdev.trackstack.service.AudioMetadataScanner;
import com.jfontdev.trackstack.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the {@link TrackService} interface.
 * <p>
 * Handles business logic for managing {@link Track} entities.
 * <p>
 * <b>Caching Strategy:</b>
 * - Read operation {@code getTrackById} is cached under the "tracks" cache.
 * - The pageable list operation {@code getAllTracks} is cached using a
 * cache-safe DTO response.
 * - Write operations evict the entire "tracks" cache to ensure consistency.
 */
@Service
public class TrackServiceImpl implements TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "artist", "album", "bpm", "key", "durationSeconds",
            "genre", "filePath", "fileFormat", "bitrate", "energy", "playCount",
            "lastPlayedDate", "addedDate");

    private final TrackRepository trackRepository;
    private final AudioMetadataScanner audioMetadataScanner;

    public TrackServiceImpl(TrackRepository trackRepository, AudioMetadataScanner audioMetadataScanner) {
        this.trackRepository = trackRepository;
        this.audioMetadataScanner = audioMetadataScanner;
    }

    @Override
    @CacheEvict(value = "tracks", allEntries = true)
    @Transactional
    public TrackResponseDTO createTrack(TrackRequestDTO dto) {
        log.info("Evicting 'tracks' cache. Creating new track: {}", dto.title());
        Track track = Track.create(
                dto.title(),
                dto.artist(),
                dto.album(),
                dto.bpm(),
                dto.key(),
                dto.durationSeconds(),
                dto.genre(),
                dto.filePath(),
                dto.fileFormat(),
                dto.bitrate(),
                dto.energy());

        Track savedTrack = trackRepository.saveAndFlush(track);
        return mapToResponseDTO(savedTrack);
    }

    @Override
    @Cacheable(value = "tracks", key = "#id")
    @Transactional(readOnly = true)
    public TrackResponseDTO getTrackById(Long id) {
        log.info("Cache miss for 'tracks' with id: {}. Fetching from database.", id);
        Track foundTrack = findTrackOrThrow(id);
        return mapToResponseDTO(foundTrack);
    }

    @Override
    @Cacheable(value = "tracks", key = "'list|page=' + #page + '|size=' + #size + '|sort=' + #sort + '|bpmMin=' + #bpmMin + '|bpmMax=' + #bpmMax + '|musicalKey=' + #musicalKey + '|genre=' + #genre")
    @Transactional(readOnly = true)
    public TrackPageResponseDTO getAllTracks(int page, int size, String sort,
            Double bpmMin, Double bpmMax, String musicalKey, String genre) {
        validatePageRequest(page, size);
        validateBpmRange(bpmMin, bpmMax);

        Sort parsedSort = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, parsedSort);

        String normalizedKey = normalizeFilterValue(musicalKey);
        String normalizedGenre = normalizeFilterValue(genre);

        log.info(
                "Cache miss for 'tracks' list query (page={}, size={}, sort={}, bpmMin={}, bpmMax={}, key={}, genre={}).",
                page, size, sort, bpmMin, bpmMax, normalizedKey, normalizedGenre);

        Specification<Track> specification = Specification.unrestricted();

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

        Page<Track> tracksPage = trackRepository.findAll(specification, pageable);

        List<TrackResponseDTO> content = tracksPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .toList();

        TrackPageMetadataDTO pageMetadata = new TrackPageMetadataDTO(
                tracksPage.getSize(),
                tracksPage.getNumber(),
                tracksPage.getTotalElements(),
                tracksPage.getTotalPages());

        return new TrackPageResponseDTO(content, pageMetadata);
    }

    @Override
    @CacheEvict(value = "tracks", allEntries = true)
    @Transactional
    public int scanLibrary() {
        log.info("Starting library scan...");
        List<Track> scannedTracks = audioMetadataScanner.scanDirectory();
        int count = 0;
        for (Track track : scannedTracks) {
            if (!trackRepository.existsByFilePath(track.getFilePath())) {
                trackRepository.save(track);
                count++;
            }
        }
        trackRepository.flush();
        log.info("Library scan complete. Imported {} new tracks.", count);
        return count;
    }

    @Override
    @CacheEvict(value = "tracks", allEntries = true)
    @Transactional
    public TrackResponseDTO updateTrack(Long id, TrackUpdateRequestDTO dto) {
        log.info("Evicting 'tracks' cache. Updating track with id: {}", id);
        Track track = findTrackOrThrow(id);

        track.update(
                dto.title(),
                dto.artist(),
                dto.album(),
                dto.bpm(),
                dto.key(),
                dto.durationSeconds(),
                dto.genre(),
                dto.filePath(),
                dto.fileFormat(),
                dto.bitrate(),
                dto.energy());

        Track savedTrack = trackRepository.saveAndFlush(track);
        return mapToResponseDTO(savedTrack);
    }

    @Override
    @CacheEvict(value = "tracks", allEntries = true)
    @Transactional
    public TrackResponseDTO patchTrack(Long id, TrackPatchRequestDTO dto) {
        log.info("Evicting 'tracks' cache. Patching track with id: {}", id);
        Track track = findTrackOrThrow(id);

        String title = dto.title() != null ? dto.title() : track.getTitle();
        String artist = dto.artist() != null ? dto.artist() : track.getArtist();
        String album = dto.album() != null ? dto.album() : track.getAlbum();
        Double bpm = dto.bpm() != null ? dto.bpm() : track.getBpm();
        String key = dto.key() != null ? dto.key() : track.getKey();
        Integer durationSeconds = dto.durationSeconds() != null ? dto.durationSeconds() : track.getDurationSeconds();
        String genre = dto.genre() != null ? dto.genre() : track.getGenre();
        String filePath = dto.filePath() != null ? dto.filePath() : track.getFilePath();
        String fileFormat = dto.fileFormat() != null ? dto.fileFormat() : track.getFileFormat();
        Integer bitrate = dto.bitrate() != null ? dto.bitrate() : track.getBitrate();
        Integer energy = dto.energy() != null ? dto.energy() : track.getEnergy();

        track.update(title, artist, album, bpm, key, durationSeconds, genre,
                filePath, fileFormat, bitrate, energy);

        Track savedTrack = trackRepository.saveAndFlush(track);
        return mapToResponseDTO(savedTrack);
    }

    @Override
    @CacheEvict(value = "tracks", allEntries = true)
    @Transactional
    public void deleteTrack(Long id) {
        log.info("Evicting 'tracks' cache. Deleting track with id: {}", id);
        Track track = findTrackOrThrow(id);
        trackRepository.delete(track);
    }

    // --- Validation helpers ---

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
    }

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

    private String normalizeFilterValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Track findTrackOrThrow(Long id) {
        Optional<Track> track = trackRepository.findById(id);
        if (track.isEmpty()) {
            throw new NotFoundException("Track not found");
        }
        return track.get();
    }

    private TrackResponseDTO mapToResponseDTO(Track track) {
        return new TrackResponseDTO(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getAlbum(),
                track.getBpm(),
                track.getKey(),
                track.getDurationSeconds(),
                track.getGenre(),
                track.getFilePath(),
                track.getFileFormat(),
                track.getBitrate(),
                track.getEnergy(),
                track.getPlayCount(),
                track.getLastPlayedDate(),
                track.getAddedDate());
    }
}
