package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.track.TrackRequestDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.service.TrackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracks")
public class TrackController {


    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @PostMapping
    public ResponseEntity<TrackResponseDTO> create(@Valid @RequestBody TrackRequestDTO dto) {
        TrackResponseDTO response = trackService.createTrack(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public TrackResponseDTO getById(@PathVariable Long id) {
        return trackService.getTrackById(id);
    }

    @GetMapping
    public List<TrackResponseDTO> getAll() {
        return trackService.getAllTracks();
    }
}
