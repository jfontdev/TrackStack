package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Track API.
 *
 * <p>These tests exercise the full Controller -> Service -> Repository -> DB
 * flow using real infrastructure via Testcontainers. They cover creation,
 * retrieval, full update (PUT), partial update (PATCH), deletion, and
 * tag relationship management.</p>
 */
public class TrackControllerIntegrationTest extends BaseIntegrationTest {

    /**
     * Verifies track creation, retrieval by ID, and listing all tracks.
     * Also verifies the response includes an empty tags list for a new track.
     */
    @Test
    void createTrackThenGetByIdAndList() {
        // GIVEN a valid track request
        Map<String, Object> payload = Map.of(
                "title", "Night Drive",
                "artist", "Nova",
                "bpm", 128.0,
                "key", "A minor",
                "duration", "3:45");

        // WHEN the track is created
        Number id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/tracks")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Night Drive"))
                .body("tags", hasSize(0))
                .extract()
                .path("id");

        long trackId = id.longValue();

        // THEN it can be retrieved by id
        given()
                .when()
                .get("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) trackId))
                .body("title", equalTo("Night Drive"));

        // THEN it appears in the list endpoint
        given()
                .when()
                .get("/api/tracks")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", equalTo((int) trackId));
    }

    /**
     * Verifies a missing track id returns a 404 with an error payload.
     */
    @Test
    void getTrackByIdReturnsNotFoundForMissingId() {
        // GIVEN a track id that does not exist
        long missingId = 9999L;

        // WHEN the track is requested
        // THEN a 404 error is returned
        given()
                .when()
                .get("/api/tracks/{id}", missingId)
                .then()
                .statusCode(404)
                .body("error", equalTo("Track not found"));
    }

    /**
     * Verifies full update (PUT) replaces all fields and returns 200.
     */
    @Test
    void updateTrackReturns200WithUpdatedData() {
        // GIVEN an existing track
        long trackId = createTrack("Original", "Artist A", 120.0, "C major", "3:00");

        // WHEN the track is fully updated
        Map<String, Object> updatePayload = Map.of(
                "title", "Updated Title",
                "artist", "Artist B",
                "bpm", 140.0,
                "key", "D minor",
                "duration", "4:30");

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) trackId))
                .body("title", equalTo("Updated Title"))
                .body("artist", equalTo("Artist B"))
                .body("bpm", equalTo(140.0f))
                .body("key", equalTo("D minor"))
                .body("duration", equalTo("4:30"));
    }

    /**
     * Verifies PUT on a non-existent track returns 404.
     */
    @Test
    void updateTrackReturns404ForMissingId() {
        Map<String, Object> updatePayload = Map.of(
                "title", "Updated",
                "artist", "Someone",
                "duration", "3:00");

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/api/tracks/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Track not found"));
    }

    /**
     * Verifies partial update (PATCH) only changes provided fields.
     */
    @Test
    void patchTrackReturns200WithPartialUpdate() {
        // GIVEN an existing track
        long trackId = createTrack("Original", "Artist A", 120.0, "C major", "3:00");

        // WHEN only the title is patched
        Map<String, Object> patchPayload = Map.of("title", "Patched Title");

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) trackId))
                .body("title", equalTo("Patched Title"))
                .body("artist", equalTo("Artist A"))
                .body("bpm", equalTo(120.0f))
                .body("key", equalTo("C major"))
                .body("duration", equalTo("3:00"));
    }

    /**
     * Verifies DELETE returns 204 and the track is gone.
     */
    @Test
    void deleteTrackReturns204() {
        // GIVEN an existing track
        long trackId = createTrack("To Delete", "Artist", 100.0, "E minor", "2:30");

        // WHEN the track is deleted
        given()
                .when()
                .delete("/api/tracks/{id}", trackId)
                .then()
                .statusCode(204);

        // THEN it no longer exists
        given()
                .when()
                .get("/api/tracks/{id}", trackId)
                .then()
                .statusCode(404);
    }

    /**
     * Verifies DELETE on a non-existent track returns 404.
     */
    @Test
    void deleteTrackReturns404ForMissingId() {
        given()
                .when()
                .delete("/api/tracks/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Track not found"));
    }

    /**
     * Verifies adding a tag to a track and then removing it.
     */
    @Test
    void addAndRemoveTagFromTrack() {
        // GIVEN an existing track and tag
        long trackId = createTrack("Tagged Track", "Artist", 128.0, "A minor", "3:30");
        long tagId = createTag("Electronic");

        // WHEN the tag is added to the track
        given()
                .when()
                .put("/api/tracks/{id}/tags/{tagId}", trackId, tagId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) trackId))
                .body("tags", hasSize(1))
                .body("tags[0].id", equalTo((int) tagId))
                .body("tags[0].name", equalTo("Electronic"));

        // THEN retrieving the track shows the tag
        given()
                .when()
                .get("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200)
                .body("tags", hasSize(1));

        // WHEN the tag is removed from the track
        given()
                .when()
                .delete("/api/tracks/{id}/tags/{tagId}", trackId, tagId)
                .then()
                .statusCode(200)
                .body("tags", hasSize(0));
    }

    // ==================== Helper methods ====================

    /**
     * Creates a track via the API and returns its ID.
     */
    private long createTrack(String title, String artist, Double bpm, String key, String duration) {
        Map<String, Object> payload = Map.of(
                "title", title,
                "artist", artist,
                "bpm", bpm,
                "key", key,
                "duration", duration);

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

    /**
     * Creates a tag via the API and returns its ID.
     */
    private long createTag(String name) {
        Number id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when()
                .post("/api/tags")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        return id.longValue();
    }
}
