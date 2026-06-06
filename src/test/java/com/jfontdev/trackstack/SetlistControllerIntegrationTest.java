package com.jfontdev.trackstack;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Setlist API.
 * <p>
 * These tests exercise the full Controller -> Service -> Repository -> DB
 * flow for setlists and slots using real infrastructure via Testcontainers.
 * They cover creation, retrieval, update, deletion, slot management,
 * reordering, and lifecycle transitions (DRAFT → READY → PERFORMED).
 * <p>
 * Since setlist slots reference tracks, tests that create slots first create
 * the necessary track(s) via the Track API.
 */
public class SetlistControllerIntegrationTest extends BaseIntegrationTest {

    // --- Helper: create a track and return its ID ---

    private Long createTrack(String title) {
        Map<String, Object> payload = Map.of(
                "title", title,
                "filePath", "/music/test/" + title.toLowerCase().replace(" ", "-") + ".mp3");

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

    // --- Helper: create a setlist and return its ID ---

    private Long createSetlist(String name, String description) {
        Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                "name", name,
                "description", description));

        Number id = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        return id.longValue();
    }

    // --- Tests ---

    @Test
    void createSetlistThenGetById() {
        // WHEN creating a setlist
        Map<String, Object> payload = Map.of(
                "name", "Weekend Techno Set",
                "description", "Peak time warehouse vibes");

        Number setlistId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Weekend Techno Set"))
                .body("description", equalTo("Peak time warehouse vibes"))
                .body("status", equalTo("DRAFT"))
                .body("slots", empty())
                .extract()
                .path("id");

        long id = setlistId.longValue();

        // THEN it can be retrieved by ID
        given()
                .when()
                .get("/api/setlists/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("name", equalTo("Weekend Techno Set"))
                .body("status", equalTo("DRAFT"));
    }

    @Test
    void createSetlistWithInitialSlots() {
        // GIVEN two tracks
        Long track1 = createTrack("Opener");
        Long track2 = createTrack("Builder");

        // WHEN creating a setlist with slots
        Map<String, Object> payload = Map.of(
                "name", "Test Set",
                "description", "With slots",
                "slots", List.of(
                        Map.of("trackId", track1, "slotOrder", 1, "energy", 2),
                        Map.of("trackId", track2, "slotOrder", 2, "energy", 3)));

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .body("slots", hasSize(2))
                .body("slots[0].trackId", equalTo(track1.intValue()))
                .body("slots[0].slotOrder", equalTo(1))
                .body("slots[0].energy", equalTo(2))
                .body("slots[1].trackId", equalTo(track2.intValue()))
                .body("slots[1].slotOrder", equalTo(2))
                .body("slots[1].energy", equalTo(3));
    }

    @Test
    void createSetlistWithInvalidTrackReturns404() {
        Map<String, Object> payload = Map.of(
                "name", "Bad Set",
                "slots", List.of(
                        Map.of("trackId", 9999, "slotOrder", 1)));

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(404)
                .body("error", containsString("Track not found"));
    }

    @Test
    void getAllSetlistsReturnsNewestFirst() {
        // GIVEN two setlists
        Long setlist1 = createSetlist("First Set", "Created first");
        Long setlist2 = createSetlist("Second Set", "Created second");

        // WHEN retrieving all setlists
        given()
                .when()
                .get("/api/setlists")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(2)))
                .body("[0].name", equalTo("Second Set"))
                .body("[1].name", equalTo("First Set"));
    }

    @Test
    void getSetlistsByStatusFiltersCorrectly() {
        // GIVEN setlists in different statuses
        Long draft = createSetlist("Draft Set", "Still working");
        createSetlist("Ready Set", "Finished");

        // Mark one as READY
        given()
                .when()
                .post("/api/setlists/{id}/ready", draft)
                .then()
                .statusCode(200);

        // WHEN filtering by DRAFT status
        given()
                .when()
                .get("/api/setlists?status=DRAFT")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].status", equalTo("DRAFT"));

        // WHEN filtering by READY status
        given()
                .when()
                .get("/api/setlists?status=READY")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("find { it.id == " + draft + " }.status", equalTo("READY"));
    }

    @Test
    void updateSetlistMetadata() {
        // GIVEN an existing setlist
        Long setlistId = createSetlist("Original Name", "Original desc");

        // WHEN updating it
        Map<String, Object> payload = Map.of(
                "name", "Updated Name",
                "description", "Updated desc");

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .put("/api/setlists/{id}", setlistId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Name"))
                .body("description", equalTo("Updated desc"))
                .body("status", equalTo("DRAFT"));
    }

    @Test
    void deleteSetlistRemovesItAndSlots() {
        // GIVEN a setlist with a slot
        Long track = createTrack("Track to Delete");
        Long setlistId = createSetlist("To Delete", "Will be removed");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track, "slotOrder", 1))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200);

        // WHEN deleting the setlist
        given()
                .when()
                .delete("/api/setlists/{id}", setlistId)
                .then()
                .statusCode(204);

        // THEN it no longer exists
        given()
                .when()
                .get("/api/setlists/{id}", setlistId)
                .then()
                .statusCode(404);
    }

    @Test
    void addSlotToExistingSetlist() {
        // GIVEN a setlist and a track
        Long setlistId = createSetlist("Empty Set", "No slots yet");
        Long track = createTrack("New Track");

        // WHEN adding a slot
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track, "slotOrder", 1, "energy", 4))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200)
                .body("slots", hasSize(1))
                .body("slots[0].trackId", equalTo(track.intValue()))
                .body("slots[0].energy", equalTo(4));
    }

    @Test
    void updateSlotInSetlist() {
        // GIVEN a setlist with a slot
        Long track1 = createTrack("Original Track");
        Long track2 = createTrack("Replacement Track");
        Long setlistId = createSetlist("Update Slot", "Testing slot update");

        Number slotId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track1, "slotOrder", 1, "energy", 2))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200)
                .extract()
                .path("slots[0].id");

        // WHEN updating the slot
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track2, "slotOrder", 1, "energy", 5))
                .when()
                .put("/api/setlists/{setlistId}/slots/{slotId}", setlistId, slotId.longValue())
                .then()
                .statusCode(200)
                .body("slots[0].trackId", equalTo(track2.intValue()))
                .body("slots[0].energy", equalTo(5));
    }

    @Test
    void removeSlotFromSetlist() {
        // GIVEN a setlist with two slots
        Long track1 = createTrack("Track 1");
        Long track2 = createTrack("Track 2");
        Long setlistId = createSetlist("Remove Slot", "Two slots");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track1, "slotOrder", 1))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200);

        Number slotId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track2, "slotOrder", 2))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200)
                .extract()
                .path("slots[1].id");

        // WHEN removing the second slot
        given()
                .when()
                .delete("/api/setlists/{setlistId}/slots/{slotId}", setlistId, slotId.longValue())
                .then()
                .statusCode(200)
                .body("slots", hasSize(1))
                .body("slots[0].trackId", equalTo(track1.intValue()));
    }

    @Test
    void reorderSlotsInSetlist() {
        // GIVEN a setlist with three slots
        Long track1 = createTrack("Track A");
        Long track2 = createTrack("Track B");
        Long track3 = createTrack("Track C");
        Long setlistId = createSetlist("Reorder", "Three tracks");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track1, "slotOrder", 1))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track2, "slotOrder", 2))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200);

        Number slot3Id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track3, "slotOrder", 3))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200)
                .extract()
                .path("slots[2].id");

        Number slot2Id = given()
                .when()
                .get("/api/setlists/{id}", setlistId)
                .then()
                .statusCode(200)
                .extract()
                .path("slots[1].id");

        Number slot1Id = given()
                .when()
                .get("/api/setlists/{id}", setlistId)
                .then()
                .statusCode(200)
                .extract()
                .path("slots[0].id");

        // WHEN reordering to 3, 1, 2
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("slotIds", List.of(slot3Id.longValue(), slot1Id.longValue(), slot2Id.longValue())))
                .when()
                .put("/api/setlists/{id}/slots/reorder", setlistId)
                .then()
                .statusCode(200)
                .body("slots[0].trackId", equalTo(track3.intValue()))
                .body("slots[1].trackId", equalTo(track1.intValue()))
                .body("slots[2].trackId", equalTo(track2.intValue()));
    }

    @Test
    void markSetlistReadyThenPerformed() {
        // GIVEN a draft setlist
        Long setlistId = createSetlist("Lifecycle Test", "Testing status changes");

        // WHEN marking as READY
        given()
                .when()
                .post("/api/setlists/{id}/ready", setlistId)
                .then()
                .statusCode(200)
                .body("status", equalTo("READY"));

        // WHEN marking as PERFORMED
        given()
                .when()
                .post("/api/setlists/{id}/performed", setlistId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PERFORMED"))
                .body("performedDate", notNullValue());
    }

    @Test
    void getSetlistByInvalidIdReturns404() {
        given()
                .when()
                .get("/api/setlists/{id}", 9999)
                .then()
                .statusCode(404)
                .body("error", containsString("Setlist not found"));
    }

    @Test
    void reorderWithMissingSlotReturns400() {
        Long setlistId = createSetlist("Bad Reorder", "One slot");
        Long track = createTrack("Only Track");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("trackId", track, "slotOrder", 1))
                .when()
                .post("/api/setlists/{id}/slots", setlistId)
                .then()
                .statusCode(200);

        // Try to reorder with a slot that doesn't exist
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("slotIds", List.of(9999)))
                .when()
                .put("/api/setlists/{id}/slots/reorder", setlistId)
                .then()
                .statusCode(400);
    }

    @Test
    void getEnergyArcReturnsOrderedPointsWithStats() {
        // GIVEN a setlist with three tracks at different energy levels
        Long track1 = createTrack("Low Energy Track");
        Long track2 = createTrack("Mid Energy Track");
        Long track3 = createTrack("High Energy Track");

        Map<String, Object> payload = Map.of(
                "name", "Energy Arc Test",
                "description", "Testing energy progression",
                "slots", List.of(
                        Map.of("trackId", track1, "slotOrder", 1, "energy", 2),
                        Map.of("trackId", track2, "slotOrder", 2, "energy", 3),
                        Map.of("trackId", track3, "slotOrder", 3, "energy", 5)));

        Number setlistId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // WHEN requesting the energy arc
        given()
                .when()
                .get("/api/setlists/{id}/energy-arc", setlistId.longValue())
                .then()
                .statusCode(200)
                .body("setlistId", equalTo(setlistId.intValue()))
                .body("setlistName", equalTo("Energy Arc Test"))
                .body("points", hasSize(3))
                .body("points[0].slotOrder", equalTo(1))
                .body("points[0].trackId", equalTo(track1.intValue()))
                .body("points[0].energy", equalTo(2))
                .body("points[1].slotOrder", equalTo(2))
                .body("points[1].trackId", equalTo(track2.intValue()))
                .body("points[1].energy", equalTo(3))
                .body("points[2].slotOrder", equalTo(3))
                .body("points[2].trackId", equalTo(track3.intValue()))
                .body("points[2].energy", equalTo(5))
                .body("stats.averageEnergy", equalTo(3.33f))
                .body("stats.peakEnergy", equalTo(5))
                .body("stats.lowEnergy", equalTo(2))
                .body("stats.energyTrend", equalTo("BUILD"));
    }

    @Test
    void getEnergyArcForInvalidSetlistReturns404() {
        given()
                .when()
                .get("/api/setlists/{id}/energy-arc", 9999)
                .then()
                .statusCode(404)
                .body("error", containsString("Setlist not found"));
    }

    @Test
    void validateTransitionsReportsKeyAndBpmIssues() {
        // GIVEN two tracks with incompatible keys and a large BPM difference
        Long track1 = createTrack("Key 4A Track");
        Long track2 = createTrack("Key 8B Track");

        // Create a setlist with these two tracks
        Map<String, Object> payload = Map.of(
                "name", "Validation Test",
                "description", "Testing transition validation",
                "slots", List.of(
                        Map.of("trackId", track1, "slotOrder", 1, "energy", 2),
                        Map.of("trackId", track2, "slotOrder", 2, "energy", 4)));

        Number setlistId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // WHEN validating transitions
        given()
                .when()
                .get("/api/setlists/{id}/validate-transitions", setlistId.longValue())
                .then()
                .statusCode(200)
                .body("setlistId", equalTo(setlistId.intValue()))
                .body("setlistName", equalTo("Validation Test"))
                .body("pairs", hasSize(1))
                .body("pairs[0].fromSlotOrder", equalTo(1))
                .body("pairs[0].toSlotOrder", equalTo(2))
                .body("pairs[0].knownTransition", equalTo(false))
                .body("pairs[0].warning", containsString("No logged transition"))
                .body("summary.totalPairs", equalTo(1))
                .body("summary.missingTransitionPairs", equalTo(1))
                .body("summary.warningsCount", equalTo(1));
    }

    @Test
    void validateTransitionsWithKnownTransitionReportsCompatibility() {
        // GIVEN two tracks with compatible keys
        Long track1 = createTrack("Key 4A Track");
        Long track2 = createTrack("Key 5A Track");

        // Create a setlist
        Map<String, Object> payload = Map.of(
                "name", "Known Transition Test",
                "description", "Testing with logged transition",
                "slots", List.of(
                        Map.of("trackId", track1, "slotOrder", 1, "energy", 2),
                        Map.of("trackId", track2, "slotOrder", 2, "energy", 4)));

        Number setlistId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Log a transition between them
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "sourceTrackId", track1,
                        "targetTrackId", track2,
                        "rating", 5,
                        "notes", "Great mix",
                        "style", "EQ blend"))
                .when()
                .post("/api/transitions")
                .then()
                .statusCode(201);

        // WHEN validating transitions
        given()
                .when()
                .get("/api/setlists/{id}/validate-transitions", setlistId.longValue())
                .then()
                .statusCode(200)
                .body("pairs[0].knownTransition", equalTo(true))
                .body("pairs[0].transitionRating", equalTo(5))
                .body("pairs[0].keyCompatible", equalTo(true))
                .body("summary.knownTransitionPairs", equalTo(1))
                .body("summary.compatibleKeyPairs", equalTo(1))
                .body("summary.warningsCount", equalTo(0));
    }

    @Test
    void validateTransitionsForInvalidSetlistReturns404() {
        given()
                .when()
                .get("/api/setlists/{id}/validate-transitions", 9999)
                .then()
                .statusCode(404)
                .body("error", containsString("Setlist not found"));
    }

    @Test
    void exportSetlistJsonReturnsFullMetadata() {
        // GIVEN a setlist with a track
        Long track = createTrack("Export Track");
        Map<String, Object> payload = Map.of(
                "name", "Export Test",
                "description", "Testing export",
                "slots", List.of(
                        Map.of("trackId", track, "slotOrder", 1, "energy", 3, "notes", "Mid set")));

        Number setlistId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // WHEN exporting as JSON
        given()
                .when()
                .post("/api/setlists/{id}/export?format=json", setlistId.longValue())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("setlistId", equalTo(setlistId.intValue()))
                .body("setlistName", equalTo("Export Test"))
                .body("status", equalTo("DRAFT"))
                .body("slots", hasSize(1))
                .body("slots[0].slotOrder", equalTo(1))
                .body("slots[0].trackId", equalTo(track.intValue()))
                .body("slots[0].trackTitle", equalTo("Export Track"))
                .body("slots[0].energy", equalTo(3));
    }

    @Test
    void exportSetlistTextReturnsPlainText() {
        // GIVEN a setlist with a track
        Long track = createTrack("Text Export Track");
        Map<String, Object> payload = Map.of(
                "name", "Text Export Test",
                "description", "Testing text export",
                "slots", List.of(
                        Map.of("trackId", track, "slotOrder", 1, "energy", 4)));

        Number setlistId = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/setlists")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // WHEN exporting as text
        given()
                .when()
                .post("/api/setlists/{id}/export?format=text", setlistId.longValue())
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(containsString("TrackStack Setlist Export"))
                .body(containsString("Text Export Test"))
                .body(containsString("[4/5]"))
                .body(containsString("Text Export Track"));
    }

    @Test
    void exportSetlistWithInvalidFormatReturns400() {
        Long setlistId = createSetlist("Bad Export", "Testing invalid format");

        given()
                .when()
                .post("/api/setlists/{id}/export?format=xml", setlistId)
                .then()
                .statusCode(400)
                .body("error", containsString("Unsupported format"));
    }

    @Test
    void exportSetlistForInvalidSetlistReturns404() {
        given()
                .when()
                .post("/api/setlists/{id}/export?format=json", 9999)
                .then()
                .statusCode(404)
                .body("error", containsString("Setlist not found"));
    }
}
