package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.ai.AISuggestionRequestDTO;
import com.jfontdev.trackstack.dto.ai.AISuggestionResponseDTO;

/**
 * Service interface for AI-powered track transition suggestions.
 * <p>
 * Provides intelligent recommendations for what track to play next based on:
 * <ul>
 *   <li>The current track's metadata (BPM, key, genre, energy)</li>
 *   <li>The user's transition history and ratings</li>
 *   <li>Natural language descriptions of desired vibe/mood</li>
 *   <li>Harmonic compatibility rules (Camelot wheel)</li>
 * </ul>
 * <p>
 * The implementation uses a local Ollama LLM (Gemma 4 26b) for suggestions
 * with automatic fallback to rule-based matching when the AI service is unavailable.
 */
public interface AISuggestionService {

    /**
     * Suggests tracks that would transition well from the given track.
     * <p>
     * This is the primary method for AI-enhanced track discovery. It attempts
     * to use the Ollama LLM for intelligent suggestions, but automatically
     * falls back to rule-based key/BPM matching if the AI service is down.
     *
     * @param request the suggestion request containing track context and preferences
     * @return a response with suggested tracks and reasoning
     */
    AISuggestionResponseDTO suggestTransitions(AISuggestionRequestDTO request);
}
