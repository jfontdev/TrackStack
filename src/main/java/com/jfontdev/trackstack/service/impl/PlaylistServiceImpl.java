package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistPatchRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistUpdateRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Playlist;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.repository.PlaylistRepository;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link PlaylistService} interface.
 * <p>
 * This service handles the business logic for managing {@link Playlist}
 * entities.
 * It acts as a bridge between the controller layer (which handles HTTP
 * requests)
 * and the repository layer (which handles database operations).
 * <p>
 * <b>Caching Strategy:</b>
 * We use Spring's caching abstraction to improve read performance.
 * - Read operations ({@code getPlaylistById}, {@code getAllPlaylists}) are
 * cached under the "playlists" cache.
 * - Write operations (create, update, patch, delete, and relationship changes)
 * evict the entire "playlists" cache to ensure that subsequent reads do not
 * return stale data.
 * <p>
 * <b>Transaction Strategy:</b>
 * All write operations are annotated with {@code @Transactional} to ensure
 * proper rollback on failure.
 */
@Service
public class PlaylistServiceImpl implements PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistServiceImpl.class);

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;

    /**
     * Constructs a new {@code PlaylistServiceImpl} with the required repositories.
     * <p>
     * We inject both {@link PlaylistRepository} and {@link TrackRepository} because
     * this service manages the Playlist-Track relationship (the owning side).
     *
     * @param playlistRepository the repository used for database operations on
     *                           playlists
     * @param trackRepository    the repository used to look up tracks for
     *                           relationship management
     */
    public PlaylistServiceImpl(PlaylistRepository playlistRepository, TrackRepository trackRepository) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> We evict all entries in the "playlists" cache because
     * adding a new playlist invalidates the result of {@code getAllPlaylists()}.
     */
    @Override
    @CacheEvict(value = "playlists", allEntries = true)
    @Transactional
    public PlaylistResponseDTO createPlaylist(PlaylistRequestDTO dto) {
        log.info("Evicting 'playlists' cache. Creating new playlist: {}", dto.name());
        Playlist playlist = Playlist.create(dto.name(), dto.description());
        Playlist saved = playlistRepository.saveAndFlush(playlist);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Caching:</b> The result of this method is cached. If the playlist is
     * requested again with the same ID, the cached value is returned instead
     * of querying the database.
     */
    @Override
    @Cacheable(value = "playlists", key = "#id")
    @Transactional(readOnly = true)
    public PlaylistResponseDTO getPlaylistById(Long id) {
        log.info("Cache miss for 'playlists' with id: {}. Fetching from database.", id);
        Playlist playlist = findPlaylistOrThrow(id);

        return mapToResponseDTO(playlist);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Caching:</b> The entire list of playlists is cached. This is highly
     * efficient for read-heavy workloads, but requires careful eviction whenever
     * a playlist is added, updated, or deleted to prevent stale data.
     */
    @Override
    @Cacheable(value = "playlists")
    @Transactional(readOnly = true)
    public List<PlaylistResponseDTO> getAllPlaylists() {
        log.info("Cache miss for 'playlists' list. Fetching all playlists from database.");
        return playlistRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "playlists" cache because
     * updating a playlist invalidates both the individual entry and the list.
     */
    @Override
    @CacheEvict(value = "playlists", allEntries = true)
    @Transactional
    public PlaylistResponseDTO updatePlaylist(Long id, PlaylistUpdateRequestDTO dto) {
        log.info("Evicting 'playlists' cache. Updating playlist with id: {}", id);
        Playlist playlist = findPlaylistOrThrow(id);

        playlist.update(dto.name(), dto.description());
        Playlist saved = playlistRepository.saveAndFlush(playlist);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Merges non-null fields from the patch DTO with the existing entity's values,
     * then delegates to the entity's {@code update} method.
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "playlists" cache.
     */
    @Override
    @CacheEvict(value = "playlists", allEntries = true)
    @Transactional
    public PlaylistResponseDTO patchPlaylist(Long id, PlaylistPatchRequestDTO dto) {
        log.info("Evicting 'playlists' cache. Patching playlist with id: {}", id);
        Playlist playlist = findPlaylistOrThrow(id);

        String name = dto.name() != null ? dto.name() : playlist.getName();
        String description = dto.description() != null ? dto.description() : playlist.getDescription();

        playlist.update(name, description);
        Playlist saved = playlistRepository.saveAndFlush(playlist);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "playlists" cache because
     * deleting a playlist invalidates the list cache.
     */
    @Override
    @CacheEvict(value = "playlists", allEntries = true)
    @Transactional
    public void deletePlaylist(Long id) {
        log.info("Evicting 'playlists' cache. Deleting playlist with id: {}", id);
        Playlist playlist = findPlaylistOrThrow(id);

        playlistRepository.delete(playlist);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "playlists" cache because
     * changing a playlist's tracks invalidates cached playlist representations.
     */
    @Override
    @CacheEvict(value = "playlists", allEntries = true)
    @Transactional
    public PlaylistResponseDTO addTrackToPlaylist(Long playlistId, Long trackId) {
        log.info("Evicting 'playlists' cache. Adding track {} to playlist {}", trackId, playlistId);
        Playlist playlist = findPlaylistOrThrow(playlistId);
        Track track = findTrackOrThrow(trackId);

        playlist.addTrack(track);
        Playlist saved = playlistRepository.saveAndFlush(playlist);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "playlists" cache because
     * changing a playlist's tracks invalidates cached playlist representations.
     */
    @Override
    @CacheEvict(value = "playlists", allEntries = true)
    @Transactional
    public PlaylistResponseDTO removeTrackFromPlaylist(Long playlistId, Long trackId) {
        log.info("Evicting 'playlists' cache. Removing track {} from playlist {}", trackId, playlistId);
        Playlist playlist = findPlaylistOrThrow(playlistId);
        Track track = findTrackOrThrow(trackId);

        playlist.removeTrack(track);
        Playlist saved = playlistRepository.saveAndFlush(playlist);

        return mapToResponseDTO(saved);
    }

    /**
     * Finds a playlist by ID or throws a {@link NotFoundException}.
     * <p>
     * This is an internal helper that centralizes the Optional handling
     * pattern used across all methods that require an existing playlist.
     *
     * @param id the playlist ID to look up
     * @return the found Playlist entity
     * @throws NotFoundException if no playlist exists with the given ID
     */
    private Playlist findPlaylistOrThrow(Long id) {
        Optional<Playlist> playlist = playlistRepository.findById(id);

        if (playlist.isEmpty()) {
            throw new NotFoundException("Playlist not found.");
        }

        return playlist.get();
    }

    /**
     * Finds a track by ID or throws a {@link NotFoundException}.
     * <p>
     * Used by relationship management methods that need to look up tracks.
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
     * Maps a {@link Playlist} entity to a {@link PlaylistResponseDTO}.
     * <p>
     * This centralizes the entity-to-DTO mapping logic to avoid repetition
     * across service methods. The mapping includes the playlist's associated
     * tracks, and each track includes its own tags. Tracks are sorted by title
     * and tags are sorted by name to guarantee deterministic API responses
     * despite the underlying sets' undefined iteration order.
     *
     * @param playlist the entity to map
     * @return the corresponding response DTO
     */
    private PlaylistResponseDTO mapToResponseDTO(Playlist playlist) {
        List<TrackResponseDTO> trackDTOs = playlist.getTracks().stream()
                .map(track -> {
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
                })
                .sorted(Comparator.comparing(TrackResponseDTO::title))
                .toList();

        return new PlaylistResponseDTO(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                trackDTOs);
    }
}
