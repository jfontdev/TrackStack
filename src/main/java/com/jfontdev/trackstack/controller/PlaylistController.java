package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;
import com.jfontdev.trackstack.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping
    public ResponseEntity<PlaylistResponseDTO> create(@Valid @RequestBody PlaylistRequestDTO dto) {
        PlaylistResponseDTO response = playlistService.createPlaylist(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PlaylistResponseDTO getById(@PathVariable Long id) {
        return playlistService.getPlaylistById(id);
    }

    @GetMapping
    public List<PlaylistResponseDTO> getAll() {
        return playlistService.getAllPlaylists();
    }
}
