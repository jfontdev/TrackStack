package com.jfontdev.trackstack.dto.ai;

import java.util.List;

/**
 * Response DTO for AI-powered transition suggestions.
 * <p>
 * Contains a list of suggested tracks with AI-generated reasoning.
 * The {@code source} field indicates whether suggestions came from the AI model
 * or from the rule-based fallback engine.
 *
 * @param trackId      the source track ID
 * @param suggestions  list of suggested transitions
 * @param aiReasoning  overall reasoning from the AI model
 * @param source       "AI" or "RULE_BASED" indicating which engine provided suggestions
 */
public record AISuggestionResponseDTO(
        Long trackId,
        List<SuggestedTransitionDTO> suggestions,
        String aiReasoning,
        String source) {
}
