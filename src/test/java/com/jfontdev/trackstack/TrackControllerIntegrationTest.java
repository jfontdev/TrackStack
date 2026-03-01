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
 * flow using real infrastructure via Testcontainers.</p>
 */
public class TrackControllerIntegrationTest extends BaseIntegrationTest {

    /**
     * Verifies track creation, retrieval by ID, and listing all tracks.
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
}
