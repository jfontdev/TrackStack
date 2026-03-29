package com.jfontdev.trackstack.dto.track;

/**
 * Pagination metadata for track list responses.
 *
 * @param size          the requested page size
 * @param number        the current zero-based page index
 * @param totalElements total number of matching elements
 * @param totalPages    total number of pages for the query
 */
public record TrackPageMetadataDTO(int size, int number, long totalElements, int totalPages) {
}
