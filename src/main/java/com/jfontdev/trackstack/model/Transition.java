package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a directed transition between two tracks.
 * <p>
 * Transitions are the core of the DJ workflow in TrackStack. They capture
 * the real-world knowledge of how well two tracks work when mixed together
 * <p>
 * Each transition is <b>directed</b>: Track A → Track B is a separate entry
 * from Track B → Track A, because the mix can feel very different in each
 * direction. This aligns with how DJs actually think about transitions.
 * <p>
 * <b>Auto-calculated fields:</b>
 * <ul>
 * <li>{@code compatibleKeys} — true if the two tracks' musical keys are
 * harmonically compatible (same key or adjacent on the Camelot wheel).</li>
 * <li>{@code bpmDifference} — absolute difference in BPM between source
 * and target tracks.</li>
 * </ul>
 * <p>
 * <b>Mutable fields:</b>
 * <ul>
 * <li>{@code rating} — user's subjective quality score (1-5 stars).</li>
 * <li>{@code notes} — free-text observations (e.g., "perfect for peak
 * time").</li>
 * <li>{@code style} — transition technique used (e.g., "blend", "cut", "echo
 * out").</li>
 * <li>{@code timesPlayed} — how many times this transition was performed
 * live.</li>
 * <li>{@code lastPlayedDate} — most recent performance of this transition.</li>
 * </ul>
 * <p>
 * This entity follows the static factory method pattern for creation and
 * provides an {@code update} method for encapsulated mutation.
 *
 * @see Track
 */
@Entity
@Table(name = "transitions")
public class Transition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_track_id", nullable = false)
    private Long sourceTrackId;

    @Column(name = "target_track_id", nullable = false)
    private Long targetTrackId;

    /**
     * User's subjective rating of how well this transition works (1-5).
     * 1 = poor, 5 = excellent.
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * Free-text notes about this transition.
     * Examples: "perfect for peak time", "key clash but works",
     * "use echo out at 32 bars".
     */
    @Column(length = 2000)
    private String notes;

    /**
     * The mixing technique used for this transition.
     * Common values: "blend", "cut", "echo out", "filter sweep", "scratch".
     */
    @Column(length = 50)
    private String style;

    /**
     * Auto-calculated field: true if the two tracks have harmonically
     * compatible keys according to the Camelot wheel.
     * <p>
     * Same key = compatible.
     * Adjacent numbers (e.g., 4A ↔ 5A, 4A ↔ 3A) = compatible.
     * Same letter, number ±7 (e.g., 4A ↔ 11A) = compatible (relative minor/major).
     */
    @Column(name = "compatible_keys")
    private Boolean compatibleKeys;

    /**
     * Auto-calculated field: absolute BPM difference between source and target.
     * Useful for identifying drastic tempo shifts.
     */
    @Column(name = "bpm_difference")
    private Double bpmDifference;

    /**
     * How many times this transition has been performed in a set.
     * Incremented via {@link #recordPlay()}.
     */
    @Column(name = "times_played")
    private Integer timesPlayed;

    /**
     * Most recent date this transition was performed.
     * Updated via {@link #recordPlay()}.
     */
    @Column(name = "last_played_date")
    private LocalDateTime lastPlayedDate;

    /**
     * When this transition was first logged in the system.
     * Immutable after creation.
     */
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    /**
     * Protected constructor for JPA.
     * External code should use
     * {@link #create(Long, Long, Integer, String, String)}.
     */
    protected Transition() {
    }

    /**
     * Parameterized constructor used by the static factory method.
     *
     * @param sourceTrackId the track being played first
     * @param targetTrackId the track being transitioned into
     * @param rating        user's quality rating (1-5)
     * @param notes         free-text observations (nullable)
     * @param style         transition technique (nullable)
     */
    private Transition(Long sourceTrackId, Long targetTrackId, Integer rating,
            String notes, String style) {
        this.sourceTrackId = sourceTrackId;
        this.targetTrackId = targetTrackId;
        this.rating = rating;
        this.notes = notes;
        this.style = style;
        this.timesPlayed = 0;
        this.createdDate = LocalDateTime.now();
    }

    /**
     * Static factory method for creating a new Transition.
     * <p>
     * This is the only way external code should construct transitions,
     * ensuring that {@code timesPlayed} starts at 0 and {@code createdDate}
     * is set automatically.
     *
     * @param sourceTrackId the track being played first
     * @param targetTrackId the track being transitioned into
     * @param rating        user's quality rating (1-5)
     * @param notes         free-text observations (nullable)
     * @param style         transition technique (nullable)
     * @return a new Transition instance
     */
    public static Transition create(Long sourceTrackId, Long targetTrackId,
            Integer rating, String notes, String style) {
        return new Transition(sourceTrackId, targetTrackId, rating, notes, style);
    }

    /**
     * Updates the mutable fields of this transition.
     * <p>
     * Does not modify auto-calculated fields ({@code compatibleKeys},
     * {@code bpmDifference}) — those are managed by the service layer.
     *
     * @param rating the new rating (1-5)
     * @param notes  the new notes
     * @param style  the new style
     */
    public void update(Integer rating, String notes, String style) {
        this.rating = rating;
        this.notes = notes;
        this.style = style;
    }

    /**
     * Records that this transition was performed, incrementing play count
     * and updating the last played date.
     * <p>
     * Called by the service layer when a setlist is marked as performed
     * and this transition was part of it.
     */
    public void recordPlay() {
        this.timesPlayed++;
        this.lastPlayedDate = LocalDateTime.now();
    }

    // --- Auto-calculation setters (called by service layer) ---

    /**
     * Sets whether the source and target tracks have compatible keys.
     * <p>
     * This is calculated by the service layer after looking up the
     * associated Track entities, not by the entity itself.
     *
     * @param compatibleKeys true if keys are harmonically compatible
     */
    public void setCompatibleKeys(Boolean compatibleKeys) {
        this.compatibleKeys = compatibleKeys;
    }

    /**
     * Sets the BPM difference between source and target tracks.
     * <p>
     * This is calculated by the service layer after looking up the
     * associated Track entities.
     *
     * @param bpmDifference absolute BPM difference
     */
    public void setBpmDifference(Double bpmDifference) {
        this.bpmDifference = bpmDifference;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Long getSourceTrackId() {
        return sourceTrackId;
    }

    public Long getTargetTrackId() {
        return targetTrackId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getNotes() {
        return notes;
    }

    public String getStyle() {
        return style;
    }

    public Boolean getCompatibleKeys() {
        return compatibleKeys;
    }

    public Double getBpmDifference() {
        return bpmDifference;
    }

    public Integer getTimesPlayed() {
        return timesPlayed;
    }

    public LocalDateTime getLastPlayedDate() {
        return lastPlayedDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
