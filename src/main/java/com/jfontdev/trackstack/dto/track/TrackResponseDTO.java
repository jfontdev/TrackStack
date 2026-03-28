package com.jfontdev.trackstack.dto.track;

import com.jfontdev.trackstack.dto.tag.TagResponseDTO;

import java.util.List;

/**
 * Response DTO representing a track returned by the API.
 * <p>
 * Includes the track's metadata and its associated tags. This DTO is the
 * single representation of a track in all API responses (create, update,
 * get by ID, list).
 *
 * @param id       the track's unique identifier
 * @param title    the track title
 * @param artist   the track artist
 * @param bpm      the beats per minute
 * @param key      the musical key
 * @param duration the track duration
 * @param tags     the tags associated with this track
 */
public record TrackResponseDTO(Long id,
                               String title,
                               String artist,
                               Double bpm,
                               String key,
                               String duration,
                               List<TagResponseDTO> tags) {
}
