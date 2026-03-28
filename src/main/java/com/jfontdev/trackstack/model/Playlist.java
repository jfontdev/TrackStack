package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a playlist of tracks.
 * <p>
 * Playlists are named collections of {@link Track}s with an optional description.
 * They participate in a many-to-many relationship with Track, where Playlist
 * is the owning side (manages the {@code playlist_tracks} join table).
 * <p>
 * This entity follows the static factory method pattern for creation
 * and provides an {@code update} method for encapsulated mutation.
 */
@Entity
@Table(name = "playlists")
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToMany
    @JoinTable(
            name = "playlist_tracks",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "track_id")
    )
    private Set<Track> tracks = new HashSet<>();

    /**
     * Parameterized constructor used by the static factory method.
     *
     * @param name        the playlist name
     * @param description an optional description of the playlist
     */
    public Playlist(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Default no-args constructor required by JPA.
     */
    public Playlist() {

    }

    /**
     * Static factory method for creating a new Playlist instance.
     *
     * @param name        the playlist name
     * @param description an optional description
     * @return a new Playlist instance with the provided field values
     */
    public static Playlist create(String name, String description) {
        return new Playlist(name, description);
    }

    /**
     * Updates all mutable fields of this playlist.
     * <p>
     * Encapsulates mutation in a single operation. The service layer calls this
     * for both full (PUT) and partial (PATCH) updates -- for PATCH, the service
     * merges non-null fields before calling this method.
     *
     * @param name        the new playlist name
     * @param description the new playlist description
     */
    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Adds a track to this playlist.
     * <p>
     * This manages the owning side of the Playlist-Track many-to-many relationship.
     * The join table {@code playlist_tracks} is updated when this playlist is persisted.
     *
     * @param track the track to add to this playlist
     */
    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    /**
     * Removes a track from this playlist.
     * <p>
     * This manages the owning side of the Playlist-Track many-to-many relationship.
     * The corresponding join table row is removed when this playlist is persisted.
     *
     * @param track the track to remove from this playlist
     */
    public void removeTrack(Track track) {
        this.tracks.remove(track);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns an unmodifiable view of the tracks in this playlist.
     * <p>
     * To modify the tracks, use {@link #addTrack(Track)} or {@link #removeTrack(Track)}
     * to maintain domain encapsulation.
     *
     * @return an unmodifiable set of tracks belonging to this playlist
     */
    public Set<Track> getTracks() {
        return Collections.unmodifiableSet(tracks);
    }
}
