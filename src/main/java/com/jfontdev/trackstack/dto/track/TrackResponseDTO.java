package com.jfontdev.trackstack.dto.track;

import java.time.LocalDateTime;

/**
 * Response DTO representing a track returned by the API.
 * <p>
 * Includes the track's metadata and file information. This DTO is the
 * single representation of a track in all API responses.
 *
 * @param id              the track's unique identifier
 * @param title           the track title
 * @param artist          the track artist
 * @param album           the track album
 * @param bpm             the beats per minute
 * @param key             the musical key
 * @param durationSeconds the track duration in seconds
 * @param genre           the track genre
 * @param filePath        the file path
 * @param fileFormat      the file format
 * @param bitrate         the bitrate in kbps
 * @param energy          the energy level 1-5
 * @param playCount       number of times played
 * @param lastPlayedDate  last played timestamp
 * @param addedDate       when track was added to library
 */
public record TrackResponseDTO(
        Long id,
        String title,
        String artist,
        String album,
        Double bpm,
        String key,
        Integer durationSeconds,
        String genre,
        String filePath,
        String fileFormat,
        Integer bitrate,
        Integer energy,
        Integer playCount,
        LocalDateTime lastPlayedDate,
        LocalDateTime addedDate) {
}
