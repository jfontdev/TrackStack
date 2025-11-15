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
            name="track_tags",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;

    @ManyToMany(mappedBy = "tracks")
    private Set<Playlist> playlists;
}
