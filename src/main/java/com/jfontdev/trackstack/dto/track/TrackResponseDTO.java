package com.jfontdev.trackstack.dto.track;

public record TrackResponseDTO(Long id,
                               String title,
                               String artist,
                               Double bpm,
                               String key,
                               String duration) {
}
