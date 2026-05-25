package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.ai.AISuggestionRequestDTO;
import com.jfontdev.trackstack.dto.ai.AISuggestionResponseDTO;
import com.jfontdev.trackstack.service.AISuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-powered DJ assistance endpoints.
 * <p>
 * Provides endpoints that leverage the local Ollama LLM for intelligent
 * track recommendations and mixing analysis. All AI endpoints gracefully
 * degrade to rule-based logic when the AI service is unavailable.
 * <p>
 * This controller is part of Phase 02.5: AI Transition Suggestions.
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AISuggestionService aiSuggestionService;

    /**
     * Constructs a new {@code AIController} with the required service.
     *
     * @param aiSuggestionService the service handling AI-powered suggestions
     */
    public AIController(AISuggestionService aiSuggestionService) {
        this.aiSuggestionService = aiSuggestionService;
    }

    /**
     * Gets AI-powered transition suggestions for a track.
     * <p>
     * Analyzes the current track's metadata, the user's transition history,
     * and the desired vibe to suggest the best next tracks using the
     * local Ollama LLM (Gemma 4 26b).
     * <p>
     * If the AI service is unavailable, automatically falls back to
     * rule-based suggestions using key compatibility and BPM matching.
     * <p>
     * Example request:
     * <pre>
     * POST /api/ai/transitions/suggest
     * {
     *   "trackId": 123,
     *   "vibe": "maintain energy but add melody",
     *   "excludeRecentlyPlayed": true,
     *   "limit": 3
     * }
     * </pre>
     *
     * @param dto the suggestion request with track context and preferences
     * @return 200 OK with suggested tracks and AI reasoning,
     *         or rule-based fallback if AI is unavailable
     */
    @PostMapping("/transitions/suggest")
    public ResponseEntity<AISuggestionResponseDTO> suggestTransitions(
            @Valid @RequestBody AISuggestionRequestDTO dto) {
        AISuggestionResponseDTO response = aiSuggestionService.suggestTransitions(dto);
        return ResponseEntity.ok(response);
    }
}
