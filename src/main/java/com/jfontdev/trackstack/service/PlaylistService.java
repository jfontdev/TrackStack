package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.playlist.PlaylistRequestDTO;
import com.jfontdev.trackstack.dto.playlist.PlaylistResponseDTO;

import java.util.List;

public interface PlaylistService {
    PlaylistResponseDTO createPlaylist(PlaylistRequestDTO dto);

    PlaylistResponseDTO getPlaylistById(Long id);

    List<PlaylistResponseDTO> getAllPlaylists();
}