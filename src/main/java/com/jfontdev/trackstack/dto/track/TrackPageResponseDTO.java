package com.jfontdev.trackstack.dto.track;

import java.util.List;

/**
 * Response DTO for paginated track queries.
 * <p>
 * This record mirrors the API shape used by the track list endpoint while
 * remaining serialization-friendly for Redis caching.
 * </p>
 *
 * @param content the current page of tracks
 * @param page    pagination metadata for the current query
 */
public record TrackPageResponseDTO(List<TrackResponseDTO> content, TrackPageMetadataDTO page) {
}
