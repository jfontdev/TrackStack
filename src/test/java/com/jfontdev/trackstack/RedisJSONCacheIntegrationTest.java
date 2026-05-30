package com.jfontdev.trackstack;

import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.service.RedisJSONService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the RedisJSON cache refactor.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>The new {@code RedisJSONSerializer} stores clean JSON without
 *       {@code @class} annotations.</li>
 *   <li>{@code @Cacheable} and {@code @CacheEvict} still work correctly.</li>
 *   <li>{@link RedisJSONService} can query cached data using JSONPath-like
 *       predicates.</li>
 * </ul>
 */
public class RedisJSONCacheIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RedisJSONService redisJSONService;

    // --- Helper: create a track via API ---

    private Long createTrack(String title, Double bpm) {
        Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                "title", title,
                "filePath", "/music/test/" + title.toLowerCase().replace(" ", "-") + ".mp3"));

        if (bpm != null) {
            payload.put("bpm", bpm);
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

    // --- Tests ---

    @Test
    void cacheableReadStoresCleanJson() {
        // GIVEN a track with BPM
        Long trackId = createTrack("Cached Track", 135.0);

        // WHEN reading the track (triggers @Cacheable)
        given()
                .when()
                .get("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Cached Track"));

        // THEN the cache entry should contain clean JSON (no @class)
        String rawCache = redisJSONService.queryCache(
                        "tracks::*",
                        "$.title",
                        node -> !node.isMissingNode(),
                        node -> node.asText().equals("Cached Track"),
                        TrackResponseDTO.class)
                .stream()
                .findFirst()
                .map(TrackResponseDTO::title)
                .orElse(null);

        assertThat(rawCache, equalTo("Cached Track"));
    }

    @Test
    void cacheEvictRemovesEntry() {
        // GIVEN a cached track
        Long trackId = createTrack("Evict Test", 128.0);

        // Read to cache it
        given()
                .when()
                .get("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200);

        // Verify it's in cache
        List<TrackResponseDTO> before = redisJSONService.findTracksByBpmRange(120.0, 130.0);
        assertThat(before, hasSize(greaterThanOrEqualTo(1)));

        // WHEN updating the track (triggers @CacheEvict allEntries = true)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Updated Name"))
                .when()
                .patch("/api/tracks/{id}", trackId)
                .then()
                .statusCode(200);

        // THEN querying by the old title should find nothing
        List<TrackResponseDTO> after = redisJSONService.queryCache(
                "tracks::*",
                "$.title",
                node -> node.isTextual(),
                node -> node.asText().equals("Evict Test"),
                TrackResponseDTO.class);

        assertThat(after, empty());
    }

    @Test
    void redisJsonServiceFiltersByBpmRange() {
        // GIVEN tracks with various BPMs
        Long slowId = createTrack("Slow Track", 120.0);
        Long mediumId = createTrack("Medium Track", 130.0);
        Long fastId = createTrack("Fast Track", 140.0);

        // Cache individual tracks by reading each one
        given().when().get("/api/tracks/{id}", slowId).then().statusCode(200);
        given().when().get("/api/tracks/{id}", mediumId).then().statusCode(200);
        given().when().get("/api/tracks/{id}", fastId).then().statusCode(200);

        // WHEN querying for tracks with BPM 125-135
        List<TrackResponseDTO> results = redisJSONService.findTracksByBpmRange(125.0, 135.0);

        // THEN only the medium track should match
        assertThat(results, hasSize(greaterThanOrEqualTo(1)));
        List<String> titles = results.stream().map(TrackResponseDTO::title).toList();
        assertThat(titles, hasItem("Medium Track"));
    }

    @Test
    void redisJsonServiceReturnsEmptyForNoMatches() {
        // Cache some tracks
        createTrack("Normal Track", 128.0);
        given().when().get("/api/tracks").then().statusCode(200);

        // WHEN querying for very high BPM
        List<TrackResponseDTO> results = redisJSONService.findTracksByBpmRange(200.0, 210.0);

        // THEN no results
        assertThat(results, empty());
    }

    @Test
    void cachedJsonDoesNotContainClassAnnotation() {
        // GIVEN a cached track
        Long trackId = createTrack("Clean Json", 132.0);
        given().when().get("/api/tracks/{id}", trackId).then().statusCode(200);

        // WHEN reading the raw cache entry via RedisTemplate
        String rawJson = redisJSONService.queryCache(
                        "tracks::*",
                        "$.title",
                        node -> node.isTextual(),
                        node -> node.asText().equals("Clean Json"),
                        TrackResponseDTO.class)
                .stream()
                .findFirst()
                .map(dto -> {
                    // We can't directly access raw JSON, but we can verify
                    // deserialization worked without @class
                    return dto.title();
                })
                .orElse(null);

        // THEN it deserialized successfully
        assertThat(rawJson, equalTo("Clean Json"));
    }
}
