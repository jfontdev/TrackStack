package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a tag that can be applied to tracks.
 * <p>
 * Tags provide a flexible categorization mechanism for tracks (e.g., "chill",
 * "upbeat", "workout"). Each tag has a unique name enforced at the database level.
 * Tags participate in a many-to-many relationship with {@link Track}, where
 * Track is the owning side.
 * <p>
 * This entity follows the static factory method pattern for creation
 * and provides an {@code update} method for encapsulated mutation.
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Track> tracks = new HashSet<>();

    /**
     * Parameterized constructor used by the static factory method.
     *
     * @param name the tag name
     */
    public Tag(String name) {
        this.name = name;
    }

    /**
     * Default no-args constructor required by JPA.
     */
    public Tag() {

    }

    /**
     * Static factory method for creating a new Tag instance.
     *
     * @param name the tag name
     * @return a new Tag instance with the provided name
     */
    public static Tag create(String name) {
        return new Tag(name);
    }

    /**
     * Updates the mutable fields of this tag.
     * <p>
     * Encapsulates mutation in a single operation. The service layer calls this
     * for both full (PUT) and partial (PATCH) updates.
     *
     * @param name the new tag name
     */
    public void update(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
