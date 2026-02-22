package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Playlist;
import com.jfontdev.trackstack.repository.PlaylistRepository;
import com.jfontdev.trackstack.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
 * - Write operations ({@code createPlaylist}) evict the entire "playlists"
 * cache to ensure
 * that subsequent reads (especially {@code getAllPlaylists}) do not return
 * stale data.
 */
@Service
public class PlaylistServiceImpl implements PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistServiceImpl.class);

    private final PlaylistRepository playlistRepository;

    /**
     * Constructs a new {@code PlaylistServiceImpl} with the required repository.
     * We use constructor injection to ensure the repository is provided and
     * immutable.
     *
     * @param playlistRepository the repository used for database operations on
     *                           playlists
     */
    public PlaylistServiceImpl(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /**
     * Creates a new playlist in the system.
     * <p>
     * This method maps the incoming DTO to a domain entity, saves it to the
     * database,
     * and then maps the saved entity back to a response DTO.
     * <p>
     * <b>Cache Eviction:</b> We evict all entries in the "playlists" cache because
     * adding
     * a new playlist invalidates the result of {@code getAllPlaylists()}.
     *
     * @param dto the data transfer object containing the details of the playlist to
     *            create
     * @return a response DTO containing the saved playlist's details, including its
     *         generated ID
     */
    @Override
    @CacheEvict(value = "playlists", allEntries = true)
    public PlaylistResponseDTO createPlaylist(PlaylistRequestDTO dto) {
        log.info("Evicting 'playlists' cache. Creating new playlist: {}", dto.name());
        Playlist playlist = Playlist.create(dto.name(), dto.description());
        Playlist saved = playlistRepository.saveAndFlush(playlist);

        return new PlaylistResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getDescription());
    }

    /**
     * Retrieves a playlist by its unique identifier.
     * <p>
     * <b>Caching:</b> The result of this method is cached. If the playlist is
     * requested again
     * with the same ID, the cached value is returned instead of querying the
     * database.
     *
     * @param id the unique identifier of the playlist to retrieve
     * @return a response DTO containing the playlist's details
     * @throws NotFoundException if no playlist is found with the provided ID
     */
    @Override
    @Cacheable(value = "playlists", key = "#id")
    public PlaylistResponseDTO getPlaylistById(Long id) {
        log.info("Cache miss for 'playlists' with id: {}. Fetching from database.", id);
        Optional<Playlist> playlist = playlistRepository.findById(id);

        if (playlist.isEmpty()) {
            throw new NotFoundException("Playlist not found.");
        }

        Playlist p = playlist.get();

        return new PlaylistResponseDTO(
                p.getId(),
                p.getName(),
                p.getDescription());
    }

    /**
     * Retrieves all playlists currently stored in the system.
     * <p>
     * <b>Caching:</b> The entire list of playlists is cached. This is highly
     * efficient for
     * read-heavy workloads, but requires careful eviction (clearing the cache)
     * whenever
     * a playlist is added, updated, or deleted to prevent stale data.
     *
     * @return a list of response DTOs representing all playlists
     */
    @Override
    @Cacheable(value = "playlists")
    public List<PlaylistResponseDTO> getAllPlaylists() {
        log.info("Cache miss for 'playlists' list. Fetching all playlists from database.");
        return playlistRepository.findAll()
                .stream()
                .map(p -> new PlaylistResponseDTO(
                        p.getId(),
                        p.getName(),
                        p.getDescription()))
                .toList();
    }
}
