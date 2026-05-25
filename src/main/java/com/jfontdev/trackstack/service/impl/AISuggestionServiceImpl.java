package com.jfontdev.trackstack.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfontdev.trackstack.dto.ai.AISuggestionRequestDTO;
import com.jfontdev.trackstack.dto.ai.AISuggestionResponseDTO;
import com.jfontdev.trackstack.dto.ai.SuggestedTransitionDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.model.Transition;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.repository.TransitionRepository;
import com.jfontdev.trackstack.service.AISuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link AISuggestionService} using Ollama LLM with
 * rule-based fallback.
 * <p>
 * <b>AI Strategy:</b>
 * <p>
 * The service constructs a detailed prompt including:
 * <ul>
 * <li>The current track's full metadata (title, artist, BPM, key, genre,
 * energy)</li>
 * <li>Up to 50 candidate tracks from the library for context</li>
 * <li>The user's recent transition history with ratings</li>
 * <li>The desired vibe/mood description</li>
 * </ul>
 * <p>
 * The prompt instructs the AI to return a structured JSON response with
 * suggested tracks and reasoning. This is then parsed into DTOs.
 * <p>
 * <b>Fallback Strategy:</b>
 * <p>
 * If the Ollama service is unavailable (network issues, model not loaded,
 * etc.),
 * the service automatically falls back to rule-based suggestions using:
 * <ul>
 * <li>Existing transitions with high ratings from the current track</li>
 * <li>Tracks with harmonically compatible keys (Camelot wheel)</li>
 * <li>Tracks with similar BPM (±5 BPM range)</li>
 * <li>Energy level matching</li>
 * </ul>
 * <p>
 * The fallback ensures the API is always functional even without AI.
 */
@Service
public class AISuggestionServiceImpl implements AISuggestionService {

    private static final Logger log = LoggerFactory.getLogger(AISuggestionServiceImpl.class);
    private static final String SOURCE_AI = "AI";
    private static final String SOURCE_RULE_BASED = "RULE_BASED";
    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIBRARY_CONTEXT = 50;

    private final ChatModel chatModel;
    private final TrackRepository trackRepository;
    private final TransitionRepository transitionRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AISuggestionServiceImpl(
            ChatModel chatModel,
            TrackRepository trackRepository,
            TransitionRepository transitionRepository,
            ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.trackRepository = trackRepository;
        this.transitionRepository = transitionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AISuggestionResponseDTO suggestTransitions(AISuggestionRequestDTO request) {
        log.info("Suggesting transitions from track: {}, vibe: {}",
                request.trackId(), request.vibe());

        // Verify the source track exists
        Track sourceTrack = trackRepository.findById(request.trackId())
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + request.trackId()));

        try {
            // Attempt AI-powered suggestions first
            return suggestWithAi(sourceTrack, request);
        } catch (Exception e) {
            // If AI fails (Ollama down, parsing error, etc.), log and fallback
            log.warn("AI suggestion failed, falling back to rule-based: {}", e.getMessage());
            return suggestWithRules(sourceTrack, request);
        }
    }

    /**
     * Attempts to get suggestions from the Ollama LLM.
     * <p>
     * Builds a comprehensive prompt with track context and asks the AI to
     * recommend the best next tracks in the set.
     *
     * @param sourceTrack the current track being played
     * @param request     the suggestion request
     * @return AI-powered suggestions
     */
    private AISuggestionResponseDTO suggestWithAi(Track sourceTrack, AISuggestionRequestDTO request) {
        // Build the prompt with track context
        String promptText = buildPrompt(sourceTrack, request);

        // Create system message defining the AI's role
        SystemMessage systemMessage = new SystemMessage(
                "You are an expert DJ assistant with deep knowledge of electronic music, "
                        + "harmonic mixing, and set flow. You analyze track metadata and suggest "
                        + "the best next tracks based on musical compatibility, energy, and vibe. "
                        + "Respond ONLY with valid JSON in the specified format.");

        // Create user message with the prompt
        UserMessage userMessage = new UserMessage(promptText);

        // Call the AI model
        Prompt prompt = new Prompt(systemMessage, userMessage);
        ChatResponse chatResponse = chatModel.call(prompt);
        String aiResponse = chatResponse.getResult().getOutput().getText();

        log.debug("AI raw response: {}", aiResponse);

        // Parse the JSON response
        List<SuggestedTransitionDTO> suggestions = parseAiResponse(aiResponse);

        return new AISuggestionResponseDTO(
                sourceTrack.getId(),
                sourceTrack.getTitle(),
                sourceTrack.getArtist(),
                suggestions,
                "AI-powered suggestions based on track analysis",
                SOURCE_AI);
    }

    /**
     * Falls back to rule-based suggestions when AI is unavailable.
     * <p>
     * Uses a combination of strategies:
     * <ol>
     * <li>Best-rated existing transitions from the source track</li>
     * <li>Tracks with compatible keys and similar BPM</li>
     * <li>Energy level matching</li>
     * </ol>
     *
     * @param sourceTrack the current track being played
     * @param request     the suggestion request
     * @return rule-based suggestions
     */
    private AISuggestionResponseDTO suggestWithRules(Track sourceTrack, AISuggestionRequestDTO request) {
        int limit = request.limit() != null ? request.limit() : DEFAULT_LIMIT;
        List<SuggestedTransitionDTO> suggestions = new ArrayList<>();

        // Strategy 1: Use existing transitions with high ratings
        List<Transition> existingTransitions = transitionRepository
                .findBySourceTrackIdOrderByRatingDesc(sourceTrack.getId());

        for (Transition transition : existingTransitions) {
            if (suggestions.size() >= limit)
                break;

            Optional<Track> targetTrack = trackRepository.findById(transition.getTargetTrackId());
            if (targetTrack.isPresent()) {
                Track track = targetTrack.get();
                suggestions.add(new SuggestedTransitionDTO(
                        track.getId(),
                        track.getTitle(),
                        track.getArtist(),
                        track.getGenre(),
                        track.getBpm(),
                        track.getKey(),
                        track.getEnergy(),
                        "Your rated transition: " + transition.getRating() + "/5 stars" +
                                (transition.getNotes() != null ? " - " + transition.getNotes() : ""),
                        transition.getRating() / 5.0));
            }
        }

        // Strategy 2: If we still need more, find compatible tracks from library
        if (suggestions.size() < limit) {
            // Get all tracks except current one and already suggested
            List<Long> excludedIds = new ArrayList<>();
            excludedIds.add(sourceTrack.getId());
            suggestions.forEach(s -> excludedIds.add(s.trackId()));

            List<Track> candidates = trackRepository.findAll();
            for (Track candidate : candidates) {
                if (suggestions.size() >= limit)
                    break;
                if (excludedIds.contains(candidate.getId()))
                    continue;

                // Check key compatibility
                boolean keyCompatible = isKeyCompatible(sourceTrack.getKey(), candidate.getKey());
                // Check BPM proximity (±5 BPM)
                boolean bpmCompatible = isBpmCompatible(sourceTrack.getBpm(), candidate.getBpm());

                if (keyCompatible || bpmCompatible) {
                    String reason = buildFallbackReason(sourceTrack, candidate, keyCompatible, bpmCompatible);
                    suggestions.add(new SuggestedTransitionDTO(
                            candidate.getId(),
                            candidate.getTitle(),
                            candidate.getArtist(),
                            candidate.getGenre(),
                            candidate.getBpm(),
                            candidate.getKey(),
                            candidate.getEnergy(),
                            reason,
                            0.7));
                }
            }
        }

        return new AISuggestionResponseDTO(
                sourceTrack.getId(),
                sourceTrack.getTitle(),
                sourceTrack.getArtist(),
                suggestions,
                "Rule-based suggestions (AI service unavailable)",
                SOURCE_RULE_BASED);
    }

    /**
     * Builds a comprehensive prompt for the AI model.
     * <p>
     * The prompt includes all relevant context so the AI can make informed
     * suggestions about track transitions.
     *
     * @param sourceTrack the current track
     * @param request     the suggestion request
     * @return the formatted prompt string
     */
    private String buildPrompt(Track sourceTrack, AISuggestionRequestDTO request) {
        StringBuilder prompt = new StringBuilder();

        // Current track context
        prompt.append("Current track:\n");
        prompt.append(formatTrackForPrompt(sourceTrack));
        prompt.append("\n\n");

        // Vibe/mood description
        if (request.vibe() != null && !request.vibe().isEmpty()) {
            prompt.append("Desired vibe for next track: ").append(request.vibe()).append("\n\n");
        }

        // Recent transition history
        List<Transition> recentTransitions = transitionRepository
                .findBySourceTrackIdOrderByRatingDesc(sourceTrack.getId());
        if (!recentTransitions.isEmpty()) {
            prompt.append("Your previous transitions from this track:\n");
            int count = 0;
            for (Transition t : recentTransitions) {
                if (count++ >= 5)
                    break; // Limit history to top 5
                Optional<Track> target = trackRepository.findById(t.getTargetTrackId());
                if (target.isPresent()) {
                    prompt.append("  - ").append(target.get().getTitle())
                            .append(" (rating: ").append(t.getRating()).append("/5)");
                    if (t.getNotes() != null) {
                        prompt.append(" - ").append(t.getNotes());
                    }
                    prompt.append("\n");
                }
            }
            prompt.append("\n");
        }

        // Library context - provide candidate tracks
        List<Track> library = trackRepository.findAll();
        prompt.append("Available tracks in your library (").append(library.size()).append(" total):\n");
        int contextCount = 0;
        for (Track track : library) {
            if (contextCount >= MAX_LIBRARY_CONTEXT)
                break;
            if (track.getId().equals(sourceTrack.getId()))
                continue;
            prompt.append(formatTrackForPrompt(track));
            prompt.append("\n");
            contextCount++;
        }
        if (library.size() > MAX_LIBRARY_CONTEXT) {
            prompt.append("... and ").append(library.size() - MAX_LIBRARY_CONTEXT).append(" more tracks\n");
        }
        prompt.append("\n");

        // Exclude recently played if requested
        if (Boolean.TRUE.equals(request.excludeRecentlyPlayed())) {
            prompt.append("EXCLUDE tracks played in the last 2 weeks.\n");
        }

        // Response format instruction
        int limit = request.limit() != null ? request.limit() : DEFAULT_LIMIT;
        prompt.append("\nBased on the current track, desired vibe, and your transition history, ");
        prompt.append("suggest ").append(limit).append(" tracks that would transition well next.\n\n");
        prompt.append("Respond ONLY with JSON in this exact format:\n");
        prompt.append("{\n");
        prompt.append("  \"suggestions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"trackId\": 123,\n");
        prompt.append("      \"reason\": \"detailed explanation of why this track works\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * Formats a track for inclusion in the AI prompt.
     *
     * @param track the track to format
     * @return a string representation with key metadata
     */
    private String formatTrackForPrompt(Track track) {
        StringBuilder sb = new StringBuilder();
        sb.append("Track ID: ").append(track.getId());
        sb.append(", Title: \"").append(track.getTitle()).append("\"");
        sb.append(", Artist: \"").append(track.getArtist()).append("\"");
        if (track.getBpm() != null)
            sb.append(", BPM: ").append(track.getBpm());
        if (track.getKey() != null)
            sb.append(", Key: ").append(track.getKey());
        if (track.getGenre() != null)
            sb.append(", Genre: ").append(track.getGenre());
        if (track.getEnergy() != null)
            sb.append(", Energy: ").append(track.getEnergy()).append("/5");
        return sb.toString();
    }

    /**
     * Parses the AI's JSON response into a list of suggestions.
     * <p>
     * Expects a JSON object with a "suggestions" array containing objects
     * with "trackId" and "reason" fields.
     *
     * @param aiResponse the raw JSON response from the AI
     * @return list of parsed suggestions
     */
    private List<SuggestedTransitionDTO> parseAiResponse(String aiResponse) {
        List<SuggestedTransitionDTO> suggestions = new ArrayList<>();

        try {
            // Extract JSON from the response (in case there's markdown or extra text)
            String json = extractJsonFromResponse(aiResponse);

            // Parse the JSON structure
            AiResponseStructure structure = objectMapper.readValue(json, AiResponseStructure.class);

            for (AiSuggestionItem item : structure.suggestions()) {
                Optional<Track> track = trackRepository.findById(item.trackId);
                if (track.isPresent()) {
                    Track t = track.get();
                    suggestions.add(new SuggestedTransitionDTO(
                            t.getId(),
                            t.getTitle(),
                            t.getArtist(),
                            t.getGenre(),
                            t.getBpm(),
                            t.getKey(),
                            t.getEnergy(),
                            item.reason,
                            0.85)); // High confidence for AI suggestions
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", aiResponse, e);
            // Return empty list if parsing fails - caller will handle
        }

        return suggestions;
    }

    /**
     * Extracts JSON content from an AI response that may contain markdown
     * or other formatting.
     *
     * @param response the raw AI response
     * @return cleaned JSON string
     */
    private String extractJsonFromResponse(String response) {
        // Remove markdown code blocks if present
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * Checks if two keys are compatible for rule-based fallback.
     *
     * @param key1 first key
     * @param key2 second key
     * @return true if compatible
     */
    private boolean isKeyCompatible(String key1, String key2) {
        if (key1 == null || key2 == null)
            return false;
        String k1 = key1.trim().toUpperCase();
        String k2 = key2.trim().toUpperCase();
        if (k1.equals(k2))
            return true;

        // Simple Camelot check for adjacent numbers
        if (k1.length() >= 2 && k2.length() >= 2) {
            char letter1 = k1.charAt(k1.length() - 1);
            char letter2 = k2.charAt(k2.length() - 1);
            if (letter1 == letter2) {
                try {
                    int num1 = Integer.parseInt(k1.substring(0, k1.length() - 1));
                    int num2 = Integer.parseInt(k2.substring(0, k2.length() - 1));
                    int diff = Math.abs(num1 - num2);
                    return diff == 1 || diff == 7 || diff == 11; // 11 is same as -1 mod 12
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Checks if two BPMs are within compatible range.
     *
     * @param bpm1 first BPM
     * @param bpm2 second BPM
     * @return true if within ±5 BPM
     */
    private boolean isBpmCompatible(Double bpm1, Double bpm2) {
        if (bpm1 == null || bpm2 == null)
            return false;
        return Math.abs(bpm1 - bpm2) <= 5.0;
    }

    /**
     * Builds a human-readable reason for rule-based fallback suggestions.
     *
     * @param source    the source track
     * @param candidate the suggested track
     * @param keyCompat whether keys are compatible
     * @param bpmCompat whether BPMs are compatible
     * @return reason string
     */
    private String buildFallbackReason(Track source, Track candidate, boolean keyCompat, boolean bpmCompat) {
        List<String> reasons = new ArrayList<>();
        if (keyCompat)
            reasons.add("compatible key (" + source.getKey() + " → " + candidate.getKey() + ")");
        if (bpmCompat)
            reasons.add("similar BPM (" + source.getBpm() + " → " + candidate.getBpm() + ")");
        if (source.getGenre() != null && source.getGenre().equals(candidate.getGenre())) {
            reasons.add("same genre (" + candidate.getGenre() + ")");
        }
        return "Rule-based match: " + String.join(", ", reasons);
    }

    /**
     * Internal record for parsing AI JSON response structure.
     */
    private record AiResponseStructure(List<AiSuggestionItem> suggestions) {
    }

    /**
     * Internal record for parsing individual AI suggestions.
     */
    private record AiSuggestionItem(Long trackId, String reason) {
    }
}
