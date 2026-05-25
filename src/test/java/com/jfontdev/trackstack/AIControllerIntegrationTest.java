package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the AI suggestion endpoints.
 * <p>
 * These tests exercise the AI controller and service with fallback logic.
 * Since Ollama is not available in the test environment, these tests primarily
 * verify the rule-based fallback behavior and endpoint contract.
 * <p>
 * Tests that require an actual Ollama instance should be run locally with
 * the AI service enabled.
 */
@TestPropertySource(properties = {
        "spring.ai.ollama.timeout=2s",
        "spring.ai.ollama.chat.options.model=llama3.2"
})
public class AIControllerIntegrationTest extends BaseIntegrationTest {

    private Long createTrack(String title, String artist, String genre, String key, Double bpm, Integer energy) {
        Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                "title", title,
                "artist", artist,
                "filePath", "/music/test/" + title.toLowerCase().replace(" ", "-") + ".mp3"));

        if (genre != null) payload.put("genre", genre);
        if (key != null) payload.put("key", key);
        if (bpm != null) payload.put("bpm", bpm);
        if (energy != null) payload.put("energy", energy);

        Number id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/tracks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        return id.longValue();
    }

    @Test
    void suggestTransitionsReturnsFallbackWhenAiUnavailable() {
        // GIVEN a source track and some candidate tracks
        Long sourceId = createTrack("Source Track", "Artist A", "Techno", "4A", 140.0, 4);
        Long target1 = createTrack("Target 1", "Artist B", "Techno", "5A", 138.0, 4);
        Long target2 = createTrack("Target 2", "Artist C", "Techno", "11A", 141.0, 5);

        // Create a transition to establish history
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceTrackId", sourceId,
                        "targetTrackId", target1,
                        "rating", 5,
                        "notes", "Great transition"))
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(201);

        // WHEN requesting AI suggestions (will fallback since Ollama not running)
        Map<String, Object> request = Map.of(
                "trackId", sourceId,
                "vibe", "maintain energy",
                "excludeRecentlyPlayed", false,
                "limit", 3);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/ai/transitions/suggest")
                .then()
                .statusCode(200)
                .body("trackId", equalTo(sourceId.intValue()))
                .body("sourceTitle", equalTo("Source Track"))
                .body("sourceArtist", equalTo("Artist A"))
                .body("suggestions", notNullValue())
                .body("suggestions.size()", greaterThan(0))
                .body("source", equalTo("RULE_BASED")); // Should fallback
    }

    @Test
    void suggestTransitionsWithMissingTrackReturns404() {
        Map<String, Object> request = Map.of(
                "trackId", 9999,
                "vibe", "test");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/ai/transitions/suggest")
                .then()
                .statusCode(404)
                .body("error", containsString("Track not found"));
    }

    @Test
    void suggestTransitionsWithInvalidRequestReturns400() {
        // Missing required trackId
        Map<String, Object> request = Map.of("vibe", "test");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/ai/transitions/suggest")
                .then()
                .statusCode(400);
    }
}
