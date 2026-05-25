package com.jfontdev.trackstack.dto.ai;

/**
 * Represents a single track suggestion from the AI or rule-based engine.
 * <p>
 * Contains the track details and reasoning for why it was suggested as the
 * next track in the mix.
 *
 * @param trackId    the suggested track's ID
 * @param title      the track title
 * @param artist     the track artist
 * @param genre      the track genre
 * @param bpm        the track BPM
 * @param key        the track musical key
 * @param energy     the track energy level
 * @param reason     why this track was suggested
 * @param confidence confidence score 0.0-1.0 (optional, AI only)
 */
public record SuggestedTransitionDTO(
        Long trackId,
        String title,
        String artist,
        String genre,
        Double bpm,
        String key,
        Integer energy,
        String reason,
        Double confidence) {
}
