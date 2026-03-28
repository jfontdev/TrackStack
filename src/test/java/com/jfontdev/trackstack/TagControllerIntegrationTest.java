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
 * repository access, and the database. They cover creation, retrieval,
 * full update (PUT), partial update (PATCH), deletion, and unique constraint
 * violation handling.</p>
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

    /**
     * Verifies full update (PUT) replaces the tag name and returns 200.
     */
    @Test
    void updateTagReturns200WithUpdatedData() {
        // GIVEN an existing tag
        long tagId = createTag("Original");

        // WHEN the tag is fully updated
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Updated"))
                .when()
                .put("/api/tags/{id}", tagId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) tagId))
                .body("name", equalTo("Updated"));
    }

    /**
     * Verifies PUT on a non-existent tag returns 404.
     */
    @Test
    void updateTagReturns404ForMissingId() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Updated"))
                .when()
                .put("/api/tags/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Tag not found."));
    }

    /**
     * Verifies partial update (PATCH) changes only provided fields.
     */
    @Test
    void patchTagReturns200WithPartialUpdate() {
        // GIVEN an existing tag
        long tagId = createTag("Original");

        // WHEN only the name is patched
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Patched"))
                .when()
                .patch("/api/tags/{id}", tagId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) tagId))
                .body("name", equalTo("Patched"));
    }

    /**
     * Verifies DELETE returns 204 and the tag is gone.
     */
    @Test
    void deleteTagReturns204() {
        // GIVEN an existing tag
        long tagId = createTag("ToDelete");

        // WHEN the tag is deleted
        given()
                .when()
                .delete("/api/tags/{id}", tagId)
                .then()
                .statusCode(204);

        // THEN it no longer exists
        given()
                .when()
                .get("/api/tags/{id}", tagId)
                .then()
                .statusCode(404);
    }

    /**
     * Verifies DELETE on a non-existent tag returns 404.
     */
    @Test
    void deleteTagReturns404ForMissingId() {
        given()
                .when()
                .delete("/api/tags/{id}", 9999L)
                .then()
                .statusCode(404)
                .body("error", equalTo("Tag not found."));
    }

    /**
     * Verifies that updating a tag to a duplicate name returns 409 Conflict.
     * <p>
     * The {@code tags.name} column has a UNIQUE constraint in the database.
     * Attempting to rename a tag to a name that already exists must trigger
     * a {@code DataIntegrityViolationException}, which our global exception
     * handler maps to HTTP 409.
     */
    @Test
    void updateTagWithDuplicateNameReturns409() {
        // GIVEN two existing tags
        createTag("Electronic");
        long secondTagId = createTag("Ambient");

        // WHEN the second tag is updated to the first tag's name
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Electronic"))
                .when()
                .put("/api/tags/{id}", secondTagId)
                .then()
                .statusCode(409)
                .body("error", notNullValue());
    }

    /**
     * Verifies that creating a tag with a duplicate name returns 409 Conflict.
     */
    @Test
    void createTagWithDuplicateNameReturns409() {
        // GIVEN an existing tag
        createTag("Electronic");

        // WHEN another tag with the same name is created
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Electronic"))
                .when()
                .post("/api/tags")
                .then()
                .statusCode(409)
                .body("error", notNullValue());
    }

    // ==================== Helper methods ====================

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
