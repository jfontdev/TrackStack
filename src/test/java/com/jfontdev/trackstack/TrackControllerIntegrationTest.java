package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
 * partial update (PATCH), deletion, and tag relationship management.
 * </p>
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
                                "duration", "3:45",
                                "genre", "Synthwave");

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
                                .body("title", equalTo("Night Drive"))
                                .body("genre", equalTo("Synthwave"));

                // THEN it appears in the list endpoint
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
                                "duration", "4:30",
                                "genre", "Techno");

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
                                .body("duration", equalTo("4:30"))
                                .body("genre", equalTo("Techno"));
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

        /**
         * Verifies paginated list responses use the configured defaults and
         * deterministic
         * title sorting when no explicit sort is provided.
         */
        @Test
        void getTracksReturnsPagedResultsWithDefaultSorting() {
                createTrack("Zeta", "Artist Z", 130.0, "G minor", "03:30", "Trance");
                createTrack("Alpha", "Artist A", 120.0, "A minor", "03:10", "House");
                createTrack("Beta", "Artist B", 125.0, "B minor", "03:20", "House");

                given()
                                .queryParam("page", 0)
                                .queryParam("size", 2)
                                .when()
                                .get("/api/tracks")
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(2))
                                .body("content[0].title", equalTo("Alpha"))
                                .body("content[1].title", equalTo("Beta"))
                                .body("page.number", equalTo(0))
                                .body("page.size", equalTo(2))
                                .body("page.totalElements", equalTo(3))
                                .body("page.totalPages", equalTo(2));
        }

        /**
         * Verifies filtering by BPM range, key, and genre on the paginated list
         * endpoint.
         */
        @Test
        void getTracksSupportsFilteringByBpmKeyAndGenre() {
                createTrack("Pulse One", "Artist A", 122.0, "A minor", "03:15", "House");
                createTrack("Pulse Two", "Artist B", 128.0, "A minor", "03:45", "House");
                createTrack("Rock Edge", "Artist C", 121.0, "E minor", "04:10", "Rock");

                given()
                                .queryParam("bpmMin", 120.0)
                                .queryParam("bpmMax", 125.0)
                                .queryParam("key", "A minor")
                                .queryParam("genre", "house")
                                .when()
                                .get("/api/tracks")
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(1))
                                .body("content[0].title", equalTo("Pulse One"))
                                .body("content[0].genre", equalTo("House"));
        }

        /**
         * Verifies that repeated paged reads do not fail with serialization errors.
         */
        @Test
        void getTracksRepeatedPagedReadsDoNotFailWithSerializationErrors() {
                createTrack("Cached Pulse", "Artist C", 124.0, "C minor", "03:33", "House");

                given()
                                .queryParam("page", 0)
                                .queryParam("size", 20)
                                .queryParam("sort", "title,asc")
                                .when()
                                .get("/api/tracks")
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(1));

                given()
                                .queryParam("page", 0)
                                .queryParam("size", 20)
                                .queryParam("sort", "title,asc")
                                .when()
                                .get("/api/tracks")
                                .then()
                                .statusCode(200)
                                .body("content", hasSize(1));
        }

        /**
         * Verifies invalid BPM ranges return 400 Bad Request.
         */
        @Test
        void getTracksReturns400ForInvalidBpmRange() {
                given()
                                .queryParam("bpmMin", 130.0)
                                .queryParam("bpmMax", 120.0)
                                .when()
                                .get("/api/tracks")
                                .then()
                                .statusCode(400)
                                .body("error", equalTo("bpmMin must be less than or equal to bpmMax."));
        }

        /**
         * Verifies unsupported sort fields return 400 Bad Request.
         */
        @Test
        void getTracksReturns400ForUnsupportedSortField() {
                given()
                                .queryParam("sort", "album,asc")
                                .when()
                                .get("/api/tracks")
                                .then()
                                .statusCode(400)
                                .body("error", containsString("Unsupported sort field"));
        }

        /**
         * Verifies invalid page sizes return 400 Bad Request.
         */
        @Test
        void getTracksReturns400ForInvalidPageSize() {
                given()
                                .queryParam("size", 0)
                                .when()
                                .get("/api/tracks")
                                .then()
                                .statusCode(400)
                                .body("error", equalTo("Size must be between 1 and 100."));
        }

        // ==================== Validation Tests ====================

        @Test
        void createTrackWithMissingFieldsReturns400() {
                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of())
                                .when()
                                .post("/api/tracks")
                                .then()
                                .statusCode(400)
                                .body("errors.title", equalTo("Title must not be empty"))
                                .body("errors.artist", equalTo("Artist must not be empty"))
                                .body("errors.duration", equalTo("Duration must not be empty"));
        }

        @Test
        void createTrackWithInvalidDurationReturns400() {
                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "title", "Track 1",
                                                "artist", "Artist 1",
                                                "duration", "5:5" // Invalid format, expects mm:ss
                                ))
                                .when()
                                .post("/api/tracks")
                                .then()
                                .statusCode(400)
                                .body("errors.duration", equalTo("Duration must be in mm:ss format"));
        }

        @Test
        void createTrackWithNegativeBpmReturns400() {
                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "title", "Track 1",
                                                "artist", "Artist 1",
                                                "duration", "05:05",
                                                "bpm", -120.0))
                                .when()
                                .post("/api/tracks")
                                .then()
                                .statusCode(400)
                                .body("errors.bpm", equalTo("BPM must be positive if provided"));
        }

        @Test
        void createTrackWithEmptyGenreReturns400() {
                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "title", "Track 1",
                                                "artist", "Artist 1",
                                                "duration", "05:05",
                                                "genre", ""))
                                .when()
                                .post("/api/tracks")
                                .then()
                                .statusCode(400)
                                .body("errors.genre", equalTo("Genre must not be empty if provided"));
        }

        @Test
        void updateTrackWithEmptyTitleReturns400() {
                long trackId = createTrack("Original Title", "Artist", 120.0, "Am", "03:30");

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "title", "   ",
                                                "artist", "Artist",
                                                "duration", "03:30"))
                                .when()
                                .put("/api/tracks/{id}", trackId)
                                .then()
                                .statusCode(400)
                                .body("errors.title", equalTo("Title must not be empty"));
        }

        @Test
        void patchTrackWithInvalidDurationReturns400() {
                long trackId = createTrack("Original Title", "Artist", 120.0, "Am", "03:30");

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of("duration", "abc"))
                                .when()
                                .patch("/api/tracks/{id}", trackId)
                                .then()
                                .statusCode(400)
                                .body("errors.duration", equalTo("Duration must be in mm:ss format if provided"));
        }

        @Test
        void patchTrackWithEmptyTitleReturns400() {
                long trackId = createTrack("Original Title", "Artist", 120.0, "Am", "03:30");

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of("title", ""))
                                .when()
                                .patch("/api/tracks/{id}", trackId)
                                .then()
                                .statusCode(400)
                                .body("errors.title", equalTo("Title must not be empty if provided"));
        }
        // ==================== Helper methods ====================

        /**
         * Creates a track via the API and returns its ID.
         */
        private long createTrack(String title, String artist, Double bpm, String key, String duration) {
                return createTrack(title, artist, bpm, key, duration, null);
        }

        /**
         * Creates a track via the API and returns its ID.
         */
        private long createTrack(String title, String artist, Double bpm, String key, String duration, String genre) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("title", title);
                payload.put("artist", artist);
                payload.put("duration", duration);

                if (bpm != null) {
                        payload.put("bpm", bpm);
                }

                if (key != null) {
                        payload.put("key", key);
                }

                if (genre != null) {
                        payload.put("genre", genre);
                }

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
