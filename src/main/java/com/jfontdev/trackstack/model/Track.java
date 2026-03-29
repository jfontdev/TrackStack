package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a music track.
 * <p>
 * Tracks are the core domain object in TrackStack. Each track has metadata
 * such as title, artist, BPM, musical key, and duration. Tracks can be
 * associated with multiple {@link Tag}s and belong to multiple
 * {@link Playlist}s
 * through many-to-many relationships.
 * <p>
 * This entity follows the static factory method pattern for creation
 * and provides an {@code update} method for encapsulated mutation,
 * keeping the entity in control of its own state transitions.
 */
@Entity
@Table(name = "tracks")
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String artist;
    private String album;
    private Double bpm;
    private String key; // musical key
    private String duration;
    private String genre;

    @ManyToMany
    @JoinTable(name = "track_tags", joinColumns = @JoinColumn(name = "track_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany(mappedBy = "tracks")
    private Set<Playlist> playlists = new HashSet<>();

    /**
     * Parameterized constructor used by the static factory method.
     *
     * @param title    the track title
     * @param artist   the track artist
     * @param bpm      the beats per minute
     * @param key      the musical key (e.g., "A minor")
     * @param duration the track duration (e.g., "3:45")
     * @param genre    the track genre (e.g., "House")
     */
    public Track(String title, String artist, Double bpm, String key, String duration, String genre) {
        this.title = title;
        this.artist = artist;
        this.bpm = bpm;
        this.key = key;
        this.duration = duration;
        this.genre = genre;
    }

    /**
     * Default no-args constructor required by JPA.
     */
    public Track() {

    }

    /**
     * Static factory method for creating a new Track instance.
     * <p>
     * Controllers and services should use this method instead of calling
     * the constructor directly, keeping entity creation centralized.
     *
     * @param title    the track title
     * @param artist   the track artist
     * @param bpm      the beats per minute
     * @param key      the musical key
     * @param duration the track duration
     * @param genre    the track genre
     * @return a new Track instance with the provided field values
     */
    public static Track create(String title, String artist, Double bpm, String key, String duration, String genre) {
        return new Track(title, artist, bpm, key, duration, genre);
    }

    /**
     * Updates all mutable fields of this track.
     * <p>
     * This method encapsulates mutation in a single operation, mirroring
     * the factory method pattern used for creation. The service layer calls
     * this method for both full (PUT) and partial (PATCH) updates -- for PATCH,
     * the service merges non-null fields from the request with existing values
     * before calling this method.
     *
     * @param title    the new track title
     * @param artist   the new track artist
     * @param bpm      the new beats per minute
     * @param key      the new musical key
     * @param duration the new track duration
     * @param genre    the new track genre
     */
    public void update(String title, String artist, Double bpm, String key, String duration, String genre) {
        this.title = title;
        this.artist = artist;
        this.bpm = bpm;
        this.key = key;
        this.duration = duration;
        this.genre = genre;
    }

    /**
     * Adds a tag to this track's tag set.
     * <p>
     * This manages the owning side of the Track-Tag many-to-many relationship.
     * The join table {@code track_tags} is updated when this track is persisted.
     *
     * @param tag the tag to associate with this track
     */
    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    /**
     * Removes a tag from this track's tag set.
     * <p>
     * This manages the owning side of the Track-Tag many-to-many relationship.
     * The corresponding join table row is removed when this track is persisted.
     *
     * @param tag the tag to disassociate from this track
     */
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Double getBpm() {
        return bpm;
    }

    public String getKey() {
        return key;
    }

    public String getDuration() {
        return duration;
    }

    public String getGenre() {
        return genre;
    }

    /**
     * Returns an unmodifiable view of the tags associated with this track.
     * <p>
     * To modify the tags, use {@link #addTag(Tag)} or {@link #removeTag(Tag)}
     * to maintain domain encapsulation.
     *
     * @return an unmodifiable set of tags associated with this track
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
