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
 * database configured through Testcontainers.</p>
 */
public class PlaylistControllerIntegrationTest extends BaseIntegrationTest {

    /**
     * Verifies playlist creation, retrieval by ID, and listing all playlists.
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
}
