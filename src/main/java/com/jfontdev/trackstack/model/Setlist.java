package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a DJ setlist — an ordered sequence of tracks
 * planned for a performance or practice session.
 * <p>
 * A setlist progresses through a lifecycle:
 * <ul>
 *   <li>{@code DRAFT} — actively being built and edited</li>
 *   <li>{@code READY} — finalized, reviewed, and ready to perform</li>
 *   <li>{@code PERFORMED} — already played in a session</li>
 * </ul>
 * <p>
 * {@code totalDurationSeconds} is auto-calculated by the service layer
 * from the durations of the tracks in its slots.
 * {@code preparationTimeMinutes} tracks how long was spent building
 * the setlist, logged by the service layer.
 * <p>
 * This entity follows the static factory method pattern for creation
 * and provides {@code update}, {@code markReady}, and {@code markPerformed}
 * methods for encapsulated mutation.
 *
 * @see SetlistSlot
 * @see Track
 */
@Entity
@Table(name = "setlists")
public class Setlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "performed_date")
    private LocalDateTime performedDate;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    /**
     * Protected no-args constructor required by JPA.
     */
    protected Setlist() {
    }

    /**
     * Parameterized constructor used by the static factory method.
     *
     * @param name        the display name of the setlist
     * @param description optional free-text description
     */
    private Setlist(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = "DRAFT";
        this.createdDate = LocalDateTime.now();
        this.totalDurationSeconds = 0;
        this.preparationTimeMinutes = 0;
    }

    /**
     * Static factory method for creating a new Setlist in DRAFT status.
     *
     * @param name        the display name of the setlist
     * @param description optional free-text description
     * @return a new Setlist instance
     */
    public static Setlist create(String name, String description) {
        return new Setlist(name, description);
    }

    /**
     * Updates the mutable descriptive fields of this setlist.
     *
     * @param name        the new display name
     * @param description the new description
     */
    public void update(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedDate = LocalDateTime.now();
    }

    /**
     * Marks this setlist as ready for performance.
     * <p>
     * Called when the DJ has finalized the track order and energy arc.
     */
    public void markReady() {
        this.status = "READY";
        this.updatedDate = LocalDateTime.now();
    }

    /**
     * Marks this setlist as performed and records the performance date.
     * <p>
     * Called when the setlist has been played in a live or practice session.
     */
    public void markPerformed() {
        this.status = "PERFORMED";
        this.performedDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // --- Auto-calculation setters (called by service layer) ---

    /**
     * Sets the total duration in seconds, calculated from slot tracks.
     *
     * @param totalDurationSeconds summed duration of all tracks
     */
    public void setTotalDurationSeconds(Integer totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    /**
     * Sets the preparation time in minutes.
     *
     * @param preparationTimeMinutes time spent building the setlist
     */
    public void setPreparationTimeMinutes(Integer preparationTimeMinutes) {
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public LocalDateTime getPerformedDate() {
        return performedDate;
    }

    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public Integer getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }
}
