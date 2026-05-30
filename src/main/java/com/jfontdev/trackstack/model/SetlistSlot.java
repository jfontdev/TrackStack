package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

/**
 * JPA entity representing a single slot within a {@link Setlist}.
 * <p>
 * Each slot holds one track at a specific position in the set sequence,
 * along with an energy level that contributes to the set's overall
 * energy arc (e.g., opening, build, peak, cooldown).
 * <p>
 * Slots are ordered by {@code slotOrder} within their parent setlist.
 * Reordering a setlist means updating the {@code slotOrder} of its slots.
 * <p>
 * This entity follows the static factory method pattern for creation
 * and provides {@code update} and {@code setSlotOrder} methods for
 * encapsulated mutation.
 *
 * @see Setlist
 * @see Track
 */
@Entity
@Table(name = "setlist_slots")
public class SetlistSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setlist_id", nullable = false)
    private Long setlistId;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(name = "slot_order", nullable = false)
    private Integer slotOrder;

    /**
     * Energy level for this slot (1-5), contributing to the set's energy arc.
     * 1 = low/ambient, 5 = peak intensity.
     */
    private Integer energy;

    @Column(length = 2000)
    private String notes;

    /**
     * Protected no-args constructor required by JPA.
     */
    protected SetlistSlot() {
    }

    /**
     * Parameterized constructor used by the static factory method.
     *
     * @param setlistId the parent setlist ID
     * @param trackId   the track placed in this slot
     * @param slotOrder the position of this slot in the setlist sequence
     * @param energy    optional energy level (1-5)
     * @param notes     optional free-text notes
     */
    private SetlistSlot(Long setlistId, Long trackId, Integer slotOrder,
                        Integer energy, String notes) {
        this.setlistId = setlistId;
        this.trackId = trackId;
        this.slotOrder = slotOrder;
        this.energy = energy;
        this.notes = notes;
    }

    /**
     * Static factory method for creating a new SetlistSlot.
     *
     * @param setlistId the parent setlist ID
     * @param trackId   the track placed in this slot
     * @param slotOrder the position of this slot in the setlist sequence
     * @param energy    optional energy level (1-5)
     * @param notes     optional free-text notes
     * @return a new SetlistSlot instance
     */
    public static SetlistSlot create(Long setlistId, Long trackId, Integer slotOrder,
                                     Integer energy, String notes) {
        return new SetlistSlot(setlistId, trackId, slotOrder, energy, notes);
    }

    /**
     * Updates the mutable fields of this slot.
     *
     * @param trackId the new track for this slot
     * @param energy  the new energy level
     * @param notes   the new notes
     */
    public void update(Long trackId, Integer energy, String notes) {
        this.trackId = trackId;
        this.energy = energy;
        this.notes = notes;
    }

    /**
     * Updates the order of this slot within the setlist.
     * <p>
     * Called by the service layer during reorder operations.
     *
     * @param slotOrder the new position in the sequence
     */
    public void setSlotOrder(Integer slotOrder) {
        this.slotOrder = slotOrder;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Long getSetlistId() {
        return setlistId;
    }

    public Long getTrackId() {
        return trackId;
    }

    public Integer getSlotOrder() {
        return slotOrder;
    }

    public Integer getEnergy() {
        return energy;
    }

    public String getNotes() {
        return notes;
    }
}
