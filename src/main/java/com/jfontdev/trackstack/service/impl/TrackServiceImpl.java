package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.service.TrackService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrackServiceImpl implements TrackService {

    private final TrackRepository trackRepository;

    public TrackServiceImpl(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    @Override
    public TrackResponseDTO createTrack(TrackRequestDTO dto) {
        Track track = Track.create(
                dto.title(),
                dto.artist(),
                dto.bpm(),
                dto.key(),
                dto.duration()
        );

        Track savedTrack = trackRepository.saveAndFlush(track);

        return new TrackResponseDTO(
                savedTrack.getId(),
                savedTrack.getTitle(),
                savedTrack.getArtist(),
                savedTrack.getBpm(),
                savedTrack.getKey(),
                savedTrack.getDuration()
        );
    }

    @Override
    public TrackResponseDTO getTrackById(Long id) {
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
                foundTrack.getDuration()
        );
    }

    @Override
    public List<TrackResponseDTO> getAllTracks() {
        return trackRepository.findAll().stream().map(track -> new TrackResponseDTO(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getBpm(),
                track.getKey(),
                track.getDuration()
        )).toList();
    }
}
