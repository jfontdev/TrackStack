package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Playlist API.
 *
 * <p>These tests ensure playlist endpoints work end-to-end against a real
 * database configured through Testcontainers. They cover creation, retrieval,
 * full update (PUT), partial update (PATCH), deletion, and track relationship
 * management.</p>
 */
public class PlaylistControllerIntegrationTest extends BaseIntegrationTest {

    /**
     * Verifies playlist creation, retrieval by ID, and listing all playlists.
     * Also verifies the response includes an empty tracks list for a new playlist.
     */
    @Test
    void createPlaylistThenGetByIdAndList() {
        // GIVEN a valid playlist request
        Map<String, Object> payload = Map.of(
                "name", "Warmup Set",
                "description", "Groovy openers for the night");

        // WHEN the playlist is created
        Number id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/playlists")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Warmup Set"))
                .body("tracks", hasSize(0))
                .extract()
                .path("id");

        long playlistId = id.longValue();

        // THEN it can be retrieved by id
        given()
                .when()
                .get("/api/playlists/{id}", playlistId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) playlistId))
                .body("name", equalTo("Warmup Set"));

        // THEN it appears in the list endpoint
        given()
                .when()
                .get("/api/playlists")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", equalTo((int) playlistId));
    }

    /**
     * Verifies a missing playlist id returns a 404 with an error payload.
     */
    @Test
    void getPlaylistByIdReturnsNotFoundForMissingId() {
        // GIVEN a playlist id that does not exist
        long missingId = 9999L;

        // WHEN the playlist is requested
        // THEN a 404 error is returned
        given()
                .when()
                .get("/api/playlists/{id}", missingId)
                .then()
                .statusCode(404)
                .body("error", equalTo("Playlist not found."));
    }

    /**
     * Verifies full update (PUT) replaces all fields and returns 200.
     */
    @Test
    void updatePlaylistReturns200WithUpdatedData() {
        // GIVEN an existing playlist
        long playlistId = createPlaylist("Original", "Original description");

        // WHEN the playlist is fully updated
        Map<String, Object> updatePayload = Map.of(
                "name", "Updated Set",
                "description", "New description");

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/api/playlists/{id}", playlistId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) playlistId))
                .body("name", equalTo("Updated Set"))
                .body("description", equalTo("New description"));
    }

    /**
     * Verifies PUT on a non-existent playlist returns 404.
     */
    @Test
    void updatePlaylistReturns404ForMissingId() {
        Map<String, Object> updatePayload = Map.of(
                "name", "Updated",
                "description", "Desc");

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/api/playlists/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Playlist not found."));
    }

    /**
     * Verifies partial update (PATCH) only changes provided fields.
     */
    @Test
    void patchPlaylistReturns200WithPartialUpdate() {
        // GIVEN an existing playlist
        long playlistId = createPlaylist("Original", "Original description");

        // WHEN only the name is patched
        Map<String, Object> patchPayload = Map.of("name", "Patched Set");

        given()
                .contentType(ContentType.JSON)
                .body(patchPayload)
                .when()
                .patch("/api/playlists/{id}", playlistId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) playlistId))
                .body("name", equalTo("Patched Set"))
                .body("description", equalTo("Original description"));
    }

    /**
     * Verifies DELETE returns 204 and the playlist is gone.
     */
    @Test
    void deletePlaylistReturns204() {
        // GIVEN an existing playlist
        long playlistId = createPlaylist("To Delete", "Will be deleted");

        // WHEN the playlist is deleted
        given()
                .when()
                .delete("/api/playlists/{id}", playlistId)
                .then()
                .statusCode(204);

        // THEN it no longer exists
        given()
                .when()
                .get("/api/playlists/{id}", playlistId)
                .then()
                .statusCode(404);
    }

    /**
     * Verifies DELETE on a non-existent playlist returns 404.
     */
    @Test
    void deletePlaylistReturns404ForMissingId() {
        given()
                .when()
                .delete("/api/playlists/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Playlist not found."));
    }

    /**
     * Verifies adding a track to a playlist and then removing it.
     */
    @Test
    void addAndRemoveTrackFromPlaylist() {
        // GIVEN an existing playlist and track
        long playlistId = createPlaylist("My Playlist", "Test playlist");
        long trackId = createTrack("Test Track", "Artist", 128.0, "A minor", "3:30");

        // WHEN the track is added to the playlist
        given()
                .when()
                .put("/api/playlists/{id}/tracks/{trackId}", playlistId, trackId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) playlistId))
                .body("tracks", hasSize(1))
                .body("tracks[0].id", equalTo((int) trackId))
                .body("tracks[0].title", equalTo("Test Track"));

        // THEN retrieving the playlist shows the track
        given()
                .when()
                .get("/api/playlists/{id}", playlistId)
                .then()
                .statusCode(200)
                .body("tracks", hasSize(1));

        // WHEN the track is removed from the playlist
        given()
                .when()
                .delete("/api/playlists/{id}/tracks/{trackId}", playlistId, trackId)
                .then()
                .statusCode(200)
                .body("tracks", hasSize(0));
    }

    /**
     * Verifies that deleting a track also removes it from the playlist
     * (via ON DELETE CASCADE on the join table foreign key).
     */
    @Test
    void deletingTrackRemovesItFromPlaylist() {
        // GIVEN a playlist with a track
        long playlistId = createPlaylist("Cascade Test", "Testing cascade");
        long trackId = createTrack("Cascade Track", "Artist", 130.0, "B minor", "4:00");

        given()
                .when()
                .put("/api/playlists/{id}/tracks/{trackId}", playlistId, trackId)
                .then()
                .statusCode(200)
                .body("tracks", hasSize(1));

        // WHEN the track is deleted directly
        given()
                .when()
                .delete("/api/tracks/{id}", trackId)
                .then()
                .statusCode(204);

        // THEN the playlist no longer contains the track
        given()
                .when()
                .get("/api/playlists/{id}", playlistId)
                .then()
                .statusCode(200)
                .body("tracks", hasSize(0));
    }

    // ==================== Helper methods ====================

    /**
     * Creates a playlist via the API and returns its ID.
     */
    private long createPlaylist(String name, String description) {
        Number id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name, "description", description))
                .when()
                .post("/api/playlists")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        return id.longValue();
    }

    /**
     * Creates a track via the API and returns its ID.
     */
    private long createTrack(String title, String artist, Double bpm, String key, String duration) {
        Number id = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", title,
                        "artist", artist,
                        "bpm", bpm,
                        "key", key,
                        "duration", duration))
                .when()
                .post("/api/tracks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        return id.longValue();
    }
}
