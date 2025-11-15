package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;

import java.util.List;

public interface TrackService {
    TrackResponseDTO createTrack(TrackRequestDTO dto);

    TrackResponseDTO getTrackById(Long id);

    List<TrackResponseDTO> getAllTracks();
}
