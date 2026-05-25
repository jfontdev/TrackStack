package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Track API.
 *
 * <p>
 * These tests exercise the full Controller -> Service -> Repository -> DB
 * flow using real infrastructure via Testcontainers. They cover creation,
 * retrieval, paginated listing, sorting/filtering, full update (PUT),
 * partial update (PATCH), and deletion.
 * </p>
 */
public class TrackControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void createTrackThenGetByIdAndList() {
        Map<String, Object> payload = Map.of(
                "title", "Night Drive",
                "artist", "Nova",
                "bpm", 128.0,
                "key", "A minor",
                "durationSeconds", 225,
                "genre", "Synthwave",
                "filePath", "/music/test/night-drive.mp3",
                "fileFormat", "mp3",
                "bitrate", 320,
                "energy", 4);

        Number id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/tracks")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Night Drive"))
                .body("genre", equalTo("Synthwave"))
                .body("filePath", equalTo("/music/test/night-drive.mp3"))
                .extract()
                .path("id");

        long trackId = id.longValue();

        given()
                .when()
                .get("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) trackId))
                .body("title", equalTo("Night Drive"))
                .body("durationSeconds", equalTo(225));

        given()
                .when()
                .get("/api/tracks")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].id", equalTo((int) trackId))
                .body("page.number", equalTo(0))
                .body("page.size", equalTo(20));
    }

    @Test
    void getTrackByIdReturnsNotFoundForMissingId() {
        given()
                .when()
                .get("/api/tracks/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Track not found"));
    }

    @Test
    void updateTrackReturns200WithUpdatedData() {
        Map<String, Object> createPayload = Map.of(
                "title", "Old Title",
                "artist", "Old Artist",
                "filePath", "/music/test/old.mp3");

        Number id = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post("/api/tracks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        long trackId = id.longValue();

        Map<String, Object> updatePayload = Map.of(
                "title", "New Title",
                "artist", "New Artist",
                "bpm", 140.0,
                "key", "11B",
                "durationSeconds", 300,
                "genre", "Techno",
                "filePath", "/music/test/new.mp3",
                "fileFormat", "flac",
                "bitrate", 1411,
                "energy", 5);

        given()
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) trackId))
                .body("title", equalTo("New Title"))
                .body("bpm", equalTo(140.0f))
                .body("genre", equalTo("Techno"));
    }

    @Test
    void patchTrackUpdatesOnlyProvidedFields() {
        Map<String, Object> createPayload = Map.of(
                "title", "Original",
                "artist", "Artist",
                "filePath", "/music/test/original.mp3");

        Number id = given()
                .contentType(ContentType.JSON)
                .body(createPayload)
                .when()
                .post("/api/tracks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        long trackId = id.longValue();

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
                .body("artist", equalTo("Artist"));
    }

    @Test
    void deleteTrackReturns204() {
        Map<String, Object> payload = Map.of(
                "title", "To Delete",
                "artist", "Artist",
                "filePath", "/music/test/delete.mp3");

        Number id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/tracks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        long trackId = id.longValue();

        given()
                .when()
                .delete("/api/tracks/{id}", trackId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/tracks/{id}", trackId)
                .then()
                .statusCode(404);
    }

    @Test
    void listTracksWithBpmFilter() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Fast Track",
                        "artist", "Artist",
                        "bpm", 140.0,
                        "filePath", "/music/test/fast.mp3"))
                .when()
                .post("/api/tracks");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Slow Track",
                        "artist", "Artist",
                        "bpm", 90.0,
                        "filePath", "/music/test/slow.mp3"))
                .when()
                .post("/api/tracks");

        given()
                .when()
                .get("/api/tracks?bpmMin=100&bpmMax=150")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].title", equalTo("Fast Track"));
    }
}
