package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.util.Set;

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

    @ManyToMany
    @JoinTable(
            name = "track_tags",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;

    @ManyToMany(mappedBy = "tracks")
    private Set<Playlist> playlists;

    public Track(String title, String artist, Double bpm, String key, String duration) {
        this.title = title;
        this.artist = artist;
        this.bpm = bpm;
        this.key = key;
        this.duration = duration;
    }

    public Track() {

    }

    public static Track create(String title, String artist, Double bpm, String key, String duration) {
        return new Track(title, artist, bpm, key, duration);
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
}
