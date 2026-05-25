package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Transition API.
 * <p>
 * These tests exercise the full Controller -> Service -> Repository -> DB
 * flow for transitions using real infrastructure via Testcontainers.
 * They cover creation, retrieval from/to tracks, best transitions filtering,
 * full update (PUT), partial update (PATCH), deletion, and play recording.
 * <p>
 * Since transitions reference tracks, each test that creates a transition
 * first creates the necessary track(s) via the Track API.
 */
public class TransitionControllerIntegrationTest extends BaseIntegrationTest {

    // --- Helper: create a track and return its ID ---

    private Long createTrack(String title, String artist, String genre, String key, Double bpm) {
        Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                "title", title,
                "artist", artist,
                "filePath", "/music/test/" + title.toLowerCase().replace(" ", "-") + ".mp3"));

        // Add optional fields if provided
        if (genre != null) payload.put("genre", genre);
        if (key != null) payload.put("key", key);
        if (bpm != null) payload.put("bpm", bpm);

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

    // --- Tests ---

    @Test
    void createTransitionThenGetById() {
        // GIVEN two tracks
        Long trackA = createTrack("Source Track", "Artist A", "Techno", "4A", 140.0);
        Long trackB = createTrack("Target Track", "Artist B", "Techno", "5A", 138.0);

        // WHEN creating a transition from A to B
        Map<String, Object> payload = Map.of(
                "sourceTrackId", trackA,
                "targetTrackId", trackB,
                "rating", 5,
                "notes", "Perfect blend, keys work well together",
                "style", "blend");

        Number transitionId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("sourceTrackId", equalTo(trackA.intValue()))
                .body("targetTrackId", equalTo(trackB.intValue()))
                .body("rating", equalTo(5))
                .body("notes", equalTo("Perfect blend, keys work well together"))
                .body("style", equalTo("blend"))
                // Auto-calculated fields should be populated
                .body("compatibleKeys", equalTo(true)) // 4A and 5A are adjacent
                .body("bpmDifference", equalTo(2.0f)) // |140 - 138| = 2
                .body("timesPlayed", equalTo(0))
                .extract()
                .path("id");

        long id = transitionId.longValue();

        // THEN it can be retrieved by ID
        given()
                .when()
                .get("/api/transitions/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("rating", equalTo(5))
                .body("compatibleKeys", equalTo(true));
    }

    @Test
    void createTransitionWithIncompatibleKeys() {
        // GIVEN two tracks with incompatible keys (4A and 7B)
        Long trackA = createTrack("Track A", "Artist A", "Techno", "4A", 140.0);
        Long trackB = createTrack("Track B", "Artist B", "House", "7B", 128.0);

        // WHEN creating a transition
        Map<String, Object> payload = Map.of(
                "sourceTrackId", trackA,
                "targetTrackId", trackB,
                "rating", 3,
                "notes", "Key clash but energy works");

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(201)
                .body("compatibleKeys", equalTo(false)) // 4A and 7B are not compatible
                .body("bpmDifference", equalTo(12.0f)); // |140 - 128| = 12
    }

    @Test
    void createDuplicateTransitionReturns400() {
        // GIVEN two tracks and an existing transition
        Long trackA = createTrack("Track A", "Artist A", "Techno", null, null);
        Long trackB = createTrack("Track B", "Artist B", "Techno", null, null);

        Map<String, Object> payload = Map.of(
                "sourceTrackId", trackA,
                "targetTrackId", trackB,
                "rating", 4);

        // First creation succeeds
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(201);

        // Second creation with same direction fails
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(400)
                .body("error", containsString("already exists"));
    }

    @Test
    void createTransitionWithMissingTrackReturns404() {
        Map<String, Object> payload = Map.of(
                "sourceTrackId", 9999,
                "targetTrackId", 9998,
                "rating", 4);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(404)
                .body("error", containsString("Source track not found"));
    }

    @Test
    void getTransitionsFromTrackReturnsOrderedByRating() {
        // GIVEN a source track and three target tracks with transitions
        Long source = createTrack("Source", "Artist", "Techno", "4A", 140.0);
        Long target1 = createTrack("Target 1", "Artist 1", "Techno", "5A", 138.0);
        Long target2 = createTrack("Target 2", "Artist 2", "Techno", "3A", 142.0);
        Long target3 = createTrack("Target 3", "Artist 3", "Techno", "4A", 140.0);

        // Create transitions with different ratings
        createTransition(source, target1, 3, null, null);
        createTransition(source, target2, 5, null, null);
        createTransition(source, target3, 4, null, null);

        // WHEN querying transitions from source
        // THEN they are ordered by rating descending (5, 4, 3)
        given()
                .when()
                .get("/api/transitions/from/{trackId}", source)
                .then()
                .statusCode(200)
                .body("size()", equalTo(3))
                .body("[0].rating", equalTo(5))
                .body("[1].rating", equalTo(4))
                .body("[2].rating", equalTo(3));
    }

    @Test
    void getTransitionsToTrackReturnsOrderedByRating() {
        // GIVEN three source tracks and one target
        Long source1 = createTrack("Source 1", "Artist 1", "Techno", null, null);
        Long source2 = createTrack("Source 2", "Artist 2", "Techno", null, null);
        Long source3 = createTrack("Source 3", "Artist 3", "Techno", null, null);
        Long target = createTrack("Target", "Artist", "Techno", null, null);

        createTransition(source1, target, 2, null, null);
        createTransition(source2, target, 4, null, null);
        createTransition(source3, target, 5, null, null);

        // WHEN querying transitions to target
        // THEN ordered by rating descending (5, 4, 2)
        given()
                .when()
                .get("/api/transitions/to/{trackId}", target)
                .then()
                .statusCode(200)
                .body("size()", equalTo(3))
                .body("[0].rating", equalTo(5))
                .body("[1].rating", equalTo(4))
                .body("[2].rating", equalTo(2));
    }

    @Test
    void getBestTransitionsWithMinRatingAndLimit() {
        // GIVEN a source track with multiple transitions
        Long source = createTrack("Source", "Artist", "Techno", null, null);
        Long target1 = createTrack("Target 1", "Artist 1", "Techno", null, null);
        Long target2 = createTrack("Target 2", "Artist 2", "Techno", null, null);
        Long target3 = createTrack("Target 3", "Artist 3", "Techno", null, null);
        Long target4 = createTrack("Target 4", "Artist 4", "Techno", null, null);

        createTransition(source, target1, 5, null, null);
        createTransition(source, target2, 2, null, null);
        createTransition(source, target3, 4, null, null);
        createTransition(source, target4, 3, null, null);

        // WHEN querying best transitions with minRating=4 and limit=2
        given()
                .when()
                .get("/api/transitions/best?trackId={trackId}&minRating=4&limit=2", source)
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].rating", equalTo(5))
                .body("[1].rating", equalTo(4));
    }

    @Test
    void updateTransitionReturns200WithUpdatedData() {
        // GIVEN an existing transition
        Long trackA = createTrack("Track A", "Artist A", "Techno", null, null);
        Long trackB = createTrack("Track B", "Artist B", "Techno", null, null);

        Number transitionId = createTransition(trackA, trackB, 3, "Old notes", "blend");

        // WHEN updating the transition
        Map<String, Object> updatePayload = Map.of(
                "rating", 5,
                "notes", "Updated notes after trying again",
                "style", "cut");

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/api/transitions/{id}", transitionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(transitionId.intValue()))
                .body("rating", equalTo(5))
                .body("notes", equalTo("Updated notes after trying again"))
                .body("style", equalTo("cut"));
    }

    @Test
    void patchTransitionUpdatesOnlyProvidedFields() {
        // GIVEN an existing transition
        Long trackA = createTrack("Track A", "Artist A", "Techno", null, null);
        Long trackB = createTrack("Track B", "Artist B", "Techno", null, null);

        Number transitionId = createTransition(trackA, trackB, 4, "Original notes", "blend");

        // WHEN patching only the rating
        Map<String, Object> patchPayload = Map.of("rating", 5);

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch("/api/transitions/{id}", transitionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(transitionId.intValue()))
                .body("rating", equalTo(5))
                .body("notes", equalTo("Original notes"))
                .body("style", equalTo("blend"));
    }

    @Test
    void deleteTransitionReturns204() {
        // GIVEN an existing transition
        Long trackA = createTrack("Track A", "Artist A", "Techno", null, null);
        Long trackB = createTrack("Track B", "Artist B", "Techno", null, null);

        Number transitionId = createTransition(trackA, trackB, 4, null, null);

        // WHEN deleting the transition
        given()
                .when()
                .delete("/api/transitions/{id}", transitionId)
                .then()
                .statusCode(204);

        // THEN it can no longer be found
        given()
                .when()
                .get("/api/transitions/{id}", transitionId)
                .then()
                .statusCode(404);
    }

    @Test
    void recordTransitionPlayIncrementsCounter() {
        // GIVEN an existing transition
        Long trackA = createTrack("Track A", "Artist A", "Techno", null, null);
        Long trackB = createTrack("Track B", "Artist B", "Techno", null, null);

        Number transitionId = createTransition(trackA, trackB, 5, null, null);

        // WHEN recording a play
        given()
                .when()
                .post("/api/transitions/{id}/record-play", transitionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(transitionId.intValue()))
                .body("timesPlayed", equalTo(1))
                .body("lastPlayedDate", notNullValue());

        // AND recording another play
        given()
                .when()
                .post("/api/transitions/{id}/record-play", transitionId)
                .then()
                .statusCode(200)
                .body("timesPlayed", equalTo(2));
    }

    @Test
    void getTransitionByIdReturnsNotFoundForMissingId() {
        given()
                .when()
                .get("/api/transitions/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Transition not found"));
    }

    // --- Private helper methods ---

    /**
     * Helper to create a transition via the API and return its ID.
     * Reduces boilerplate in tests that need multiple transitions.
     */
    private Number createTransition(Long sourceTrackId, Long targetTrackId,
                                     Integer rating, String notes, String style) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("sourceTrackId", sourceTrackId);
        payload.put("targetTrackId", targetTrackId);
        payload.put("rating", rating);
        if (notes != null) payload.put("notes", notes);
        if (style != null) payload.put("style", style);

        return given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
