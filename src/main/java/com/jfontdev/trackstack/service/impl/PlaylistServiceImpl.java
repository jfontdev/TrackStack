package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Playlist;
import com.jfontdev.trackstack.repository.PlaylistRepository;
import com.jfontdev.trackstack.service.PlaylistService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    @Override
    public PlaylistResponseDTO createPlaylist(PlaylistRequestDTO dto) {
        Playlist playlist = Playlist.create(dto.name(), dto.description());
        Playlist saved = playlistRepository.saveAndFlush(playlist);

        return new PlaylistResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getDescription()
        );
    }

    @Override
    public PlaylistResponseDTO getPlaylistById(Long id) {
        Optional<Playlist> playlist = playlistRepository.findById(id);

        if (playlist.isEmpty()) {
            throw new NotFoundException("Playlist not found.");
        }

        Playlist p = playlist.get();

        return new PlaylistResponseDTO(
                p.getId(),
                p.getName(),
                p.getDescription()
        );
    }

    @Override
    public List<PlaylistResponseDTO> getAllPlaylists() {
        return playlistRepository.findAll()
                .stream()
                .map(p -> new PlaylistResponseDTO(
                        p.getId(),
                        p.getName(),
                        p.getDescription()
                ))
                .toList();
    }
}
