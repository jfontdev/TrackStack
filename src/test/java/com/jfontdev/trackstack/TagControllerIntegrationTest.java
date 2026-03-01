package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Tag API.
 *
 * <p>These tests validate end-to-end behavior across HTTP, service logic,
 * repository access, and the database.</p>
 */
public class TagControllerIntegrationTest extends BaseIntegrationTest {

    /**
     * Verifies tag creation, retrieval by ID, and listing all tags.
     */
    @Test
    void createTagThenGetByIdAndList() {
        // GIVEN a valid tag request
        Map<String, Object> payload = Map.of("name", "House");

        // WHEN the tag is created
        Number id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/tags")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("House"))
                .extract()
                .path("id");

        long tagId = id.longValue();

        // THEN it can be retrieved by id
        given()
                .when()
                .get("/api/tags/{id}", tagId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) tagId))
                .body("name", equalTo("House"));

        // THEN it appears in the list endpoint
        given()
                .when()
                .get("/api/tags")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", equalTo((int) tagId));
    }

    /**
     * Verifies a missing tag id returns a 404 with an error payload.
     */
    @Test
    void getTagByIdReturnsNotFoundForMissingId() {
        // GIVEN a tag id that does not exist
        long missingId = 9999L;

        // WHEN the tag is requested
        // THEN a 404 error is returned
        given()
                .when()
                .get("/api/tags/{id}", missingId)
                .then()
                .statusCode(404)
                .body("error", equalTo("Tag not found."));
    }
}
